import { useMemo, useRef, useState } from "react";
import { ArrowUpRight, FileText, Navigation, PawPrint, Search, Waves } from "lucide-react";
import { MapView } from "../components/map/MapView";
import { useMapLabels } from "../components/map/useMapLabels";
import { PageTitle } from "../components/PageTitle";
import { SegmentedControl } from "../components/SegmentedControl";
import {
  ExternalLink,
  NearestFirst,
  NearMeButton,
  ResultRow,
  ResultsMessage,
  ResultsPanel,
  SelectedResult,
  ShowMore,
  SourceAttribution,
  ToolFeedback,
  ToolFilters,
  ToolLayout,
  ToolWorkspace,
} from "../components/tool/Tool";
import { useDistanceLabel, useSourceDates } from "../format";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { googleDirections } from "../geo/directions";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { bathingWaterUrl, dogBeachesUrl, eeaLicenseUrl, osmCopyrightUrl } from "../sources";
import { type Beach, useBeaches } from "./api";
import { type DogBeach, dogBeaches, dogBeachSeason } from "./dogBeaches";
import "./beaches.css";

const PAGE = 30;

const qualityLabels: Record<string, MessageKey> = {
  excellent: "waterExcellent",
  good: "waterGood",
  sufficient: "waterSufficient",
  "good-or-sufficient": "waterGoodOrSufficient",
  poor: "waterPoor",
  "not-classified": "waterNotClassified",
  "missing-data": "waterMissingData",
};

/** Best class first; beaches without a class last. */
const qualityOrder = ["excellent", "good", "good-or-sufficient", "sufficient", "poor", "not-classified", "missing-data"];
const qualityRank = (quality: string | null) => {
  const index = quality ? qualityOrder.indexOf(quality) : -1;
  return index === -1 ? qualityOrder.length : index;
};

type Kind = "all" | "dogs";
type Sort = "quality" | "name" | "distance";
const DOG = "🐾";

/** Upper limits for "excellent" coastal water, 95th percentile in cfu/100 ml (Directive 2006/7/EC, Annex I). */
const EXCELLENT_E_COLI = 250;
const EXCELLENT_ENTEROCOCCI = 100;

/** Source names are upper case ("KANALI POMOU"); shown as "Kanali Pomou". */
const display = (name: string) =>
  name.toLowerCase().replace(/(^|[\s(/-])(\p{L})/gu, (_, before: string, letter: string) => before + letter.toUpperCase());

function QualityChip({ quality }: { quality: string | null }) {
  const { t } = useI18n();
  if (!quality) return null;
  return (
    <span className={"beach-quality quality-" + quality}>
      {qualityLabels[quality] ? t(qualityLabels[quality]) : quality}
    </span>
  );
}

export default function Beaches() {
  const { t, language } = useI18n();
  const results = useRef<HTMLDivElement>(null);
  const [search, setSearch] = useState("");
  const [shown, setShown] = useState(PAGE);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [kind, setKind] = useState<Kind>("all");
  const [sort, setSort] = useState<Sort>("quality");
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useBeaches();
  const beaches = useMemo(() => query.data?.beaches ?? [], [query.data]);
  const dogs = kind === "dogs";

  const needle = search.trim().toLowerCase();
  // One filter for both the list and the map.
  const matching = useMemo(
    () => (needle ? beaches.filter((beach) => beach.name.toLowerCase().includes(needle)) : beaches),
    [beaches, needle],
  );
  const placeOf = (beach: DogBeach) => beach.place[language];
  const matchingDogs = useMemo(
    () =>
      needle
        ? dogBeaches.filter((beach) => (beach.name + " " + beach.place[language]).toLowerCase().includes(needle))
        : dogBeaches,
    [needle, language],
  );
  // "Nearest first" needs a location; until there is one the list stays by water quality.
  const order: Sort = sort === "distance" && !position ? "quality" : sort;
  const distanceTo = (beach: { latitude: number | null; longitude: number | null }) =>
    position && beach.latitude !== null && beach.longitude !== null
      ? distance(position, { latitude: beach.latitude, longitude: beach.longitude })
      : null;
  const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name);
  const byDistance = (a: Beach | DogBeach, b: Beach | DogBeach) =>
    (distanceTo(a) ?? Infinity) - (distanceTo(b) ?? Infinity);
  const sorted = [...matching].sort(
    order === "distance"
      ? byDistance
      : order === "name"
        ? byName
        : (a, b) => qualityRank(a.quality) - qualityRank(b.quality) || byName(a, b),
  );
  const sortedDogs = [...matchingDogs].sort(position ? byDistance : byName);
  const selected = dogs ? undefined : beaches.find((beach) => beach.id === selectedId);
  const selectedDog = dogs ? dogBeaches.find((beach) => beach.id === selectedId) : undefined;

  const points = useMemo(
    () =>
      dogs
        ? matchingDogs.flatMap((beach) =>
            beach.latitude === null || beach.longitude === null
              ? []
              : [{ id: beach.id, label: beach.name, latitude: beach.latitude, longitude: beach.longitude, tone: "dog" }],
          )
        : matching.map((beach) => ({
            id: beach.id,
            label: display(beach.name),
            latitude: beach.latitude,
            longitude: beach.longitude,
            tone: beach.quality ? "quality-" + beach.quality : undefined,
          })),
    [dogs, matching, matchingDogs],
  );
  const labels = useMapLabels("beaches", "beachCount");
  const distanceLabel = useDistanceLabel();
  const sourceDates = useSourceDates();
  // EEA publishes one bathing season a year, so the season is the date of the data.
  const [publishedFact, checkedFact] = sourceDates(null, query.data?.source.retrievedAt);
  const number = new Intl.NumberFormat(language, { maximumFractionDigits: 0 });

  function chooseKind(next: Kind) {
    setKind(next);
    setSelectedId(null);
    setShown(PAGE);
  }

  function chooseSort(next: Sort) {
    setSort(next);
    setShown(PAGE);
    if (next === "distance" && !position) locate(() => setSelectedId(null));
  }

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  const detail = (beach: Beach) => {
    const away = distanceTo(beach);
    return [
      <QualityChip quality={beach.quality} />,
      away !== null ? distanceLabel(away) : null,
    ];
  };

  return (
    <>
      <PageTitle title={t("beaches")} path="/beaches" />
      <ToolWorkspace label={t("beaches")}>
        <ToolFilters>
          <label className="tool-search">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              placeholder={t("searchBeaches")}
              aria-label={t("searchBeaches")}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setShown(PAGE);
              }}
            />
          </label>
          {!dogs && (
            <select
              aria-label={t("beachSort")}
              value={order}
              onChange={(event) => chooseSort(event.target.value as Sort)}
            >
              <option value="quality">{t("sortQuality")}</option>
              <option value="name">{t("sortName")}</option>
              <option value="distance">{t("sortDistance")}</option>
            </select>
          )}
          <NearMeButton
            locating={locating}
            onClick={() =>
              locate(() => {
                setSelectedId(null);
                setSort("distance");
              })
            }
          />
          <SegmentedControl
            label={t("beachKind")}
            value={kind}
            options={[
              { value: "all", label: t("allBeaches") },
              { value: "dogs", label: DOG + " " + t("dogBeaches") },
            ]}
            onChange={chooseKind}
          />
        </ToolFilters>
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <MapView
              points={points}
              bounds={areas[0].bounds}
              selectedId={selectedId ?? undefined}
              position={position}
              onSelect={choose}
              markerSymbol={dogs ? DOG : "≈"}
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={
              dogs
                ? t("dogBeachCount", { count: matchingDogs.length })
                : query.isPending
                  ? t("loading")
                  : t("beachCount", { count: matching.length })
            }
            action={
              position &&
              (dogs || order === "distance") && (
                <NearestFirst
                  onClear={() => {
                    clearLocation();
                    setSort("quality");
                  }}
                />
              )
            }
            notes={[
              dogs
                ? t("dogBeachNote", { season: dogBeachSeason })
                : query.data?.season && t("beachSeason", { season: query.data.season }),
            ]}
          >
            {dogs && (
              <>
                {matchingDogs.length === 0 && (
                  <ResultsMessage kind="empty" onReset={() => setSearch("")}>
                    {t("dogBeachNoMatches")}
                  </ResultsMessage>
                )}
                {selectedDog && (
                  <SelectedResult label={t("selectedDogBeach")} title={selectedDog.name} detail={placeOf(selectedDog)}>
                    {(selectedDog.municipal || selectedDog.latitude === null) && (
                      <p className="beach-dog-note">
                        {selectedDog.municipal ? t("dogBeachMunicipal") : t("dogBeachNoPoint")}
                      </p>
                    )}
                    <a
                      href={googleDirections(
                        selectedDog.latitude === null
                          ? `${selectedDog.name} beach, ${selectedDog.place.en}, Cyprus`
                          : `${selectedDog.latitude},${selectedDog.longitude}`,
                      )}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Navigation size={15} aria-hidden="true" />
                      {t("directions")}
                      <ArrowUpRight size={14} aria-hidden="true" />
                    </a>
                  </SelectedResult>
                )}
                <ol className="result-list" aria-label={t("dogBeaches")}>
                  {sortedDogs.map((beach) => {
                    const away = distanceTo(beach);
                    return (
                      <ResultRow
                        key={beach.id}
                        icon={<PawPrint size={15} />}
                        tone="dog"
                        title={beach.name}
                        details={[placeOf(beach), away !== null ? distanceLabel(away) : null]}
                        selected={selectedDog?.id === beach.id}
                        onSelect={() => choose(beach.id)}
                      />
                    );
                  })}
                </ol>
              </>
            )}
            {!dogs && (
              <>
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("beachLoadError")}</ResultsMessage>
            )}
            {query.data && beaches.length === 0 && <ResultsMessage kind="pending">{t("beachUnavailable")}</ResultsMessage>}
            {query.data && beaches.length > 0 && matching.length === 0 && (
              <ResultsMessage kind="empty" onReset={() => setSearch("")}>
                {t("beachNoMatches")}
              </ResultsMessage>
            )}
            {selected && (
              <SelectedResult
                label={t("selectedBeach")}
                title={display(selected.name)}
                detail={selected.season ? t("beachSeason", { season: selected.season }) : undefined}
              >
                <div>
                  <QualityChip quality={selected.quality} />
                  {selected.history.length > 1 && (
                    <ul className="beach-history" aria-label={t("waterHistory")}>
                      {selected.history.map((item) => (
                        <li key={item.season} title={qualityLabels[item.quality] ? t(qualityLabels[item.quality]) : item.quality}>
                          <span className={"dot quality-" + item.quality} aria-hidden="true" />
                          {item.season}
                          <span className="visually-hidden">
                            {qualityLabels[item.quality] ? t(qualityLabels[item.quality]) : item.quality}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                  {selected.samples !== null && (
                    <>
                      <table className="beach-tests">
                        <thead>
                          <tr>
                            <th scope="col">{t("waterTest", { samples: selected.samples })}</th>
                            <td>{t("excellentLimit")}</td>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <th scope="row">E. coli</th>
                            <td>
                              {selected.escherichiaColi === null ? "–" : number.format(selected.escherichiaColi)} /{" "}
                              {EXCELLENT_E_COLI}
                            </td>
                          </tr>
                          <tr>
                            <th scope="row">{t("enterococci")}</th>
                            <td>
                              {selected.intestinalEnterococci === null ? "–" : number.format(selected.intestinalEnterococci)} /{" "}
                              {EXCELLENT_ENTEROCOCCI}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                      <p className="beach-note">{t("waterTestNote")}</p>
                    </>
                  )}
                </div>
                <a
                  href={googleDirections(`${selected.latitude},${selected.longitude}`)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Navigation size={15} aria-hidden="true" />
                  {t("directions")}
                  <ArrowUpRight size={14} aria-hidden="true" />
                </a>
                {selected.profileUrl && (
                  <a href={selected.profileUrl} target="_blank" rel="noopener noreferrer">
                    <FileText size={15} aria-hidden="true" />
                    {t("beachProfile")}
                    <ArrowUpRight size={14} aria-hidden="true" />
                  </a>
                )}
              </SelectedResult>
            )}
            <ol className="result-list" aria-label={t("beaches")}>
              {sorted.slice(0, shown).map((beach) => (
                <ResultRow
                  key={beach.id}
                  icon="≈"
                  tone={beach.quality ? "quality-" + beach.quality : undefined}
                  title={display(beach.name)}
                  details={detail(beach)}
                  selected={selected?.id === beach.id}
                  onSelect={() => choose(beach.id)}
                />
              ))}
            </ol>
            {sorted.length > shown && <ShowMore onClick={() => setShown((count) => count + PAGE)} />}
              </>
            )}
          </ResultsPanel>
        </ToolLayout>
        <SourceAttribution
          href={query.data?.source.url ?? bathingWaterUrl}
          badge={<Waves size={20} aria-hidden="true" />}
          name={t("bathingWaterSource")}
          facts={[
            query.data?.season ? t("dataSeason", { season: query.data.season }) : publishedFact,
            checkedFact,
            <ExternalLink href={eeaLicenseUrl}>CC BY 4.0</ExternalLink>,
            <ExternalLink href={dogBeachesUrl}>{t("dogBeachSource", { season: dogBeachSeason })}</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

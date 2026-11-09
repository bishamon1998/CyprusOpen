import { useMemo, useRef, useState } from "react";
import { ArrowUpRight, FileText, Navigation, Search, Waves } from "lucide-react";
import { MapView } from "../components/map/MapView";
import { useMapLabels } from "../components/map/useMapLabels";
import { PageTitle } from "../components/PageTitle";
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
import { bathingWaterUrl, eeaLicenseUrl, osmCopyrightUrl } from "../sources";
import { type Beach, useBeaches } from "./api";
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
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useBeaches();
  const beaches = useMemo(() => query.data?.beaches ?? [], [query.data]);

  const needle = search.trim().toLowerCase();
  // One filter for both the list and the map.
  const matching = useMemo(
    () => (needle ? beaches.filter((beach) => beach.name.toLowerCase().includes(needle)) : beaches),
    [beaches, needle],
  );
  const distanceTo = (beach: Beach) => (position ? distance(position, beach) : null);
  const sorted = position
    ? [...matching].sort((a, b) => (distanceTo(a) ?? Infinity) - (distanceTo(b) ?? Infinity))
    : matching;
  const selected = beaches.find((beach) => beach.id === selectedId);

  const points = useMemo(
    () =>
      matching.map((beach) => ({
        id: beach.id,
        label: display(beach.name),
        latitude: beach.latitude,
        longitude: beach.longitude,
        tone: beach.quality ? "quality-" + beach.quality : undefined,
      })),
    [matching],
  );
  const labels = useMapLabels("beaches", "beachCount");
  const distanceLabel = useDistanceLabel();
  const sourceDates = useSourceDates();
  // EEA publishes one bathing season a year, so the season is the date of the data.
  const [publishedFact, checkedFact] = sourceDates(null, query.data?.source.retrievedAt);
  const number = new Intl.NumberFormat(language, { maximumFractionDigits: 0 });

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
      <PageTitle title={t("beaches")} />
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
          <NearMeButton locating={locating} onClick={() => locate(() => setSelectedId(null))} />
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
              markerSymbol="≈"
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={query.isPending ? t("loading") : t("beachCount", { count: matching.length })}
            action={position && <NearestFirst onClear={clearLocation} />}
            notes={[query.data?.season && t("beachSeason", { season: query.data.season })]}
          >
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("beachLoadError")}</ResultsMessage>
            )}
            {query.data && beaches.length === 0 && <ResultsMessage>{t("beachUnavailable")}</ResultsMessage>}
            {query.data && beaches.length > 0 && matching.length === 0 && (
              <ResultsMessage>{t("beachNoMatches")}</ResultsMessage>
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
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

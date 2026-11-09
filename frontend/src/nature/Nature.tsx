import { useMemo, useRef, useState } from "react";
import { ArrowUpRight, Navigation, Search, Trees } from "lucide-react";
import { type MapLine, MapView } from "../components/map/MapView";
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
import { useDistanceLabel } from "../format";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { googleDirections } from "../geo/directions";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { ccByLicenseUrl, natureSourceUrl, osmCopyrightUrl } from "../sources";
import { type Kind, kinds, type NatureSite, useNature } from "./api";
import "./nature.css";

const PAGE = 30;

const kindLabels: Record<Kind, MessageKey> = {
  picnic: "picnicSite",
  camping: "campingSite",
  trail: "natureTrail",
};
const filterLabels: Record<Kind, MessageKey> = {
  picnic: "picnicSites",
  camping: "campingSites",
  trail: "natureTrails",
};
/** Marker and list symbols; the colour comes from the "tone-…" class. */
const symbols: Record<Kind, string> = { picnic: "♣", camping: "▲", trail: "↝" };

export default function Nature() {
  const { t, language } = useI18n();
  const results = useRef<HTMLDivElement>(null);
  const [search, setSearch] = useState("");
  const [kind, setKind] = useState<Kind | "all">("all");
  const [shown, setShown] = useState(PAGE);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useNature();
  const sites = useMemo(() => query.data?.sites ?? [], [query.data]);
  // Greek names where the source has them, otherwise the published Latin spelling.
  const name = (site: NatureSite) => (language === "el" && site.greekName) || site.name;

  const needle = search.trim().toLocaleLowerCase();
  // One filter for both the list and the map.
  const matching = useMemo(
    () =>
      sites.filter(
        (site) =>
          (kind === "all" || site.kind === kind) &&
          (!needle ||
            site.name.toLocaleLowerCase().includes(needle) ||
            Boolean(site.greekName?.toLocaleLowerCase().includes(needle))),
      ),
    [sites, kind, needle],
  );
  const sorted = position
    ? [...matching].sort((a, b) => distance(position, a) - distance(position, b))
    : matching;
  const selected = sites.find((site) => site.id === selectedId);

  const points = useMemo(
    () =>
      matching.map((site) => ({
        id: site.id,
        label: (language === "el" && site.greekName) || site.name,
        latitude: site.latitude,
        longitude: site.longitude,
        tone: site.kind,
        symbol: symbols[site.kind],
      })),
    [matching, language],
  );
  const lines = useMemo<MapLine[]>(
    () =>
      matching
        .filter((site) => site.route.length > 0)
        .map((site) => ({
          id: site.id,
          label: (language === "el" && site.greekName) || site.name,
          route: site.route,
        })),
    [matching, language],
  );
  const labels = useMapLabels("natureMap", "natureCount");
  const distanceLabel = useDistanceLabel();
  const number = new Intl.NumberFormat(language, { maximumFractionDigits: 1 });

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  const facts = (site: NatureSite) => [
    site.lengthKm !== null ? t("trailLength", { length: number.format(site.lengthKm) }) : null,
    site.capacity !== null ? t("campingCapacity", { count: site.capacity }) : null,
  ];
  const detail = (site: NatureSite) =>
    [t(kindLabels[site.kind]), ...facts(site), position ? distanceLabel(distance(position, site)) : null]
      .filter(Boolean)
      .join(" · ");

  return (
    <>
      <PageTitle title={t("nature")} />
      <ToolWorkspace label={t("nature")}>
        <ToolFilters>
          <label className="tool-search">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              placeholder={t("searchNature")}
              aria-label={t("searchNature")}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setShown(PAGE);
              }}
            />
          </label>
          <select
            aria-label={t("natureKind")}
            value={kind}
            onChange={(event) => {
              setKind(event.target.value as Kind | "all");
              setShown(PAGE);
              setSelectedId(null);
            }}
          >
            <option value="all">{t("allNature")}</option>
            {kinds.map((item) => (
              <option key={item} value={item}>
                {t(filterLabels[item])}
              </option>
            ))}
          </select>
          <NearMeButton locating={locating} onClick={() => locate(() => setSelectedId(null))} />
        </ToolFilters>
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <MapView
              points={points}
              lines={lines}
              bounds={areas[0].bounds}
              selectedId={selectedId ?? undefined}
              position={position}
              onSelect={choose}
              markerSymbol={symbols.picnic}
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={query.isPending ? t("loading") : t("natureCount", { count: matching.length })}
            action={position && <NearestFirst onClear={clearLocation} />}
            notes={[t("natureNote")]}
          >
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("natureLoadError")}</ResultsMessage>
            )}
            {query.data && sites.length === 0 && <ResultsMessage>{t("natureUnavailable")}</ResultsMessage>}
            {query.data && sites.length > 0 && matching.length === 0 && (
              <ResultsMessage>{t("natureNoMatches")}</ResultsMessage>
            )}
            {selected && (
              <SelectedResult
                label={t(kindLabels[selected.kind])}
                title={name(selected)}
                detail={facts(selected).filter(Boolean).join(" · ") || undefined}
              >
                <a
                  href={googleDirections(`${selected.latitude},${selected.longitude}`)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Navigation size={15} aria-hidden="true" />
                  {t(selected.kind === "trail" ? "trailDirections" : "directions")}
                  <ArrowUpRight size={14} aria-hidden="true" />
                </a>
              </SelectedResult>
            )}
            <ol className="result-list" aria-label={t("natureSites")}>
              {sorted.slice(0, shown).map((site) => (
                <ResultRow
                  key={site.id}
                  icon={symbols[site.kind]}
                  tone={site.kind}
                  title={name(site)}
                  details={[detail(site)]}
                  selected={selected?.id === site.id}
                  onSelect={() => choose(site.id)}
                />
              ))}
            </ol>
            {sorted.length > shown && <ShowMore onClick={() => setShown((count) => count + PAGE)} />}
          </ResultsPanel>
        </ToolLayout>
        <SourceAttribution
          href={query.data?.source.url ?? natureSourceUrl}
          badge={<Trees size={20} aria-hidden="true" />}
          name={t("forestsDepartment")}
          facts={[
            <ExternalLink href={ccByLicenseUrl}>CC BY 4.0</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

import { useCallback, useMemo, useRef, useState } from "react";
import { ArrowUpRight, Navigation, Recycle } from "lucide-react";
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
import { areas, townOf } from "../geo/cyprusAreas";
import { googleDirections } from "../geo/directions";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { ccBySaLicenseUrl, osmCopyrightUrl, recyclingSourceUrl } from "../sources";
import { type Kind, kinds, type RecyclingPoint, useRecycling } from "./api";
import "./recycling.css";

const PAGE = 30;

const kindLabels: Record<Kind, MessageKey> = { island: "recyclingIsland", glass: "glassBank" };
const filterLabels: Record<Kind, MessageKey> = { island: "recyclingIslands", glass: "glassBanks" };
const contents: Record<Kind, MessageKey> = { island: "islandContents", glass: "glassContents" };
/** Marker and list symbols; the colour comes from the "tone-…" class. */
const symbols: Record<Kind, string> = { island: "♻", glass: "●" };

export default function Recycling() {
  const { t } = useI18n();
  const results = useRef<HTMLDivElement>(null);
  const [areaId, setAreaId] = useState("cyprus");
  const [kind, setKind] = useState<Kind | "all">("all");
  const [shown, setShown] = useState(PAGE);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useRecycling();
  const all = useMemo(() => query.data?.points ?? [], [query.data]);
  const area = areas.find((item) => item.id === areaId)!;

  const matching = useMemo(() => {
    const [[south, west], [north, east]] = area.bounds;
    return all.filter(
      (point) =>
        (kind === "all" || point.kind === kind) &&
        (area.id === "cyprus" ||
          (point.latitude >= south && point.latitude <= north && point.longitude >= west && point.longitude <= east)),
    );
  }, [all, kind, area]);
  const sorted = position
    ? [...matching].sort((a, b) => distance(position, a) - distance(position, b))
    : matching;
  const selected = all.find((point) => point.id === selectedId);

  const points = useMemo(
    () =>
      matching.map((point) => ({
        id: point.id,
        label: point.description ?? t(kindLabels[point.kind]),
        latitude: point.latitude,
        longitude: point.longitude,
        tone: point.kind,
        symbol: symbols[point.kind],
      })),
    [matching, t],
  );
  const labels = useMapLabels("recyclingMap", "recyclingCount");
  const distanceLabel = useDistanceLabel();
  const sourceDates = useSourceDates();
  const town = useCallback(
    (point: RecyclingPoint) => {
      const { town, inside } = townOf(point);
      return inside ? t(town) : t("aroundArea", { town: t(town) });
    },
    [t],
  );

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  function reset() {
    setShown(PAGE);
    setSelectedId(null);
  }

  return (
    <>
      <PageTitle title={t("recycling")} path="/recycling" />
      <ToolWorkspace label={t("recycling")}>
        <ToolFilters>
          <select
            aria-label={t("area")}
            value={areaId}
            onChange={(event) => {
              setAreaId(event.target.value);
              reset();
            }}
          >
            {areas.map((item) => (
              <option key={item.id} value={item.id}>
                {t(item.name)}
              </option>
            ))}
          </select>
          <select
            aria-label={t("recyclingKind")}
            value={kind}
            onChange={(event) => {
              setKind(event.target.value as Kind | "all");
              reset();
            }}
          >
            <option value="all">{t("allRecycling")}</option>
            {kinds.map((item) => (
              <option key={item} value={item}>
                {t(filterLabels[item])}
              </option>
            ))}
          </select>
          <NearMeButton
            locating={locating}
            onClick={() =>
              locate(() => {
                setAreaId("cyprus");
                reset();
              })
            }
          />
        </ToolFilters>
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <MapView
              points={points}
              bounds={area.bounds}
              selectedId={selectedId ?? undefined}
              position={position}
              onSelect={choose}
              markerSymbol={symbols.glass}
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={query.isPending ? t("loading") : t("recyclingCount", { count: matching.length })}
            action={position && <NearestFirst onClear={clearLocation} />}
            notes={[t("recyclingNote")]}
          >
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("recyclingLoadError")}</ResultsMessage>
            )}
            {query.data && all.length === 0 && (
              <ResultsMessage kind="pending">{t("recyclingUnavailable")}</ResultsMessage>
            )}
            {query.data && all.length > 0 && matching.length === 0 && (
              <ResultsMessage
                kind="empty"
                onReset={() => {
                  setAreaId("cyprus");
                  setKind("all");
                }}
              >
                {t("recyclingNoMatches")}
              </ResultsMessage>
            )}
            {selected && (
              <SelectedResult
                label={t(kindLabels[selected.kind])}
                title={selected.description ?? town(selected)}
                detail={[t(contents[selected.kind]), selected.description ? town(selected) : null]
                  .filter(Boolean)
                  .join(", ")}
              >
                <a
                  href={googleDirections(`${selected.latitude},${selected.longitude}`)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Navigation size={15} aria-hidden="true" />
                  {t("directions")}
                  <ArrowUpRight size={14} aria-hidden="true" />
                </a>
              </SelectedResult>
            )}
            <ol className="result-list" aria-label={t("recyclingPoints")}>
              {sorted.slice(0, shown).map((point) => (
                <ResultRow
                  key={point.id}
                  icon={symbols[point.kind]}
                  tone={point.kind}
                  title={t(kindLabels[point.kind])}
                  details={[
                    point.description && <span lang="el">{point.description}</span>,
                    [town(point), position ? distanceLabel(distance(position, point)) : null]
                      .filter(Boolean)
                      .join(", "),
                  ]}
                  selected={selected?.id === point.id}
                  onSelect={() => choose(point.id)}
                />
              ))}
            </ol>
            {sorted.length > shown && <ShowMore onClick={() => setShown((count) => count + PAGE)} />}
          </ResultsPanel>
        </ToolLayout>
        <SourceAttribution
          href={query.data?.source.url ?? recyclingSourceUrl}
          badge={<Recycle size={20} aria-hidden="true" />}
          name={t("greenDot")}
          facts={[
            ...sourceDates(query.data?.source.publishedAt, query.data?.source.retrievedAt),
            <ExternalLink href={ccBySaLicenseUrl}>CC BY-SA 4.0</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

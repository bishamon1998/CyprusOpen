import { useMemo, useRef, useState } from "react";
import { Wind } from "lucide-react";
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
  SourceAttribution,
  ToolFeedback,
  ToolFilters,
  ToolLayout,
  ToolWorkspace,
} from "../components/tool/Tool";
import { useDistanceLabel, useSourceDates } from "../format";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { airQualityIndexUrl, airQualityUrl, ccBySaLicenseUrl, osmCopyrightUrl } from "../sources";
import { type Level, levels, type Station, useAirQuality } from "./api";
import "./air.css";

/** Readings older than this are shown with a warning: the source normally publishes every hour. */
const STALE_AFTER = 3 * 60 * 60_000;

const levelLabels: Record<Level, MessageKey> = {
  good: "airGood",
  fair: "airFair",
  moderate: "airModerate",
  poor: "airPoor",
  "very-poor": "airVeryPoor",
  "extremely-poor": "airExtremelyPoor",
};

const advice: Record<Level, [MessageKey, MessageKey]> = {
  good: ["airAdviceGood", "airAdviceGood"],
  fair: ["airAdviceFair", "airAdviceFair"],
  moderate: ["airAdviceFair", "airAdviceModerateSensitive"],
  poor: ["airAdvicePoor", "airAdvicePoorSensitive"],
  "very-poor": ["airAdvicePoor", "airAdviceVeryPoorSensitive"],
  "extremely-poor": ["airAdviceExtremelyPoor", "airAdviceExtremelyPoorSensitive"],
};

const pollutantNames: Record<string, string> = {
  pm25: "PM2.5",
  pm10: "PM10",
  o3: "O₃",
  no2: "NO₂",
  so2: "SO₂",
  co: "CO",
};

const stationTypes: Record<string, MessageKey> = {
  Traffic: "stationTraffic",
  Urban: "stationUrban",
  Industrial: "stationIndustrial",
  Rural: "stationRural",
};

function LevelChip({ level }: { level: Level | null }) {
  const { t } = useI18n();
  if (!level) return null;
  return <span className={"air-level tone-" + level}>{t(levelLabels[level])}</span>;
}

export default function AirQuality() {
  const { t, language } = useI18n();
  const results = useRef<HTMLDivElement>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useAirQuality();
  const stations = useMemo(() => query.data?.stations ?? [], [query.data]);

  const name = (station: Station) => (language === "el" ? station.name.el : station.name.en);
  const distanceTo = (station: Station) => (position ? distance(position, station) : null);
  const rank = (station: Station) => (station.level ? levels.indexOf(station.level) : -1);
  const sorted = position
    ? [...stations].sort((a, b) => (distanceTo(a) ?? Infinity) - (distanceTo(b) ?? Infinity))
    : [...stations].sort((a, b) => rank(b) - rank(a) || name(a).localeCompare(name(b)));
  const selected = stations.find((station) => station.id === selectedId);

  const points = useMemo(
    () =>
      stations.map((station) => ({
        id: station.id,
        label: language === "el" ? station.name.el : station.name.en,
        latitude: station.latitude,
        longitude: station.longitude,
        tone: station.level ?? undefined,
      })),
    [stations, language],
  );
  const labels = useMapLabels("airQuality", "stationCount");
  const distanceLabel = useDistanceLabel();
  const sourceDates = useSourceDates();

  const time = (value: string) =>
    new Date(value).toLocaleString(language === "en" ? "en-GB" : language, {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
      timeZone: "Asia/Nicosia",
    });
  const latest = stations.reduce<string | null>(
    (newest, station) => (!newest || station.measuredAt > newest ? station.measuredAt : newest),
    null,
  );
  const stale = latest !== null && Date.now() - new Date(latest).getTime() > STALE_AFTER;
  const number = new Intl.NumberFormat(language, { maximumFractionDigits: 1 });
  const detail = (station: Station) => {
    const away = distanceTo(station);
    const type = stationTypes[station.type];
    return [
      <LevelChip level={station.level} />,
      type ? t(type) : station.type,
      away !== null ? distanceLabel(away) : null,
    ];
  };

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  return (
    <>
      <PageTitle title={t("airQuality")} />
      <ToolWorkspace label={t("airQuality")}>
        <ToolFilters>
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
              markerSymbol="≋"
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={query.isPending ? t("loading") : t("stationCount", { count: stations.length })}
            action={position && <NearestFirst onClear={clearLocation} />}
            notes={[
              latest && t("airMeasuredAt", { time: time(latest) }),
              stale && t("airStale"),
            ]}
          >
            <ul className="air-legend" aria-label={t("airIndex")}>
              {levels.map((level) => (
                <li key={level}>
                  <LevelChip level={level} />
                </li>
              ))}
            </ul>
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("airLoadError")}</ResultsMessage>
            )}
            {query.data && stations.length === 0 && <ResultsMessage>{t("airUnavailable")}</ResultsMessage>}
            {selected && (
              <SelectedResult label={t("selectedStation")} title={name(selected)} detail={time(selected.measuredAt)}>
                <table className="air-readings">
                  <thead>
                    <tr>
                      <th scope="col">{t("pollutant")}</th>
                      <th scope="col">µg/m³</th>
                      <th scope="col">{t("airIndex")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selected.readings.map((reading) => (
                      <tr key={reading.pollutant}>
                        <th scope="row">{pollutantNames[reading.pollutant] ?? reading.pollutant}</th>
                        <td>{number.format(reading.value)}</td>
                        <td>
                          <LevelChip level={reading.level} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {selected.level && (
                  <div className="air-advice">
                    <p>
                      <strong>{t("airEveryone")}:</strong> {t(advice[selected.level][0])}
                    </p>
                    <p>
                      <strong>{t("airSensitive")}:</strong> {t(advice[selected.level][1])}
                    </p>
                  </div>
                )}
              </SelectedResult>
            )}
            <ol className="result-list" aria-label={t("airStations")}>
              {sorted.map((station) => (
                <ResultRow
                  key={station.id}
                  icon="≋"
                  tone={station.level ?? undefined}
                  title={name(station)}
                  lang={language === "el" ? "el" : "en"}
                  details={detail(station)}
                  selected={selected?.id === station.id}
                  onSelect={() => choose(station.id)}
                />
              ))}
            </ol>
          </ResultsPanel>
        </ToolLayout>
        <SourceAttribution
          href={query.data?.source.url ?? airQualityUrl}
          badge={<Wind size={20} aria-hidden="true" />}
          name={t("labourInspection")}
          facts={[
            ...sourceDates(query.data?.source.measuredAt, query.data?.source.retrievedAt, true),
            <ExternalLink href={airQualityIndexUrl}>{t("airIndexSource")}</ExternalLink>,
            <ExternalLink href={ccBySaLicenseUrl}>CC BY-SA 4.0</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

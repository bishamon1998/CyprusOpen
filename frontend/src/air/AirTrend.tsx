import { lazy, Suspense, useMemo, useState } from "react";
import { type ChartOption } from "../components/Chart";
import { SegmentedControl } from "../components/SegmentedControl";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { type Level, levels, type Station, useStationHistory } from "./api";

const Chart = lazy(() => import("../components/Chart"));

/** Hours shown as the row of recent results. */
const RECENT = 8;
const DAY = 24 * 60 * 60_000;

/** Same colours as the CSS tones of the European Air Quality Index (air.css). */
const levelColors: Record<Level, string> = {
  good: "#50f0e6",
  fair: "#50ccaa",
  moderate: "#f0e641",
  poor: "#ff5050",
  "very-poor": "#960032",
  "extremely-poor": "#7d2181",
};
const NO_LEVEL = "#8a909d";

type Range = "day" | "week";

/**
 * How a station's air changed: its last hours as a row and a chart of one pollutant over a day or a week. The
 * site collects the hours itself (the source only publishes the current one), so a new station starts short.
 */
export function AirTrend({
  station,
  pollutantNames,
  levelLabels,
}: {
  station: Station;
  pollutantNames: Record<string, string>;
  levelLabels: Record<Level, MessageKey>;
}) {
  const { t, language } = useI18n();
  const history = useStationHistory(station.id);
  const [range, setRange] = useState<Range>("day");
  const [picked, setPicked] = useState<string | null>(null);

  // The pollutant that sets the station's level is the one worth watching first.
  const rank = (level: Level | null) => (level ? levels.indexOf(level) : -1);
  const worst = [...station.readings].sort((a, b) => rank(b.level) - rank(a.level))[0]?.pollutant;
  const pollutant = picked && station.readings.some((reading) => reading.pollutant === picked) ? picked : worst;
  const name = (key: string) => pollutantNames[key] ?? key;

  const locale = language === "en" ? "en-GB" : language;
  const hourFormat = useMemo(
    () => new Intl.DateTimeFormat(locale, { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Nicosia" }),
    [locale],
  );
  const dayFormat = useMemo(
    () => new Intl.DateTimeFormat(locale, { day: "numeric", month: "short", timeZone: "Asia/Nicosia" }),
    [locale],
  );
  const fullFormat = useMemo(
    () =>
      new Intl.DateTimeFormat(locale, {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "Asia/Nicosia",
      }),
    [locale],
  );
  const number = useMemo(() => new Intl.NumberFormat(language, { maximumFractionDigits: 1 }), [language]);

  const hours = useMemo(() => history.data?.hours ?? [], [history.data]);
  const series = useMemo(
    () =>
      hours.flatMap((hour) => {
        const reading = hour.readings.find((item) => item.pollutant === pollutant);
        return reading ? [{ time: new Date(hour.measuredAt).getTime(), value: reading.value, level: reading.level }] : [];
      }),
    [hours, pollutant],
  );
  const newest = series.at(-1)?.time ?? 0;
  const shown = range === "day" ? series.filter((point) => point.time > newest - DAY) : series;
  const recent = series.slice(-RECENT);

  const option = useMemo<ChartOption>(
    () => ({
      grid: { left: 44, right: 12, top: 24, bottom: 28 },
      tooltip: {
        trigger: "axis",
        formatter: (params: { data: { value: [number, number]; level: Level | null } }[]) => {
          const point = params[0]?.data;
          if (!point) return "";
          const level = point.level ? " · " + t(levelLabels[point.level]) : "";
          return `${fullFormat.format(point.value[0])}<br>${number.format(point.value[1])} µg/m³${level}`;
        },
      },
      xAxis: {
        type: "time",
        axisLabel: {
          hideOverlap: true,
          formatter: (value: number) => (range === "day" ? hourFormat : dayFormat).format(value),
        },
        splitLine: { show: false },
      },
      yAxis: {
        type: "value",
        name: "µg/m³",
        nameTextStyle: { align: "left", padding: [0, 0, 0, -36] },
        splitLine: { lineStyle: { color: "#eef0f4" } },
      },
      series: [
        {
          type: "line",
          showSymbol: true,
          symbol: "circle",
          symbolSize: range === "day" ? 7 : 4,
          lineStyle: { color: "#c9cedb", width: 2 },
          data: shown.map((point) => ({
            value: [point.time, point.value],
            level: point.level,
            itemStyle: { color: point.level ? levelColors[point.level] : NO_LEVEL, borderColor: "#fff", borderWidth: 1 },
          })),
        },
      ],
    }),
    [shown, range, hourFormat, dayFormat, fullFormat, number, t, levelLabels],
  );

  if (!pollutant) return null;
  return (
    <section className="air-trend" aria-label={t("airTrend")}>
      <h3>{t("airTrend")}</h3>
      {station.readings.length > 1 && (
        <SegmentedControl
          label={t("pollutant")}
          value={pollutant}
          options={station.readings.map((reading) => ({ value: reading.pollutant, label: name(reading.pollutant) }))}
          onChange={setPicked}
        />
      )}
      {history.isError && <p className="air-trend-note">{t("airHistoryError")}</p>}
      {history.data && series.length < 2 && <p className="air-trend-note">{t("airHistoryCollecting")}</p>}
      {recent.length > 1 && (
        <>
          <h4>{t("airRecentHours")}</h4>
          <ol className="air-recent">
            {recent.map((point) => (
              <li key={point.time}>
                <time dateTime={new Date(point.time).toISOString()}>{hourFormat.format(point.time)}</time>
                <span className={"air-recent-value" + (point.level ? " tone-" + point.level : "")}>
                  {number.format(point.value)}
                </span>
                {point.level && <span className="visually-hidden">{t(levelLabels[point.level])}</span>}
              </li>
            ))}
          </ol>
        </>
      )}
      {series.length > 1 && (
        <>
          <div className="air-trend-head">
            <SegmentedControl
              label={t("airTrend")}
              value={range}
              options={[
                { value: "day", label: t("airRangeDay") },
                { value: "week", label: t("airRangeWeek") },
              ]}
              onChange={setRange}
            />
          </div>
          <Suspense fallback={<div className="air-trend-chart" style={{ height: 190 }} />}>
            <Chart
              option={option}
              height={190}
              label={t("airTrendChart", { pollutant: name(pollutant), station: station.name.en })}
            />
          </Suspense>
          <p className="air-trend-note">
            {t("airHistorySince", {
              from: fullFormat.format(shown[0].time),
              to: fullFormat.format(shown.at(-1)!.time),
            })}
          </p>
        </>
      )}
    </section>
  );
}

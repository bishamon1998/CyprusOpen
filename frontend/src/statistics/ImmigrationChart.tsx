import { useMemo, useState } from "react";
import { SegmentedControl } from "../components/SegmentedControl";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { useSeriesTable, type SeriesTable } from "./api";
import { ChartCard } from "./ChartCard";
import { axisStyle, chartState, tooltipStyle } from "./chartState";
import { valuesByPeriod } from "./series";

type View = "citizenship" | "residence" | "trend";

const groups: Record<Exclude<View, "trend">, { key: string; label: MessageKey }[]> = {
  citizenship: [
    { key: "citizenship-cyprus", label: "groupCyprus" },
    { key: "citizenship-eu", label: "groupEu" },
    { key: "citizenship-non-eu", label: "groupNonEu" },
  ],
  residence: [
    { key: "previous-residence-eu", label: "groupEu" },
    { key: "previous-residence-non-eu", label: "groupNonEu" },
  ],
};

const trend: { key: string; label: MessageKey }[] = [
  { key: "immigrants", label: "immigrants" },
  { key: "emigrants", label: "emigrants" },
];

export function ImmigrationChart() {
  const { t, language } = useI18n();
  const [view, setView] = useState<View>("citizenship");
  const groupsTable = useSeriesTable("immigrant-groups");
  const movementsTable = useSeriesTable("migration-movements");
  const table = view === "trend" ? movementsTable : groupsTable;
  const format = useMemo(() => new Intl.NumberFormat(language), [language]);

  const state = chartState([table], ([data]: SeriesTable[]) => {
    const lines =
      view === "trend"
        ? trend.map((item) => ({
            name: t(item.label),
            type: "line",
            showSymbol: false,
            data: valuesByPeriod(data, item.key),
          }))
        : groups[view].map((item) => ({
            name: t(item.label),
            type: "bar",
            stack: "total",
            barMaxWidth: 34,
            data: valuesByPeriod(data, item.key),
          }));
    return {
      status: "ready",
      height: 320,
      label: t("immigrationChartLabel"),
      option: {
        grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
        legend: { top: 0, left: 0, icon: "roundRect", itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
        tooltip: { ...tooltipStyle, valueFormatter: (value: number | null) => (value === null ? "–" : format.format(value)) },
        xAxis: { type: "category", data: data.periods, ...axisStyle },
        yAxis: { type: "value", ...axisStyle, axisLabel: { ...axisStyle.axisLabel, formatter: (value: number) => format.format(value) } },
        series: lines,
      },
    };
  });

  const latest = groupsTable.data;
  const latestIndex = latest ? latest.periods.length - 1 : -1;
  const latestTotal = latest && latestIndex >= 0 ? valuesByPeriod(latest, "total")[latestIndex] : null;

  return (
    <ChartCard
      title={t("immigration")}
      description={t("immigrationDescription")}
      illustration="people"
      state={state}
      source={table.data?.source}
      controls={
        <SegmentedControl
          label={t("view")}
          value={view}
          onChange={setView}
          options={[
            { value: "citizenship", label: t("byCitizenship") },
            { value: "residence", label: t("byPreviousResidence") },
            { value: "trend", label: t("since1981") },
          ]}
        />
      }
      summary={
        latest && latestTotal !== null
          ? t("immigrationSummary", { year: latest.periods[latestIndex], count: format.format(latestTotal) })
          : undefined
      }
    />
  );
}

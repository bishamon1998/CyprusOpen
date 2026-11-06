import { useMemo, useState } from "react";
import { SegmentedControl } from "../components/SegmentedControl";
import { type IllustrationKind } from "../components/Illustration";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { type SeriesTable, useSeriesTable } from "./api";
import { ChartCard, type ChartState } from "./ChartCard";
import { axisStyle, chartState, tooltipStyle } from "./chartState";
import { type Unit, useAxisFormat, useValueFormat } from "./format";
import {
  countryName,
  defaultYear,
  isMonthly,
  seasonComplete,
  seasonPeriods,
  seasons,
  totals,
  valuesByPeriod,
  yearsOf,
  type Season,
} from "./series";

const TOTAL = "total";

type Labels = Record<string, MessageKey> | "countries";

/** A trend over time: lines, bars, or bars stacked into a total. */
export type TrendView = {
  kind: "line" | "bar" | "stacked";
  label: MessageKey;
  dataset: string;
  unit: Unit;
  series: { key: string; label: MessageKey }[];
};

/** Categories compared for one year (and season, for monthly tables), largest first. */
export type RankingView = {
  kind: "ranking";
  label: MessageKey;
  dataset: string;
  unit: Unit;
  labels: Labels;
  top?: number;
  seasons?: boolean;
};

export type ChartView = TrendView | RankingView;

export type StatisticCardConfig = {
  id: string;
  title: MessageKey;
  description: MessageKey;
  illustration: IllustrationKind;
  views: ChartView[];
};

const seasonLabels: Record<Season, MessageKey> = {
  year: "wholeYear",
  winter: "winter",
  spring: "spring",
  summer: "summer",
  autumn: "autumn",
};

/**
 * One statistics card. Its views share the frame, source footer and summary; each view reads one
 * dataset and is drawn as a trend or a ranking. New charts are configured in `cards.ts`.
 */
export function StatisticCard({ card }: { card: StatisticCardConfig }) {
  const { t, language } = useI18n();
  const value = useValueFormat();
  const axis = useAxisFormat();
  const [viewIndex, setViewIndex] = useState(0);
  const [chosenYear, setYear] = useState<string | null>(null);
  const [season, setSeason] = useState<Season>("year");
  const view = card.views[viewIndex];
  const query = useSeriesTable(view.dataset);
  const table = query.data;

  const label = (labels: Labels, key: string) =>
    labels === "countries" ? countryName(key, language) : t(labels[key] ?? "other");

  const ranking = useMemo(() => {
    if (view.kind !== "ranking" || !table || table.series.length === 0) return null;
    const years = yearsOf(table);
    const year = chosenYear && years.includes(chosenYear) ? chosenYear : defaultYear(table);
    const period = view.seasons ? season : "year";
    const all = totals(table, seasonPeriods(table, year, period));
    return {
      years,
      year,
      complete: seasonComplete(table, year, period),
      total: all.find((item) => item.key === TOTAL)?.value ?? null,
      items: all
        .filter((item) => item.key !== TOTAL && (view.labels === "countries" || item.key in view.labels))
        .sort((a, b) => b.value - a.value)
        .slice(0, view.top ?? 12),
    };
  }, [view, table, chosenYear, season]);

  const state: ChartState = chartState([query], ([data]: SeriesTable[]) => {
    if (view.kind === "ranking") {
      if (!ranking || ranking.items.length === 0) return { status: "empty" };
      const rows = [...ranking.items].reverse();
      return {
        status: "ready",
        height: 64 + rows.length * 30,
        label: t(card.title) + " · " + t(view.label),
        option: {
          grid: { left: 8, right: 64, top: 8, bottom: 8, containLabel: true },
          tooltip: {
            ...tooltipStyle,
            axisPointer: { type: "shadow" },
            valueFormatter: (amount: number) => value(amount, view.unit),
          },
          xAxis: { type: "value", ...axisStyle, axisLabel: { show: false } },
          yAxis: {
            type: "category",
            data: rows.map((item) => label(view.labels, item.key) + (item.lowReliability ? " *" : "")),
            ...axisStyle,
            axisLabel: { ...axisStyle.axisLabel, color: "#191c24", fontSize: 12, width: 150, overflow: "truncate" },
          },
          series: [
            {
              type: "bar",
              name: t(view.label),
              data: rows.map((item) => item.value),
              barMaxWidth: 18,
              itemStyle: { borderRadius: [0, 4, 4, 0] },
              label: {
                show: true,
                position: "right",
                color: "#727784",
                fontSize: 11,
                formatter: ({ value: amount }: { value: number }) => value(amount, view.unit),
              },
            },
          ],
        },
      };
    }
    const lines = view.series.map((item) => {
      const values = valuesByPeriod(data, item.key);
      return {
        name: t(item.label),
        type: view.kind === "line" ? "line" : "bar",
        ...(view.kind === "stacked" ? { stack: "total" } : {}),
        // Years a table does not publish are null. Without connectNulls a value between two gaps has no
        // segment and, with symbols hidden, stays invisible until hovered; a lone value still needs its symbol.
        ...(view.kind === "line"
          ? { connectNulls: true, showSymbol: values.filter((amount) => amount !== null).length < 2 }
          : { barMaxWidth: 34 }),
        data: values,
      };
    });
    return {
      status: "ready",
      height: 320,
      label: t(card.title) + " · " + t(view.label),
      option: {
        grid: { left: 8, right: 8, top: lines.length > 1 ? 36 : 12, bottom: 8, containLabel: true },
        legend: lines.length > 1
          ? { top: 0, left: 0, icon: "roundRect", itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }
          : { show: false },
        tooltip: { ...tooltipStyle, valueFormatter: (amount: number | null) => value(amount, view.unit) },
        xAxis: { type: "category", data: data.periods, ...axisStyle },
        yAxis: {
          type: "value",
          scale: view.kind === "line",
          ...axisStyle,
          axisLabel: { ...axisStyle.axisLabel, formatter: (amount: number) => axis(amount, view.unit) },
        },
        series: lines,
      },
    };
  });

  const summary = () => {
    if (!table) return undefined;
    if (view.kind === "ranking") {
      if (!ranking) return undefined;
      const notes = [
        ranking.total !== null && t("totalValue", { value: value(ranking.total, view.unit) }),
        !ranking.complete && t("partialPeriod"),
        ranking.items.some((item) => item.lowReliability) && t("lowReliability"),
      ].filter(Boolean);
      return notes.length ? notes.join(" · ") : undefined;
    }
    if (view.kind === "stacked") {
      const parts = view.series.map((item) => valuesByPeriod(table, item.key));
      let last = table.periods.length - 1;
      while (last >= 0 && parts.every((values) => values[last] === null)) last--;
      if (last < 0) return undefined;
      const sum = parts.reduce((total, values) => total + (values[last] ?? 0), 0);
      return t("latestTotal", { period: table.periods[last], value: value(sum, view.unit) });
    }
    const first = view.series[0];
    const values = valuesByPeriod(table, first.key);
    let last = values.length - 1;
    while (last >= 0 && values[last] === null) last--;
    if (last < 0) return undefined;
    return t("latestValue", {
      period: table.periods[last],
      series: t(first.label),
      value: value(values[last], view.unit),
    });
  };

  const controls = (
    <>
      {card.views.length > 1 && (
        <SegmentedControl
          label={t("view")}
          value={String(viewIndex)}
          onChange={(index) => setViewIndex(Number(index))}
          options={card.views.map((item, index) => ({ value: String(index), label: t(item.label) }))}
        />
      )}
      {ranking && (
        <select
          className="chart-select"
          aria-label={t("year")}
          value={ranking.year}
          onChange={(event) => setYear(event.target.value)}
        >
          {ranking.years.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      )}
      {ranking && view.kind === "ranking" && view.seasons && table && isMonthly(table) && (
        <SegmentedControl
          label={t("season")}
          value={season}
          onChange={setSeason}
          options={seasons.map((option) => ({ value: option, label: t(seasonLabels[option]) }))}
        />
      )}
    </>
  );

  return (
    <ChartCard
      title={t(card.title)}
      description={t(card.description)}
      illustration={card.illustration}
      state={state}
      source={table?.source}
      controls={controls}
      summary={summary()}
    />
  );
}

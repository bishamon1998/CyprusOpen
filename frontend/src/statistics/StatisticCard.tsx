import { useMemo, useState } from "react";
import { SegmentedControl } from "../components/SegmentedControl";
import { type IllustrationKind } from "../components/Illustration";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { type SeriesTable, useSeriesTable } from "./api";
import { ChartCard, type ChartState } from "./ChartCard";
import { ALL_YEARS, calendarDays, extremes } from "./calendar";
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

/** Counts by day of the year (series keys "MM-DD"), drawn as a month × day heatmap. */
export type CalendarView = {
  kind: "calendar";
  label: MessageKey;
  dataset: string;
  /** What one cell counts, e.g. "Births per day". */
  measure: MessageKey;
};

export type ChartView = TrendView | RankingView | CalendarView;

/** Heatmap colours from the quietest to the busiest day. */
const calendarColors = ["#eef1fd", "#9aa9f5", "#4263eb", "#1d2b8f"];

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
 * dataset and is drawn as a trend, a ranking or a calendar heatmap. New charts are configured in `cards.ts`.
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

  const decimal = useMemo(() => new Intl.NumberFormat(language, { maximumFractionDigits: 1 }), [language]);
  const date = useMemo(() => {
    const format = new Intl.DateTimeFormat(language, { day: "numeric", month: "long" });
    // 2024 is a leap year, so 29 February formats too.
    return (month: number, day: number) => format.format(new Date(2024, month - 1, day));
  }, [language]);
  const monthNames = useMemo(() => {
    const format = new Intl.DateTimeFormat(language, { month: "short" });
    return Array.from({ length: 12 }, (_, month) => format.format(new Date(2024, month, 1)));
  }, [language]);

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

  const calendar = useMemo(() => {
    if (view.kind !== "calendar" || !table || table.series.length === 0) return null;
    const years = yearsOf(table);
    const year = chosenYear === ALL_YEARS || (chosenYear && years.includes(chosenYear)) ? chosenYear : ALL_YEARS;
    const days = calendarDays(table, year);
    return { years, year, days, extremes: extremes(days) };
  }, [view, table, chosenYear]);

  const state: ChartState = chartState([query], ([data]: SeriesTable[]) => {
    if (view.kind === "calendar") {
      if (!calendar || calendar.days.length === 0) return { status: "empty" };
      const amounts = calendar.days.map((day) => day.value);
      const amount = (count: number) => (calendar.year === ALL_YEARS ? decimal.format(count) : value(count, "count"));
      return {
        status: "ready",
        height: 380,
        label: t(card.title) + " · " + t(view.label),
        option: {
          grid: { left: 8, right: 8, top: 4, bottom: 44, containLabel: true },
          tooltip: {
            ...tooltipStyle,
            trigger: "item",
            formatter: ({ data: [day, month, count] }: { data: [number, number, number] }) =>
              date(month + 1, day + 1) + "<br/>" + t(view.measure) + ": <b>" + amount(count) + "</b>",
          },
          xAxis: {
            type: "category",
            data: Array.from({ length: 31 }, (_, day) => String(day + 1)),
            ...axisStyle,
            splitLine: { show: false },
            axisLabel: { ...axisStyle.axisLabel, interval: (index: number) => index === 0 || (index + 1) % 5 === 0 },
          },
          yAxis: { type: "category", data: monthNames, inverse: true, ...axisStyle, splitLine: { show: false } },
          visualMap: {
            min: Math.min(...amounts),
            max: Math.max(...amounts),
            text: [amount(Math.max(...amounts)), amount(Math.min(...amounts))],
            textGap: 8,
            orient: "horizontal",
            left: "center",
            bottom: 0,
            itemWidth: 10,
            itemHeight: 140,
            calculable: false,
            inRange: { color: calendarColors },
            textStyle: { color: "#727784", fontSize: 11 },
          },
          series: [
            {
              type: "heatmap",
              name: t(view.measure),
              data: calendar.days.map((day) => [day.day - 1, day.month - 1, Math.round(day.value * 10) / 10]),
              itemStyle: { borderColor: "#fff", borderWidth: 1, borderRadius: 2 },
              emphasis: { itemStyle: { borderColor: "#191c24", borderWidth: 1 } },
            },
          ],
        },
      };
    }
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
        // Lines appear through an animated SVG clipPath; iOS Safari does not repaint when that clipPath grows,
        // so the line stayed hidden until a tap. Without the animation the clip is drawn at full width at once.
        ...(view.kind === "line"
          ? {
              animation: false,
              connectNulls: true,
              showSymbol: values.filter((amount) => amount !== null).length < 2,
            }
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
    if (view.kind === "calendar") {
      if (!calendar?.extremes) return undefined;
      const { most, least } = calendar.extremes;
      const amount = (count: number) => (calendar.year === ALL_YEARS ? decimal.format(count) : value(count, "count"));
      return t("calendarExtremes", {
        most: date(most.month, most.day),
        mostValue: amount(most.value),
        least: date(least.month, least.day),
        leastValue: amount(least.value),
      });
    }
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
      {calendar && (
        <select
          className="chart-select"
          aria-label={t("year")}
          value={calendar.year}
          onChange={(event) => setYear(event.target.value)}
        >
          <option value={ALL_YEARS}>
            {t("allYearsAverage", {
              from: calendar.years[calendar.years.length - 1],
              to: calendar.years[0],
            })}
          </option>
          {calendar.years.map((option) => (
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

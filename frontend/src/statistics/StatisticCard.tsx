import { useMemo, useState } from "react";
import { SegmentedControl } from "../components/SegmentedControl";
import { type IllustrationKind } from "../components/Illustration";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { type SeriesTable, useSeriesTable } from "./api";
import { ChartCard, type ChartState } from "./ChartCard";
import { ALL_YEARS, calendarDays, extremes } from "./calendar";
import { monthProfile, pairMatrix, PROFILE_YEARS } from "./profiles";
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

/** Category labels: message keys, localized country names for ISO codes, or the keys as they are ("<1", "10–14"). */
type Labels = Record<string, MessageKey> | "countries" | "keys";

/** A trend over time: lines, bars, or bars stacked into a total. */
export type TrendView = {
  kind: "line" | "bar" | "stacked";
  label: MessageKey;
  dataset: string;
  unit: Unit;
  /** `color` overrides the shared palette, e.g. to keep a category recognisable across views. */
  series: { key: string; label: MessageKey; color?: string }[];
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
  /** Reads only series starting with this prefix (a table holding several measures: "day-DE", "trip-DE"). */
  prefix?: string;
  /** Fixed display order of keys (for ordered groups such as years of marriage); largest first when absent. */
  order?: string[];
  /** Summary for the total series; "Total: {value}" by default. */
  totalLabel?: MessageKey;
};

/** Counts by day of the year (series keys "MM-DD"), drawn as a month × day heatmap. */
export type CalendarView = {
  kind: "calendar";
  label: MessageKey;
  dataset: string;
  /** What one cell counts, e.g. "Births per day". */
  measure: MessageKey;
};

/** Average per day in each month of the year (series keys "01"–"12"), as bars. */
export type MonthsView = {
  kind: "months";
  label: MessageKey;
  dataset: string;
  measure: MessageKey;
};

/** Counts by pair (series keys "row|column", e.g. groom|bride nationality), as a heatmap. */
export type MatrixView = {
  kind: "matrix";
  label: MessageKey;
  dataset: string;
  rowsLabel: MessageKey;
  columnsLabel: MessageKey;
};

export type ChartView = TrendView | RankingView | CalendarView | MonthsView | MatrixView;

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
 * dataset and is drawn as a trend, a ranking, a month profile or a calendar or pair heatmap. New charts are configured in `cards.ts`.
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
    labels === "countries"
      ? countryName(key, language)
      : labels === "keys"
        ? key
        : t(labels[key] ?? "other");
  const country = (code: string) => (code === "other" ? t("other") : countryName(code, language));

  const ranking = useMemo(() => {
    if (view.kind !== "ranking" || !table || table.series.length === 0) return null;
    const years = yearsOf(table);
    const year = chosenYear && years.includes(chosenYear) ? chosenYear : defaultYear(table);
    const period = view.seasons ? season : "year";
    const prefix = view.prefix ?? "";
    const all = totals(table, seasonPeriods(table, year, period))
      .filter((item) => item.key.startsWith(prefix))
      .map((item) => ({ ...item, key: item.key.slice(prefix.length) }));
    const order = view.order ?? null;
    // A measure's own total ("day-total") or, for tables with one total, the plain "total" series.
    const total = all.find((item) => item.key === TOTAL) ??
      totals(table, seasonPeriods(table, year, period)).find((item) => item.key === TOTAL);
    return {
      years,
      year,
      complete: seasonComplete(table, year, period),
      total: total?.value ?? null,
      items: all
        .filter((item) => item.key !== TOTAL && (order ? order.includes(item.key) : typeof view.labels === "string" || item.key in view.labels))
        .sort((a, b) => (order ? order.indexOf(a.key) - order.indexOf(b.key) : b.value - a.value))
        .slice(0, view.top ?? 12),
    };
  }, [view, table, chosenYear, season]);

  /** Views that can show one year or every year together ("all"): which years exist and which is chosen. */
  const span = useMemo(() => {
    if ((view.kind !== "calendar" && view.kind !== "months" && view.kind !== "matrix") || !table) return null;
    if (table.series.length === 0) return null;
    const years = yearsOf(table);
    const year = chosenYear === ALL_YEARS || (chosenYear && years.includes(chosenYear)) ? chosenYear : ALL_YEARS;
    const covered = view.kind === "months" ? years.slice(0, PROFILE_YEARS) : years;
    const allLabel = t(view.kind === "matrix" ? "allYearsTotal" : "allYearsAverage", {
      from: covered[covered.length - 1],
      to: covered[0],
    });
    return { years, year, allLabel };
  }, [view, table, chosenYear, t]);

  const calendar = useMemo(() => {
    if (view.kind !== "calendar" || !table || !span) return null;
    const days = calendarDays(table, span.year);
    return { year: span.year, days, extremes: extremes(days) };
  }, [view, table, span]);

  const months = useMemo(() => {
    if (view.kind !== "months" || !table || !span) return null;
    const values = monthProfile(table, span.year);
    const sorted = [...values].sort((a, b) => b.value - a.value);
    return { values, most: sorted[0], least: sorted[sorted.length - 1] };
  }, [view, table, span]);

  const matrix = useMemo(() => {
    if (view.kind !== "matrix" || !table || !span) return null;
    return pairMatrix(table, span.year);
  }, [view, table, span]);

  const state: ChartState = chartState([query], ([data]: SeriesTable[]) => {
    if (view.kind === "months") {
      if (!months || months.values.length === 0) return { status: "empty" };
      return {
        status: "ready",
        height: 300,
        label: t(card.title) + " · " + t(view.label),
        option: {
          grid: { left: 8, right: 8, top: 12, bottom: 8, containLabel: true },
          tooltip: {
            ...tooltipStyle,
            axisPointer: { type: "shadow" },
            valueFormatter: (amount: number) => decimal.format(amount),
          },
          xAxis: { type: "category", data: months.values.map((item) => monthNames[item.month - 1]), ...axisStyle },
          yAxis: { type: "value", ...axisStyle },
          series: [
            {
              type: "bar",
              name: t(view.measure),
              barMaxWidth: 28,
              itemStyle: { borderRadius: [4, 4, 0, 0] },
              data: months.values.map((item) => ({
                value: Math.round(item.value * 10) / 10,
                itemStyle: item === months.most ? { color: "#1d2b8f" } : item === months.least ? { color: "#9aa9f5" } : {},
              })),
            },
          ],
        },
      };
    }
    if (view.kind === "matrix") {
      if (!matrix || matrix.cells.length === 0) return { status: "empty" };
      const counts = matrix.cells.map(([, , count]) => count).sort((a, b) => b - a);
      // The largest pair (both Cypriot) would wash out every other cell; the scale tops out at the second.
      const max = counts[1] ?? counts[0];
      return {
        status: "ready",
        height: 120 + matrix.rows.length * 26,
        label: t(card.title) + " · " + t(view.label),
        option: {
          grid: { left: 8, right: 8, top: 8, bottom: 40, containLabel: true },
          tooltip: {
            ...tooltipStyle,
            trigger: "item",
            formatter: ({ value: [column, row, count] }: { value: [number, number, number] }) =>
              t(view.rowsLabel) + ": " + country(matrix.rows[row]) + "<br/>" +
              t(view.columnsLabel) + ": " + country(matrix.columns[column]) + "<br/><b>" + value(count, "count") + "</b>",
          },
          xAxis: {
            type: "category",
            position: "top",
            data: matrix.columns.map(country),
            ...axisStyle,
            splitLine: { show: false },
            axisLabel: { ...axisStyle.axisLabel, rotate: 50, fontSize: 10, width: 70, overflow: "truncate", interval: 0 },
          },
          yAxis: {
            type: "category",
            data: matrix.rows.map(country),
            inverse: true,
            ...axisStyle,
            splitLine: { show: false },
            axisLabel: { ...axisStyle.axisLabel, fontSize: 10, width: 80, overflow: "truncate", interval: 0 },
          },
          visualMap: {
            min: 0,
            max,
            orient: "horizontal",
            left: "center",
            bottom: 0,
            itemWidth: 10,
            itemHeight: 140,
            calculable: false,
            text: [value(max, "count") + "+", "0"],
            textGap: 8,
            inRange: { color: calendarColors },
            outOfRange: { color: calendarColors[calendarColors.length - 1] },
            textStyle: { color: "#727784", fontSize: 11 },
          },
          series: [
            {
              type: "heatmap",
              // Counts on the darkest cells are drawn in white to stay readable.
              data: matrix.cells.map(([column, row, count]) => ({
                value: [column, row, count],
                label: { color: count >= max * 0.6 ? "#fff" : "#191c24" },
              })),
              label: {
                // Phone-width cells are too small for numbers; the tooltip shows them on tap.
                show: window.innerWidth >= 600,
                fontSize: 9,
                color: "#191c24",
                formatter: ({ value: [, , count] }: { value: [number, number, number] }) => (count > 0 ? axis(count, "count") : ""),
              },
              itemStyle: { borderColor: "#fff", borderWidth: 1, borderRadius: 2 },
              emphasis: { itemStyle: { borderColor: "#191c24", borderWidth: 1 } },
            },
          ],
        },
      };
    }
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
        ...(item.color ? { itemStyle: { color: item.color }, lineStyle: { color: item.color } } : {}),
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
    if (view.kind === "months") {
      if (!months?.most || !months.least) return undefined;
      return t("monthsExtremes", {
        most: monthNames[months.most.month - 1],
        mostValue: decimal.format(months.most.value),
        least: monthNames[months.least.month - 1],
        leastValue: decimal.format(months.least.value),
      });
    }
    if (view.kind === "matrix") {
      if (!matrix?.topMixed) return undefined;
      return t("topMixedPair", {
        rows: t(view.rowsLabel),
        row: country(matrix.topMixed.row),
        columns: t(view.columnsLabel),
        column: country(matrix.topMixed.column),
        value: value(matrix.topMixed.value, "count"),
      });
    }
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
        ranking.total !== null && t(view.totalLabel ?? "totalValue", { value: value(ranking.total, view.unit) }),
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
      {ranking && ranking.years.length > 1 && (
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
      {span && (
        <select
          className="chart-select"
          aria-label={t("year")}
          value={span.year}
          onChange={(event) => setYear(event.target.value)}
        >
          <option value={ALL_YEARS}>{span.allLabel}</option>
          {span.years.map((option) => (
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

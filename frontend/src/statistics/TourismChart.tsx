import { useMemo, useState } from "react";
import { SegmentedControl } from "../components/SegmentedControl";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { useSeriesTable, type SeriesTable } from "./api";
import { ChartCard } from "./ChartCard";
import { axisStyle, chartState, tooltipStyle } from "./chartState";
import {
  countryName,
  seasonComplete,
  seasonPeriods,
  seasons,
  totals,
  yearsOf,
  type Season,
} from "./series";

const TOP_COUNTRIES = 10;
const TOTAL = "total";

const seasonLabels: Record<Season, MessageKey> = {
  year: "wholeYear",
  winter: "winter",
  spring: "spring",
  summer: "summer",
  autumn: "autumn",
};

/** Latest year with all twelve months published, or the latest year when none is complete. */
function defaultYear(table: SeriesTable) {
  const years = yearsOf(table);
  return years.find((year) => seasonComplete(table, year, "year")) ?? years[0];
}

export function TourismChart() {
  const { t, language } = useI18n();
  const query = useSeriesTable("tourist-arrivals");
  const [chosenYear, setYear] = useState<string | null>(null);
  const [season, setSeason] = useState<Season>("year");
  const format = useMemo(() => new Intl.NumberFormat(language), [language]);
  const year = chosenYear ?? (query.data ? defaultYear(query.data) : "");

  const selection = useMemo(() => {
    if (!query.data || !year) return null;
    const indexes = seasonPeriods(query.data, year, season);
    const all = totals(query.data, indexes);
    const countries = all
      .filter((item) => item.key !== TOTAL)
      .sort((a, b) => b.value - a.value)
      .slice(0, TOP_COUNTRIES);
    return {
      countries,
      total: all.find((item) => item.key === TOTAL)?.value ?? null,
      complete: seasonComplete(query.data, year, season),
    };
  }, [query.data, year, season]);

  const state = chartState([query], () => {
    if (!selection || selection.countries.length === 0) return { status: "empty" };
    const rows = [...selection.countries].reverse();
    return {
      status: "ready",
      height: 64 + rows.length * 30,
      label: t("tourismChartLabel"),
      option: {
        grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
        tooltip: {
          ...tooltipStyle,
          axisPointer: { type: "shadow" },
          valueFormatter: (value: number) => format.format(value),
        },
        xAxis: { type: "value", ...axisStyle, axisLabel: { show: false } },
        yAxis: {
          type: "category",
          data: rows.map(
            (item) => countryName(item.key, language) + (item.lowReliability ? " *" : ""),
          ),
          ...axisStyle,
          axisLabel: { ...axisStyle.axisLabel, color: "#191c24", fontSize: 12 },
        },
        series: [
          {
            type: "bar",
            name: t("tourism"),
            data: rows.map((item) => item.value),
            barMaxWidth: 18,
            itemStyle: { borderRadius: [0, 4, 4, 0] },
            label: {
              show: true,
              position: "right",
              color: "#727784",
              fontSize: 11,
              formatter: ({ value }: { value: number }) => format.format(value),
            },
          },
        ],
      },
    };
  });

  const notes = selection
    ? [
        selection.total !== null &&
          t("tourismSummary", { count: format.format(selection.total) }),
        !selection.complete && t("partialPeriod"),
        selection.countries.some((item) => item.lowReliability) && t("lowReliability"),
      ].filter(Boolean)
    : [];

  return (
    <ChartCard
      title={t("tourism")}
      description={t("tourismDescription")}
      illustration="travel"
      state={state}
      source={query.data?.source}
      controls={
        query.data && (
          <>
            <select
              className="chart-select"
              aria-label={t("year")}
              value={year}
              onChange={(event) => setYear(event.target.value)}
            >
              {yearsOf(query.data).map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <SegmentedControl
              label={t("season")}
              value={season}
              onChange={setSeason}
              options={seasons.map((option) => ({ value: option, label: t(seasonLabels[option]) }))}
            />
          </>
        )
      }
      summary={notes.length > 0 ? notes.join(" · ") : undefined}
    />
  );
}

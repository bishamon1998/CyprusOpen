import { type SeriesTable } from "./api";

export type Season = "year" | "winter" | "spring" | "summer" | "autumn";
export const seasons: Season[] = ["year", "winter", "spring", "summer", "autumn"];

/** Calendar-year seasons: winter is January, February and December of the same year. */
const seasonMonths: Record<Season, number[]> = {
  year: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
  winter: [1, 2, 12],
  spring: [3, 4, 5],
  summer: [6, 7, 8],
  autumn: [9, 10, 11],
};

export function line(table: SeriesTable, key: string) {
  return table.series.find((series) => series.key === key);
}

/** Values of one series by period, for annual tables. */
export function valuesByPeriod(table: SeriesTable, key: string) {
  const series = line(table, key);
  return table.periods.map((_, index) => series?.values[index] ?? null);
}

/** Whether periods are months (`2024-07`) rather than years (`2024`). */
export function isMonthly(table: SeriesTable) {
  return table.periods.some((period) => period.length > 4);
}

/** Years that appear in a table, newest first. */
export function yearsOf(table: SeriesTable) {
  return [...new Set(table.periods.map((period) => period.slice(0, 4)))]
    .sort()
    .reverse();
}

/** Indexes of the periods in a year (and, for monthly tables, a season). */
export function seasonPeriods(table: SeriesTable, year: string, season: Season) {
  const months = seasonMonths[season];
  return table.periods.flatMap((period, index) =>
    period.slice(0, 4) === year &&
    (period.length === 4 || months.includes(Number(period.slice(5))))
      ? [index]
      : [],
  );
}

/** Whether the whole year or season is published; annual tables are always complete. */
export function seasonComplete(table: SeriesTable, year: string, season: Season) {
  const indexes = seasonPeriods(table, year, season);
  return (
    indexes.length > 0 &&
    (!isMonthly(table) || indexes.length === seasonMonths[season].length)
  );
}

/** Latest year with complete data, or the latest year when none is complete. */
export function defaultYear(table: SeriesTable) {
  const years = yearsOf(table);
  return years.find((year) => seasonComplete(table, year, "year")) ?? years[0];
}

export type Total = { key: string; value: number; lowReliability: boolean };

/** Sums each series over the given periods, skipping series without any value there. */
export function totals(table: SeriesTable, indexes: number[]): Total[] {
  return table.series.flatMap((series) => {
    const present = indexes.filter((index) => series.values[index] !== null);
    if (present.length === 0) return [];
    return [
      {
        key: series.key,
        value: present.reduce((sum, index) => sum + (series.values[index] ?? 0), 0),
        lowReliability: present.some((index) => series.lowReliability.includes(index)),
      },
    ];
  });
}

const regionNames = new Map<string, Intl.DisplayNames>();

/**
 * Localized country name for an ISO 3166-1 alpha-2 code. A key that is not a region code ("non-eu", "total")
 * is shown as it is: `Intl.DisplayNames` throws for it, which would break the whole card.
 */
export function countryName(code: string, language: string) {
  let names = regionNames.get(language);
  if (!names) {
    names = new Intl.DisplayNames([language], { type: "region" });
    regionNames.set(language, names);
  }
  try {
    return names.of(code) ?? code;
  } catch {
    return code;
  }
}

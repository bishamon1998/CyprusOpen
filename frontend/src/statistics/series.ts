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

/** Years that appear in a monthly table, newest first. */
export function yearsOf(table: SeriesTable) {
  return [...new Set(table.periods.map((period) => period.slice(0, 4)))]
    .sort()
    .reverse();
}

/** Indexes of the monthly periods that fall in a year and season. */
export function seasonPeriods(table: SeriesTable, year: string, season: Season) {
  const months = seasonMonths[season];
  return table.periods.flatMap((period, index) =>
    period.startsWith(year + "-") && months.includes(Number(period.slice(5)))
      ? [index]
      : [],
  );
}

/** Whether every month of the season is published for that year. */
export function seasonComplete(table: SeriesTable, year: string, season: Season) {
  return seasonPeriods(table, year, season).length === seasonMonths[season].length;
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

/** Localized country name for an ISO 3166-1 alpha-2 code. */
export function countryName(code: string, language: string) {
  let names = regionNames.get(language);
  if (!names) {
    names = new Intl.DisplayNames([language], { type: "region" });
    regionNames.set(language, names);
  }
  return names.of(code) ?? code;
}

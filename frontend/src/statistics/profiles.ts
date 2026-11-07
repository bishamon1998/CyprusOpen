import { type SeriesTable } from "./api";
import { ALL_YEARS } from "./calendar";
import { line, yearsOf } from "./series";

/** How many of the latest years the "average" of a month profile covers. */
export const PROFILE_YEARS = 10;

export type MonthValue = { month: number; value: number };

/** Days in a month; 2024-02 has 29. */
const daysIn = (year: string, month: number) => new Date(Number(year), month, 0).getDate();

/**
 * Average per day in each month, from a table whose series are months ("01"–"12") and whose periods are
 * years. Per-day values make February comparable with 31-day months. {@link ALL_YEARS} averages the
 * latest {@link PROFILE_YEARS} years that have the month.
 */
export function monthProfile(table: SeriesTable, year: string): MonthValue[] {
  const years = year === ALL_YEARS ? yearsOf(table).slice(0, PROFILE_YEARS) : [year];
  return Array.from({ length: 12 }, (_, index) => index + 1).flatMap((month) => {
    const series = line(table, String(month).padStart(2, "0"));
    const perDay = years.flatMap((y) => {
      const amount = series?.values[table.periods.indexOf(y)];
      return amount === null || amount === undefined ? [] : [amount / daysIn(y, month)];
    });
    if (perDay.length === 0) return [];
    return [{ month, value: perDay.reduce((sum, amount) => sum + amount, 0) / perDay.length }];
  });
}

export type PairMatrix = {
  rows: string[];
  columns: string[];
  /** [column index, row index, count] */
  cells: [number, number, number][];
  /** The largest pair whose two sides differ. */
  topMixed: { row: string; column: string; value: number } | null;
};

/**
 * Counts by pair from series keyed "row|column" (groom|bride), for one year or summed over all years.
 * Rows and columns are ordered by their totals, with "other" last.
 */
export function pairMatrix(table: SeriesTable, year: string): PairMatrix {
  const index = table.periods.indexOf(year);
  const pairs = table.series.flatMap((series) => {
    const [row, column] = series.key.split("|");
    if (!row || !column) return [];
    const values = year === ALL_YEARS ? series.values : [series.values[index] ?? null];
    const value = values.reduce<number>((sum, amount) => sum + (amount ?? 0), 0);
    return [{ row, column, value }];
  });
  const order = (side: "row" | "column") => {
    const totals = new Map<string, number>();
    for (const pair of pairs) totals.set(pair[side], (totals.get(pair[side]) ?? 0) + pair.value);
    return [...totals]
      .sort(([a, x], [b, y]) => Number(a === "other") - Number(b === "other") || y - x)
      .map(([key]) => key);
  };
  const rows = order("row");
  const columns = order("column");
  let topMixed: PairMatrix["topMixed"] = null;
  for (const pair of pairs) {
    if (pair.row !== pair.column && pair.row !== "other" && pair.column !== "other" &&
      (!topMixed || pair.value > topMixed.value)) {
      topMixed = pair;
    }
  }
  return {
    rows,
    columns,
    cells: pairs.map((pair) => [columns.indexOf(pair.column), rows.indexOf(pair.row), pair.value]),
    topMixed,
  };
}

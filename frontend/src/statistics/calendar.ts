import { type SeriesTable } from "./api";

/** Year choice meaning "the average over every published year". */
export const ALL_YEARS = "all";

export type CalendarDay = { month: number; day: number; value: number };

/**
 * Counts by day of the year from a table whose series keys are dates ("08-15") and whose periods are years.
 * For one year the value is that year's count; for {@link ALL_YEARS} it is the mean over the years that have
 * the date, so 29 February is compared fairly with days that occur every year.
 */
export function calendarDays(table: SeriesTable, year: string): CalendarDay[] {
  const index = table.periods.indexOf(year);
  const days = table.series.flatMap((series) => {
    const [month, day] = series.key.split("-").map(Number);
    if (!month || !day) return [];
    const values = (year === ALL_YEARS ? series.values : [series.values[index] ?? null]).filter(
      (amount): amount is number => amount !== null,
    );
    if (values.length === 0) return [];
    return [{ month, day, value: values.reduce((sum, amount) => sum + amount, 0) / values.length }];
  });
  return days.sort((a, b) => a.month - b.month || a.day - b.day);
}

/** The busiest and the quietest day; ties keep the earlier date. */
export function extremes(days: CalendarDay[]) {
  if (days.length === 0) return null;
  let most = days[0];
  let least = days[0];
  for (const day of days) {
    if (day.value > most.value) most = day;
    if (day.value < least.value) least = day;
  }
  return { most, least };
}

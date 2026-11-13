import { type MessageKey } from "../locales";

/** A calendar date without a time zone: the day itself is the same everywhere. */
export type Day = { year: number; month: number; day: number };

export type Holiday = { name: MessageKey; date: Day };

/**
 * Orthodox Easter Sunday in the Gregorian calendar (Meeus's Julian algorithm plus the 13-day calendar difference,
 * valid 1900–2099).
 */
export function orthodoxEaster(year: number): Day {
  const a = year % 4;
  const b = year % 7;
  const c = year % 19;
  const d = (19 * c + 15) % 30;
  const e = (2 * a + 4 * b - d + 34) % 7;
  const month = Math.floor((d + e + 114) / 31);
  const day = ((d + e + 114) % 31) + 1;
  return shift({ year, month, day }, 13);
}

/** The date `days` after (or before, when negative) another. */
export function shift(date: Day, days: number): Day {
  const moved = new Date(Date.UTC(date.year, date.month - 1, date.day + days));
  return { year: moved.getUTCFullYear(), month: moved.getUTCMonth() + 1, day: moved.getUTCDate() };
}

/** 0 = Sunday … 6 = Saturday. */
export function weekday(date: Day) {
  return new Date(Date.UTC(date.year, date.month - 1, date.day)).getUTCDay();
}

export function daysBetween(from: Day, to: Day) {
  return Math.round(
    (Date.UTC(to.year, to.month - 1, to.day) - Date.UTC(from.year, from.month - 1, from.day)) / 86_400_000,
  );
}

/**
 * Bank and public service holidays as the Central Bank of Cyprus lists them each year: fixed dates plus the days
 * that follow Orthodox Easter. In date order.
 */
export function holidays(year: number): Holiday[] {
  const easter = orthodoxEaster(year);
  const fixed = (month: number, day: number): Day => ({ year, month, day });
  const list: Holiday[] = [
    { name: "holidayNewYear", date: fixed(1, 1) },
    { name: "holidayEpiphany", date: fixed(1, 6) },
    { name: "holidayGreenMonday", date: shift(easter, -48) },
    { name: "holidayGreekIndependence", date: fixed(3, 25) },
    { name: "holidayCyprusNational", date: fixed(4, 1) },
    { name: "holidayGoodFriday", date: shift(easter, -2) },
    { name: "holidayEasterMonday", date: shift(easter, 1) },
    { name: "holidayEasterTuesday", date: shift(easter, 2) },
    { name: "holidayLabour", date: fixed(5, 1) },
    { name: "holidayPentecost", date: shift(easter, 50) },
    { name: "holidayAssumption", date: fixed(8, 15) },
    { name: "holidayIndependence", date: fixed(10, 1) },
    { name: "holidayOchi", date: fixed(10, 28) },
    { name: "holidayChristmas", date: fixed(12, 25) },
    { name: "holidayBoxing", date: fixed(12, 26) },
  ];
  // Labour Day can fall in Easter week (2027: Easter on 2 May), so sort rather than trust the order above.
  return list.sort((a, b) => daysBetween(b.date, a.date));
}

/** Today in Cyprus, whatever the visitor's own time zone. */
export function cyprusToday(now = new Date()): Day {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Nicosia",
    year: "numeric",
    month: "numeric",
    day: "numeric",
  }).formatToParts(now);
  const part = (type: string) => Number(parts.find((item) => item.type === type)!.value);
  return { year: part("year"), month: part("month"), day: part("day") };
}

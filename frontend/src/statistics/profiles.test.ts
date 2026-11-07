import { describe, expect, it } from "vitest";
import { ALL_YEARS, calendarDays, extremes } from "./calendar";
import { monthProfile, pairMatrix } from "./profiles";
import { table } from "./testTable";

describe("calendarDays", () => {
  const births = table(["2023", "2024"], {
    "01-01": [20, 30],
    "02-29": [null, 14],
    "12-25": [9, 13],
    total: [1000, 1100],
  });

  it("reads one year or the mean over the years that have the date", () => {
    expect(calendarDays(births, "2023")).toEqual([
      { month: 1, day: 1, value: 20 },
      { month: 12, day: 25, value: 9 },
    ]);
    // 29 February is averaged over the one year it exists, not divided by two.
    expect(calendarDays(births, ALL_YEARS)).toEqual([
      { month: 1, day: 1, value: 25 },
      { month: 2, day: 29, value: 14 },
      { month: 12, day: 25, value: 11 },
    ]);
  });

  it("finds the busiest and quietest day, keeping the earlier date on ties", () => {
    expect(extremes(calendarDays(births, ALL_YEARS))).toEqual({
      most: { month: 1, day: 1, value: 25 },
      least: { month: 12, day: 25, value: 11 },
    });
    expect(extremes([{ month: 3, day: 1, value: 5 }, { month: 4, day: 1, value: 5 }])?.most.month).toBe(3);
    expect(extremes([])).toBeNull();
  });
});

describe("monthProfile", () => {
  it("compares months per day, so February is not penalized for being short", () => {
    const births = table(["2023", "2024"], { "01": [310, 620], "02": [280, 580], total: [5000, 6000] });
    expect(monthProfile(births, "2024")).toEqual([
      { month: 1, value: 20 },
      { month: 2, value: 20 },
    ]);
    expect(monthProfile(births, ALL_YEARS)).toEqual([
      { month: 1, value: 15 },
      { month: 2, value: 15 },
    ]);
  });
});

describe("pairMatrix", () => {
  const pairs = table(["2023", "2024"], {
    "CY|CY": [900, 950],
    "CY|RO": [68, 59],
    "GR|CY": [40, 30],
    "other|other": [500, 400],
    "CY|other": [5, 5],
  });

  it("orders sides by total with other last and finds the largest mixed pair", () => {
    const matrix = pairMatrix(pairs, "2024");
    expect(matrix.rows).toEqual(["CY", "GR", "other"]);
    expect(matrix.columns).toEqual(["CY", "RO", "other"]);
    expect(matrix.cells).toContainEqual([1, 0, 59]);
    expect(matrix.topMixed).toEqual({ row: "CY", column: "RO", value: 59 });
    expect(pairMatrix(pairs, ALL_YEARS).topMixed).toEqual({ row: "CY", column: "RO", value: 127 });
  });
});

import { describe, expect, it } from "vitest";
import { countryName, defaultYear, isMonthly, seasonComplete, seasonPeriods, totals, valuesByPeriod } from "./series";
import { table } from "./testTable";

const months = (year: string, count: number) =>
  Array.from({ length: count }, (_, index) => `${year}-${String(index + 1).padStart(2, "0")}`);

describe("seasons of monthly tables", () => {
  // 2024 complete, 2025 published up to July.
  const arrivals = table([...months("2024", 12), ...months("2025", 7)], {
    total: [...Array(12).fill(100), ...Array(7).fill(200)],
    GB: [...Array(12).fill(40), null, null, null, null, null, null, 90],
  }, { GB: [18] });

  it("finds the periods of a year and of a season", () => {
    expect(isMonthly(arrivals)).toBe(true);
    expect(seasonPeriods(arrivals, "2024", "year")).toHaveLength(12);
    // Winter is January, February and December of the same calendar year.
    expect(seasonPeriods(arrivals, "2024", "winter").map((index) => arrivals.periods[index]))
      .toEqual(["2024-01", "2024-02", "2024-12"]);
    expect(seasonPeriods(arrivals, "2025", "summer")).toEqual([17, 18]);
  });

  it("treats a year or season as complete only when every month is published", () => {
    expect(seasonComplete(arrivals, "2024", "year")).toBe(true);
    expect(seasonComplete(arrivals, "2025", "year")).toBe(false);
    expect(seasonComplete(arrivals, "2025", "spring")).toBe(true);
    expect(seasonComplete(arrivals, "2025", "summer")).toBe(false);
    expect(defaultYear(arrivals)).toBe("2024");
  });

  it("sums series over periods, skipping missing values and keeping low reliability", () => {
    expect(totals(arrivals, seasonPeriods(arrivals, "2025", "year"))).toEqual([
      { key: "total", value: 1400, lowReliability: false },
      { key: "GB", value: 90, lowReliability: true },
    ]);
    expect(totals(arrivals, [12, 13])).toEqual([{ key: "total", value: 400, lowReliability: false }]);
  });
});

describe("annual tables", () => {
  const inflation = table(["2023", "2024", "2025"], { inflation: [3.9, null, 2.1] });

  it("are always complete and read values by period", () => {
    expect(isMonthly(inflation)).toBe(false);
    expect(seasonComplete(inflation, "2024", "year")).toBe(true);
    expect(defaultYear(inflation)).toBe("2025");
    expect(valuesByPeriod(inflation, "inflation")).toEqual([3.9, null, 2.1]);
    expect(valuesByPeriod(inflation, "missing")).toEqual([null, null, null]);
  });
});

describe("countryName", () => {
  it("localizes ISO codes and shows other keys unchanged instead of throwing", () => {
    expect(countryName("GB", "en")).toBe("United Kingdom");
    expect(countryName("DE", "ru")).toBe("Германия");
    expect(countryName("non-eu", "en")).toBe("non-eu");
  });
});

import { type SeriesTable } from "./api";

/** A statistics API table for unit tests: periods plus values (and low-reliability indexes) per series key. */
export function table(
  periods: string[],
  series: Record<string, (number | null)[]>,
  lowReliability: Record<string, number[]> = {},
): SeriesTable {
  return {
    source: {
      code: "test",
      name: "CYSTAT",
      url: "https://www.data.gov.cy/en/dataset/1",
      license: "CC BY 4.0",
      licenseUrl: "https://creativecommons.org/licenses/by/4.0/",
      updatedAt: "2026-10-19T05:00:00Z",
      checkedAt: "2026-10-20T05:00:00Z",
    },
    periods,
    series: Object.entries(series).map(([key, values]) => ({ key, values, lowReliability: lowReliability[key] ?? [] })),
  };
}

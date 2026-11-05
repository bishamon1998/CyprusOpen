import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export type DatasetSource = {
  code: string;
  name: string;
  url: string;
  license: string;
  licenseUrl: string;
  updatedAt: string | null;
  checkedAt: string | null;
};

export type SeriesLine = {
  key: string;
  values: (number | null)[];
  /** Indexes into `periods` that the source marks as low reliability. */
  lowReliability: number[];
};

/** Series aligned on one shared period axis (`2024` or `2024-07`). */
export type SeriesTable = {
  source: DatasetSource;
  periods: string[];
  series: SeriesLine[];
};

/** Official tables change monthly at most, so a loaded table is reused for the session. */
export function useSeriesTable(code: string) {
  return useQuery({
    queryKey: ["statistics", code],
    staleTime: Infinity,
    queryFn: () =>
      get<SeriesTable>(
        "/api/v1/statistics/datasets/" + encodeURIComponent(code),
      ),
  });
}

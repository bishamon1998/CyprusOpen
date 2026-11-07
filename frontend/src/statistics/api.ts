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

/**
 * Official tables change monthly at most, so a loaded table is reused for the session. A table that
 * is still empty (its first import runs right after a deploy) is checked again until it has data.
 */
export function useSeriesTable(code: string) {
  return useQuery({
    queryKey: ["statistics", code],
    staleTime: Infinity,
    refetchInterval: (query) => (query.state.data && query.state.data.series.length === 0 ? 15_000 : false),
    queryFn: () =>
      get<SeriesTable>(
        "/api/v1/statistics/datasets/" + encodeURIComponent(code),
      ),
  });
}

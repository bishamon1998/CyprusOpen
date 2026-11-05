import { type UseQueryResult } from "@tanstack/react-query";
import { type ChartState } from "./ChartCard";
import { type SeriesTable } from "./api";

/**
 * Maps the queries a chart depends on to a card state. `ready` runs only once every
 * table has loaded and contains data.
 */
export function chartState(
  queries: UseQueryResult<SeriesTable>[],
  ready: (tables: SeriesTable[]) => ChartState,
): ChartState {
  const failed = queries.filter((query) => query.isError);
  if (failed.length > 0) {
    return {
      status: "error",
      retrying: failed.some((query) => query.isFetching),
      retry: () => failed.forEach((query) => void query.refetch()),
    };
  }
  const tables = queries.flatMap((query) => (query.data ? [query.data] : []));
  if (tables.length < queries.length) return { status: "loading" };
  if (tables.some((table) => table.series.length === 0)) return { status: "empty" };
  return ready(tables);
}

/** Axis and tooltip styling shared by the statistics charts. */
export const axisStyle = {
  axisLine: { lineStyle: { color: "#e8eaf0" } },
  axisTick: { show: false },
  axisLabel: { color: "#727784", fontSize: 11 },
  splitLine: { lineStyle: { color: "#f0f1f5" } },
};

export const tooltipStyle = {
  trigger: "axis",
  confine: true,
  borderColor: "#e8eaf0",
  textStyle: { fontSize: 12, color: "#191c24" },
};

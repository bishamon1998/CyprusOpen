import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export type Dataset = {
  code: string;
  title: string;
  breakdown: string;
  availability: "not_imported";
  lastUpdatedAt: string | null;
  discoveryUrl: string;
};

export function useDatasets() {
  return useQuery({
    queryKey: ["datasets"],
    queryFn: () => get<Dataset[]>("/api/v1/statistics/datasets"),
  });
}

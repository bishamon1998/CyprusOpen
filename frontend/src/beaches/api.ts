import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export type Beach = {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  profileUrl: string | null;
  /** "excellent", "good", "sufficient", "good-or-sufficient", "poor", "not-classified", "missing-data" */
  quality: string | null;
  season: number | null;
  history: { season: number; quality: string }[];
  samples: number | null;
  /** 95th percentiles of the latest season, cfu/100 ml. */
  escherichiaColi: number | null;
  intestinalEnterococci: number | null;
};

export type Beaches = {
  source: { name: string; url: string; license: string; licenseUrl: string; retrievedAt: string | null };
  season: number | null;
  beaches: Beach[];
};

/** Classifications change once a year; one load per visit is enough. */
export function useBeaches() {
  return useQuery({
    queryKey: ["beaches"],
    staleTime: Infinity,
    queryFn: () => get<Beaches>("/api/v1/beaches"),
  });
}

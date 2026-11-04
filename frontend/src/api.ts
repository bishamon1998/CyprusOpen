import { useQuery } from "@tanstack/react-query";
import { get } from "./api/http";

export type Dataset = {
  code: string;
  title: string;
  breakdown: string;
  availability: "not_imported";
  lastUpdatedAt: string | null;
  discoveryUrl: string;
};

export type ParkingPlace = {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  availableSpaces: number | null;
};

export type ParkingReport = {
  freshness: "unavailable";
  lastUpdatedAt: string | null;
  directory: {
    name: string;
    url: string;
    license: string;
    licenseUrl: string;
    publishedAt: string | null;
    retrievedAt: string | null;
    checkedAt: string | null;
  };
  items: ParkingPlace[];
};

export function useDatasets() {
  return useQuery({
    queryKey: ["datasets"],
    queryFn: () => get<Dataset[]>("/api/v1/statistics/datasets"),
  });
}

export function useParking() {
  return useQuery({
    queryKey: ["parking"],
    refetchInterval: (query) =>
      query.state.data?.directory.publishedAt ? 300_000 : 10_000,
    queryFn: async () => {
      const report = await get<ParkingReport>("/api/v1/parking");
      if (!report.directory || !Array.isArray(report.items)) {
        throw new Error("Parking directory is not available.");
      }
      return report;
    },
  });
}

export const catalogUrl =
  "https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset";

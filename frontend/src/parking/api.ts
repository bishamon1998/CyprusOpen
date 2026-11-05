import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

/** Name the parking service uses for facilities without a usable English name. */
export const UNNAMED_PARKING = "Car park";

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

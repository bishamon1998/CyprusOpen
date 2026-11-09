import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

/** European Air Quality Index levels, best first. */
export const levels = ["good", "fair", "moderate", "poor", "very-poor", "extremely-poor"] as const;
export type Level = (typeof levels)[number];

export type Reading = { pollutant: string; value: number; level: Level | null };

export type Station = {
  id: string;
  name: { en: string; el: string };
  type: string;
  latitude: number;
  longitude: number;
  measuredAt: string;
  level: Level | null;
  readings: Reading[];
};

export type AirQuality = {
  source: {
    name: string;
    url: string;
    license: string;
    licenseUrl: string;
    index: string;
    indexUrl: string;
    /** The newest station reading. */
    measuredAt: string | null;
    retrievedAt: string | null;
  };
  stations: Station[];
};

// The source publishes hourly; recheck often enough to show a new hour soon after it is imported.
const REFRESH = 10 * 60_000;

export function useAirQuality() {
  return useQuery({
    queryKey: ["air-quality"],
    staleTime: REFRESH,
    refetchInterval: REFRESH,
    queryFn: () => get<AirQuality>("/api/v1/air-quality"),
  });
}

export type Hour = { measuredAt: string; level: Level | null; readings: Reading[] };
export type StationHistory = { stationId: string; hours: Hour[] };

/** A station's hourly readings of the last week, oldest first; collected by the site, so it may start later. */
export function useStationHistory(id: string | undefined) {
  return useQuery({
    queryKey: ["air-quality", "history", id],
    enabled: id !== undefined,
    staleTime: REFRESH,
    refetchInterval: REFRESH,
    queryFn: () => get<StationHistory>(`/api/v1/air-quality/stations/${encodeURIComponent(id!)}/history`),
  });
}

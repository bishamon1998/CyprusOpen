import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";
import { type Position } from "../geo/coordinates";

export type Pharmacy = {
  id: string;
  district: string;
  registrationNumber: string;
  name: string;
  address: string;
  municipality: string;
  phone: string;
  location: (Position & { source: string }) | null;
};

export type PharmacySchedule = {
  date: string;
  source: string;
  checkedAt: string | null;
  onCall: boolean;
  items: Pharmacy[];
};

/** Loads a chosen date, or the current roster (refreshed every 30 seconds) when no date is set. */
export function usePharmacySchedule(date: string) {
  return useQuery({
    queryKey: ["pharmacies", date],
    refetchInterval: date ? false : 30_000,
    refetchOnWindowFocus: !date,
    queryFn: () =>
      get<PharmacySchedule>(
        "/api/v1/pharmacies" +
          (date ? "?" + new URLSearchParams({ date }) : ""),
      ),
  });
}

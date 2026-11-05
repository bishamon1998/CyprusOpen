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
  location:
    | (Position & { precision: "exact" | "street" | "locality"; source: string })
    | null;
};

export type PharmacySchedule = {
  date: string;
  source: string;
  checkedAt: string | null;
  onCall: boolean;
  items: Pharmacy[];
};

// The current roster changes only at 08:00 and when overnight on-call service starts.
const CURRENT_ROSTER_REFRESH = 5 * 60_000;

/** Loads a chosen date once, or the current roster, rechecked every five minutes and on return to the tab. */
export function usePharmacySchedule(date: string) {
  return useQuery({
    queryKey: ["pharmacies", date],
    staleTime: date ? Infinity : CURRENT_ROSTER_REFRESH,
    refetchInterval: date ? false : CURRENT_ROSTER_REFRESH,
    refetchOnWindowFocus: !date,
    queryFn: () =>
      get<PharmacySchedule>(
        "/api/v1/pharmacies" +
          (date ? "?" + new URLSearchParams({ date }) : ""),
      ),
  });
}

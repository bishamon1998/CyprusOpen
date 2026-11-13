import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export const kinds = ["hospital", "centre"] as const;
export type Kind = (typeof kinds)[number];

export type ServicePlace = {
  id: string;
  kind: Kind;
  /** Greek, as published: the files have no other language. */
  name: string;
  address: string | null;
  phone: string | null;
  /** Read from the published text; null when it could not be read (and for hospitals). */
  hours: { days: number[]; opens: string; closes: string } | null;
  hoursText: string | null;
  appointmentUrl: string | null;
  latitude: number;
  longitude: number;
};

export type ServiceSource = {
  name: string;
  url: string;
  license: string;
  licenseUrl: string;
  /** When the source last changed its file. */
  publishedAt: string | null;
  retrievedAt: string | null;
};

export type Services = {
  sources: { hospitals: ServiceSource; centres: ServiceSource };
  places: ServicePlace[];
};

/** The hospital and service centre lists change a few times a year; one load per visit is enough. */
export function useServices() {
  return useQuery({
    queryKey: ["services"],
    staleTime: Infinity,
    queryFn: async () => {
      const services = await get<Services>("/api/v1/services");
      if (!services.sources?.hospitals || !Array.isArray(services.places)) {
        throw new Error("Public services are not available.");
      }
      return services;
    },
  });
}

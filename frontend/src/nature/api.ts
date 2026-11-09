import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export const kinds = ["picnic", "camping", "trail"] as const;
export type Kind = (typeof kinds)[number];

export type NatureSite = {
  id: string;
  kind: Kind;
  /** Name in Latin letters, as published. */
  name: string;
  greekName: string | null;
  /** Site position; for a trail, the start of its first line. */
  latitude: number;
  longitude: number;
  lengthKm: number | null;
  /** Maximum number of people (camping sites), when published. */
  capacity: number | null;
  /** Trail lines as [latitude, longitude] pairs; empty for other kinds. */
  route: [number, number][][];
};

export type Nature = {
  source: { name: string; url: string; license: string; licenseUrl: string; retrievedAt: string | null };
  sites: NatureSite[];
};

/** The Department of Forests files change rarely; one load per visit is enough. */
export function useNature() {
  return useQuery({
    queryKey: ["nature"],
    staleTime: Infinity,
    queryFn: async () => {
      const nature = await get<Nature>("/api/v1/nature");
      if (!nature.source || !Array.isArray(nature.sites)) throw new Error("Nature sites are not available.");
      return nature;
    },
  });
}

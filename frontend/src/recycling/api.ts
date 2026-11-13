import { useQuery } from "@tanstack/react-query";
import { get } from "../api/http";

export const kinds = ["island", "glass"] as const;
export type Kind = (typeof kinds)[number];

export type RecyclingPoint = {
  id: string;
  /** "island": packaging, paper and glass bins together; "glass": a glass bank. */
  kind: Kind;
  /** Green Dot's own label ("K107"); not unique. */
  code: string | null;
  /** Where the point is, in Greek as published ("Πάρκο"). */
  description: string | null;
  latitude: number;
  longitude: number;
};

export type Recycling = {
  source: {
    name: string;
    url: string;
    license: string;
    licenseUrl: string;
    /** When Green Dot exported the points. */
    publishedAt: string | null;
    retrievedAt: string | null;
  };
  points: RecyclingPoint[];
};

/** The Green Dot file has not changed since 2019; one load per visit is enough. */
export function useRecycling() {
  return useQuery({
    queryKey: ["recycling"],
    staleTime: Infinity,
    queryFn: async () => {
      const recycling = await get<Recycling>("/api/v1/recycling");
      if (!recycling.source || !Array.isArray(recycling.points)) throw new Error("Recycling points are not available.");
      return recycling;
    },
  });
}

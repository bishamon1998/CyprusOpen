import { useQuery } from "@tanstack/react-query";
import { get } from "../../api/http";

export function useMapConfig() {
  return useQuery({
    queryKey: ["map-config"],
    queryFn: () =>
      get<{ tileUrl: string; attribution: string; attributionUrl: string }>(
        "/map-config.json",
      ),
    staleTime: Infinity,
  });
}

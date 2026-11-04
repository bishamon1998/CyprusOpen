import { latLng } from "leaflet";

export type Bounds = [[number, number], [number, number]];
export type Position = { latitude: number; longitude: number };

export function distance(from: Position, to: Position) {
  return (
    latLng(from.latitude, from.longitude).distanceTo(
      latLng(to.latitude, to.longitude),
    ) / 1000
  );
}

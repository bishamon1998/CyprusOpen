import { type MessageKey } from "../locales";
import { type Bounds, distance, type Position } from "./coordinates";

export type Area = {
  id: string;
  name: MessageKey;
  /** The town alone, for labelling a place in a list; missing for the whole island. */
  town?: MessageKey;
  bounds: Bounds;
};

export const areas: Area[] = [
  {
    id: "cyprus",
    name: "areaCyprus",
    bounds: [
      [34.5, 32.25],
      [35.75, 34.65],
    ],
  },
  {
    id: "nicosia",
    name: "areaNicosia",
    town: "townNicosia",
    bounds: [
      [35.04, 33.21],
      [35.24, 33.49],
    ],
  },
  {
    id: "limassol",
    name: "areaLimassol",
    town: "townLimassol",
    bounds: [
      [34.61, 32.9],
      [34.78, 33.2],
    ],
  },
  {
    id: "larnaca",
    name: "areaLarnaca",
    town: "townLarnaca",
    bounds: [
      [34.84, 33.51],
      [35.01, 33.72],
    ],
  },
  {
    id: "paphos",
    name: "areaPaphos",
    town: "townPaphos",
    bounds: [
      [34.7, 32.35],
      [34.87, 32.52],
    ],
  },
  {
    id: "ayia-napa",
    name: "areaAyiaNapa",
    town: "townAyiaNapa",
    bounds: [
      [34.95, 33.9],
      [35.09, 34.1],
    ],
  },
];

const centre = ({ bounds: [[south, west], [north, east]] }: Area): Position => ({
  latitude: (south + north) / 2,
  longitude: (west + east) / 2,
});

/** The town a place is in, or else the nearest one (`inside` false), to label it without coordinates. */
export function townOf(place: Position): { town: MessageKey; inside: boolean } {
  const towns = areas.filter((area) => area.town);
  const inside = towns.find(({ bounds: [[south, west], [north, east]] }) =>
    place.latitude >= south && place.latitude <= north && place.longitude >= west && place.longitude <= east);
  const area = inside ?? towns.reduce((nearest, area) =>
    distance(place, centre(area)) < distance(place, centre(nearest)) ? area : nearest);
  return { town: area.town!, inside: inside !== undefined };
}

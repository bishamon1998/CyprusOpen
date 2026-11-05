import { type MessageKey } from "../locales";
import { type Bounds } from "./coordinates";

export type Area = {
  id: string;
  name: MessageKey;
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
    bounds: [
      [35.04, 33.21],
      [35.24, 33.49],
    ],
  },
  {
    id: "limassol",
    name: "areaLimassol",
    bounds: [
      [34.61, 32.9],
      [34.78, 33.2],
    ],
  },
  {
    id: "larnaca",
    name: "areaLarnaca",
    bounds: [
      [34.84, 33.51],
      [35.01, 33.72],
    ],
  },
  {
    id: "paphos",
    name: "areaPaphos",
    bounds: [
      [34.7, 32.35],
      [34.87, 32.52],
    ],
  },
  {
    id: "ayia-napa",
    name: "areaAyiaNapa",
    bounds: [
      [34.95, 33.9],
      [35.09, 34.1],
    ],
  },
];

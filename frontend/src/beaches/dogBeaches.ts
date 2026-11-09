/**
 * Beaches where dogs may swim, summer 2026: the seven designated beaches reported in July 2026, with Potamia
 * named by the Municipality of Paralimni-Deryneia. No open dataset lists them, so this list is kept by hand
 * (source: `dogBeachesUrl`). Points are the "Dog's Beach" places of OpenStreetMap; Pitili has none, so it is
 * listed without a map point.
 */
export type DogBeach = {
  id: string;
  name: string;
  place: { en: string; ru: string; el: string };
  latitude: number | null;
  longitude: number | null;
  /** Designated by the municipality rather than on the national list. */
  municipal?: boolean;
};

export const dogBeachSeason = 2026;

export const dogBeaches: DogBeach[] = [
  {
    id: "steratzia",
    name: "Steratzia",
    place: { en: "Kato Pyrgos, Tylliria", ru: "Като Пиргос, Тиллирия", el: "Κάτω Πύργος Τηλλυρίας" },
    latitude: 35.1852791,
    longitude: 32.684882,
  },
  {
    id: "kassianes",
    name: "Kassianes (Spyros Beach)",
    place: { en: "Larnaca, south of the airport", ru: "Ларнака, южнее аэропорта", el: "Λάρνακα, νότια του αεροδρομίου" },
    latitude: 34.8725502,
    longitude: 33.6373496,
  },
  {
    id: "acheleia",
    name: "Acheleia",
    place: { en: "Acheleia, Paphos, by the Ezousa river mouth", ru: "Ахелия, Пафос, у устья Эзусы", el: "Αχέλεια, Πάφος, στις εκβολές του Έζουσα" },
    latitude: 34.7255876,
    longitude: 32.4646021,
  },
  {
    id: "pitili",
    name: "Pitili",
    place: { en: "Neo Chorio, Paphos, east of the Platani river mouth", ru: "Нео Хорио, Пафос, восточнее устья Платани", el: "Νέο Χωριό, Πάφος, ανατολικά των εκβολών του Πλατανιού" },
    latitude: null,
    longitude: null,
  },
  {
    id: "karavopetra",
    name: "Karavopetra",
    place: { en: "Monagroulli, Limassol", ru: "Монагрулли, Лимасол", el: "Μοναγρούλλι, Λεμεσός" },
    latitude: 34.706525,
    longitude: 33.2198115,
  },
  {
    id: "prolimnos",
    name: "Prolimnos",
    place: { en: "West of Pissouri, Limassol", ru: "Западнее Писсури, Лимасол", el: "Δυτικά του Πισσουρίου, Λεμεσός" },
    latitude: 34.6576718,
    longitude: 32.6485784,
  },
  {
    id: "potamia",
    name: "Potamia",
    place: { en: "Kapparis, Paralimni", ru: "Каппарис, Паралимни", el: "Κάππαρης, Παραλίμνι" },
    latitude: 35.0621793,
    longitude: 34.0066218,
    municipal: true,
  },
];

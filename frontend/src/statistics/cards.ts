import { type MessageKey } from "../locales";
import { type StatisticCardConfig } from "./StatisticCard";

/**
 * Every chart on the Statistics page. A view reads one dataset served by statistics-service
 * (`/api/v1/statistics/datasets/{code}`); series keys match the backend. Cards are placed on the page
 * through `categories` below.
 */
const cards: StatisticCardConfig[] = [
  {
    id: "calendar",
    title: "calendar",
    description: "calendarDescription",
    illustration: "calendar",
    views: [
      { kind: "calendar", label: "birthdays", dataset: "birth-dates", measure: "birthsPerDay" },
      { kind: "calendar", label: "weddings", dataset: "wedding-dates", measure: "weddingsPerDay" },
    ],
  },
  {
    id: "immigration",
    title: "immigration",
    description: "immigrationDescription",
    illustration: "people",
    views: [
      {
        kind: "stacked",
        label: "byCitizenship",
        dataset: "immigrant-groups",
        unit: "count",
        series: [
          { key: "citizenship-cyprus", label: "groupCyprus" },
          { key: "citizenship-eu", label: "groupEu" },
          { key: "citizenship-non-eu", label: "groupNonEu" },
        ],
      },
      {
        kind: "stacked",
        label: "byPreviousResidence",
        dataset: "immigrant-groups",
        unit: "count",
        series: [
          { key: "previous-residence-eu", label: "groupEu" },
          { key: "previous-residence-non-eu", label: "groupNonEu" },
        ],
      },
      {
        kind: "line",
        label: "since1981",
        dataset: "migration-movements",
        unit: "count",
        series: [
          { key: "immigrants", label: "immigrants" },
          { key: "emigrants", label: "emigrants" },
        ],
      },
    ],
  },
  {
    id: "tourism",
    title: "tourism",
    description: "tourismDescription",
    illustration: "travel",
    views: [
      {
        kind: "ranking",
        label: "tourism",
        dataset: "tourist-arrivals",
        unit: "count",
        labels: "countries",
        top: 10,
        seasons: true,
      },
    ],
  },
  {
    id: "inflation",
    title: "inflation",
    description: "inflationDescription",
    illustration: "prices",
    views: [
      {
        kind: "bar",
        label: "byYear",
        dataset: "inflation",
        unit: "percent",
        series: [{ key: "inflation", label: "inflation" }],
      },
      {
        kind: "ranking",
        label: "byCategory",
        dataset: "price-changes",
        unit: "percent",
        labels: {
          food: "priceFood",
          "alcohol-tobacco": "priceAlcohol",
          clothing: "priceClothing",
          housing: "priceHousing",
          furnishings: "priceFurnishings",
          health: "priceHealth",
          transport: "priceTransport",
          communication: "priceCommunication",
          recreation: "priceRecreation",
          education: "priceEducation",
          restaurants: "priceRestaurants",
          insurance: "priceInsurance",
          "personal-care": "pricePersonalCare",
        },
        top: 13,
      },
    ],
  },
  {
    id: "population",
    title: "population",
    description: "populationDescription",
    illustration: "people",
    views: [
      {
        kind: "line",
        label: "population",
        dataset: "population",
        unit: "thousands",
        series: [{ key: "total", label: "residents" }],
      },
      {
        kind: "stacked",
        label: "populationGrowth",
        dataset: "population-change",
        unit: "count",
        series: [
          { key: "natural-increase", label: "naturalIncrease" },
          { key: "net-migration", label: "netMigration" },
        ],
      },
      {
        kind: "line",
        label: "lifeExpectancy",
        dataset: "life-expectancy",
        unit: "years",
        series: [
          { key: "females", label: "women" },
          { key: "males", label: "men" },
        ],
      },
    ],
  },
  {
    id: "marriages",
    title: "marriages",
    description: "marriagesDescription",
    illustration: "family",
    views: [
      {
        kind: "stacked",
        label: "marriages",
        dataset: "marriages",
        unit: "count",
        series: [
          { key: "religious", label: "religiousMarriages" },
          { key: "civil", label: "civilMarriages" },
        ],
      },
      {
        kind: "stacked",
        label: "civilUnions",
        dataset: "civil-unions",
        unit: "count",
        series: [
          { key: "opposite-sex", label: "oppositeSex" },
          { key: "same-sex-men", label: "sameSexMen" },
          { key: "same-sex-women", label: "sameSexWomen" },
        ],
      },
    ],
  },
  {
    id: "crime",
    title: "crime",
    description: "crimeDescription",
    illustration: "safety",
    views: [
      {
        kind: "ranking",
        label: "byType",
        dataset: "serious-offences",
        unit: "count",
        labels: {
          property: "offenceProperty",
          damage: "offenceDamage",
          person: "offencePerson",
          "public-order": "offencePublicOrder",
          "lawful-authority": "offenceAuthority",
          "public-harm": "offencePublicHarm",
          sexual: "offenceSexual",
          forgery: "offenceForgery",
          other: "other",
        },
      },
      {
        kind: "line",
        label: "byYear",
        dataset: "serious-offences",
        unit: "count",
        series: [{ key: "total", label: "seriousOffences" }],
      },
    ],
  },
  {
    id: "earnings",
    title: "earnings",
    description: "earningsDescription",
    illustration: "earnings",
    views: [
      {
        kind: "line",
        label: "meanAndMedian",
        dataset: "earnings",
        unit: "euro",
        series: [
          { key: "median-total", label: "median" },
          { key: "mean-total", label: "mean" },
        ],
      },
      {
        kind: "line",
        label: "menAndWomen",
        dataset: "earnings",
        unit: "euro",
        series: [
          { key: "median-men", label: "men" },
          { key: "median-women", label: "women" },
        ],
      },
    ],
  },
  {
    id: "fuel",
    title: "fuelSales",
    description: "fuelDescription",
    illustration: "fuel",
    views: [
      {
        kind: "line",
        label: "roadFuels",
        dataset: "fuel-sales",
        unit: "tonnes",
        series: [
          { key: "diesel", label: "diesel" },
          { key: "petrol-95", label: "petrol95" },
          { key: "petrol-98", label: "petrol98" },
        ],
      },
      {
        kind: "ranking",
        label: "byFuel",
        dataset: "fuel-sales",
        unit: "tonnes",
        labels: {
          diesel: "diesel",
          "petrol-95": "petrol95",
          "petrol-98": "petrol98",
          "jet-fuel": "jetFuel",
          "heating-oil": "heatingOil",
          "marine-gasoil": "marineGasoil",
          "heavy-fuel-oil": "heavyFuelOil",
          lpg: "lpg",
        },
      },
    ],
  },
];

export type StatisticCategory = { id: string; title: MessageKey; cards: StatisticCardConfig[] };

const card = (id: string) => {
  const found = cards.find((item) => item.id === id);
  if (!found) throw new Error("Unknown statistics card " + id);
  return found;
};

/** Collapsible sections of the Statistics page, in display order. */
export const categories: StatisticCategory[] = [
  { id: "population", title: "categoryPopulation", cards: [card("population"), card("immigration")] },
  { id: "family", title: "categoryFamily", cards: [card("calendar"), card("marriages")] },
  { id: "economy", title: "categoryEconomy", cards: [card("inflation"), card("earnings"), card("fuel")] },
  { id: "tourism", title: "categoryTourism", cards: [card("tourism")] },
  { id: "safety", title: "categorySafety", cards: [card("crime")] },
];

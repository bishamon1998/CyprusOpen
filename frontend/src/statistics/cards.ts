import { type MessageKey } from "../locales";
import { type StatisticCardConfig } from "./StatisticCard";

/**
 * Every chart on the Statistics page. A view reads one dataset served by statistics-service
 * (`/api/v1/statistics/datasets/{code}`); series keys match the backend. Cards are placed on the page
 * through `categories` below.
 */
/** District keys as statistics-service stores them (CYSTAT spelling). */
const DISTRICTS: Record<string, MessageKey> = {
  lefkosia: "districtNicosia",
  ammochostos: "districtFamagusta",
  larnaka: "districtLarnaca",
  lemesos: "districtLimassol",
  pafos: "districtPaphos",
};

const cards: StatisticCardConfig[] = [
  {
    id: "seasons",
    title: "seasonsOfLife",
    description: "seasonsOfLifeDescription",
    illustration: "calendar",
    views: [
      { kind: "months", label: "births", dataset: "births-by-month", measure: "birthsPerDay" },
      { kind: "months", label: "deaths", dataset: "deaths-by-month", measure: "deathsPerDay" },
    ],
  },
  {
    id: "causes",
    title: "causesOfDeath",
    description: "causesOfDeathDescription",
    illustration: "health",
    views: [
      {
        kind: "ranking",
        label: "byCause",
        dataset: "death-causes",
        unit: "count",
        top: 18,
        labels: Object.fromEntries(
          Array.from({ length: 18 }, (_, index) => ["c" + (index + 1), ("cause" + (index + 1)) as MessageKey]),
        ),
      },
    ],
  },
  {
    id: "family-today",
    title: "familyToday",
    description: "familyTodayDescription",
    illustration: "family",
    views: [
      {
        kind: "line",
        label: "weddingAge",
        dataset: "marriage-age",
        unit: "years",
        series: [
          { key: "groom-first", label: "groom" },
          { key: "bride-first", label: "bride" },
        ],
      },
      {
        kind: "line",
        label: "motherAge",
        dataset: "mother-age",
        unit: "years",
        series: [
          { key: "first-child", label: "firstChild" },
          { key: "any-child", label: "anyChild" },
        ],
      },
      {
        kind: "line",
        label: "outsideMarriage",
        dataset: "extramarital-births",
        unit: "permille",
        series: [{ key: "ratio", label: "birthsOutsideMarriage" }],
      },
    ],
  },
  {
    id: "pairs",
    title: "whoMarriesWhom",
    description: "whoMarriesWhomDescription",
    illustration: "family",
    views: [{ kind: "matrix", label: "nationalities", dataset: "marriage-pairs", rowsLabel: "groom", columnsLabel: "bride" }],
  },
  {
    id: "divorces",
    title: "divorces",
    description: "divorcesDescription",
    illustration: "family",
    views: [
      {
        kind: "ranking",
        label: "byYearsMarried",
        dataset: "divorce-duration",
        unit: "count",
        labels: "keys",
        top: 13,
        order: ["<1", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10–14", "15–19", "20+"],
      },
      {
        kind: "bar",
        label: "byYear",
        dataset: "divorce-duration",
        unit: "count",
        series: [{ key: "total", label: "divorces" }],
      },
    ],
  },
  {
    id: "spending",
    title: "householdSpending",
    description: "householdSpendingDescription",
    illustration: "earnings",
    views: [
      {
        kind: "ranking",
        label: "byCategory",
        dataset: "household-spending",
        unit: "per10k",
        top: 13,
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
        totalLabel: "spendingTotal",
      },
    ],
  },
  {
    id: "income",
    title: "householdIncome",
    description: "householdIncomeDescription",
    illustration: "earnings",
    views: [
      {
        kind: "ranking",
        label: "byDistrict",
        dataset: "household-income",
        unit: "euro",
        prefix: "district-",
        labels: DISTRICTS,
        totalLabel: "cyprusAverage",
      },
      {
        kind: "ranking",
        label: "bySource",
        dataset: "household-income",
        unit: "euro",
        prefix: "source-",
        labels: {
          employment: "incomeEmployment",
          "self-employment": "incomeSelfEmployment",
          pensions: "incomePensions",
          rent: "incomeRent",
          investments: "incomeInvestments",
          benefits: "incomeBenefits",
        },
        totalLabel: "cyprusAverage",
      },
    ],
  },
  {
    id: "house-prices",
    title: "housePrices",
    description: "housePricesDescription",
    illustration: "home",
    views: [
      {
        kind: "line",
        label: "priceIndex",
        dataset: "house-prices",
        unit: "index",
        series: [{ key: "index", label: "housePriceIndex" }],
      },
      {
        kind: "bar",
        label: "annualChange",
        dataset: "house-prices",
        unit: "percent",
        series: [{ key: "annual-change", label: "annualChange" }],
      },
    ],
  },
  {
    id: "permits",
    title: "newHomes",
    description: "newHomesDescription",
    illustration: "home",
    views: [
      {
        kind: "line",
        label: "perMonth",
        dataset: "building-permits",
        unit: "count",
        series: [{ key: "total", label: "dwellings" }],
      },
      { kind: "ranking", label: "byDistrict", dataset: "building-permits", unit: "count", labels: DISTRICTS },
    ],
  },
  {
    id: "vehicles",
    title: "carsByFuel",
    description: "carsByFuelDescription",
    illustration: "car",
    views: [
      {
        kind: "stacked",
        label: "newRegistrations",
        dataset: "vehicles",
        unit: "count",
        series: [
          { key: "new-petrol", label: "petrol" },
          { key: "new-diesel", label: "diesel" },
          { key: "new-hybrid", label: "hybrid" },
          { key: "new-electric", label: "electric" },
          { key: "new-other", label: "other" },
        ],
      },
      {
        kind: "stacked",
        label: "onTheRoad",
        dataset: "vehicles",
        unit: "count",
        series: [
          { key: "fleet-petrol", label: "petrol" },
          { key: "fleet-diesel", label: "diesel" },
          { key: "fleet-hybrid", label: "hybrid" },
          { key: "fleet-electric", label: "electric" },
          { key: "fleet-other", label: "other" },
        ],
      },
    ],
  },
  {
    id: "tourist-spending",
    title: "touristSpending",
    description: "touristSpendingDescription",
    illustration: "travel",
    views: [
      {
        kind: "ranking",
        label: "perDay",
        dataset: "tourist-spending",
        unit: "euro",
        labels: "countries",
        prefix: "day-",
        totalLabel: "allTourists",
      },
      {
        kind: "ranking",
        label: "perTrip",
        dataset: "tourist-spending",
        unit: "euro",
        labels: "countries",
        prefix: "trip-",
        totalLabel: "allTourists",
      },
      {
        kind: "ranking",
        label: "nightsStayed",
        dataset: "tourist-spending",
        unit: "nights",
        labels: "countries",
        prefix: "nights-",
        totalLabel: "allTourists",
      },
    ],
  },
  {
    id: "trips",
    title: "residentTrips",
    description: "residentTripsDescription",
    illustration: "travel",
    views: [
      {
        kind: "stacked",
        label: "tripsByYear",
        dataset: "resident-trips",
        unit: "count",
        series: [
          { key: "abroad", label: "tripsAbroad" },
          { key: "domestic", label: "tripsInCyprus" },
        ],
      },
    ],
  },
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
  {
    id: "population",
    title: "categoryPopulation",
    cards: [card("population"), card("immigration"), card("seasons"), card("causes")],
  },
  {
    id: "family",
    title: "categoryFamily",
    cards: [card("calendar"), card("family-today"), card("pairs"), card("marriages"), card("divorces")],
  },
  {
    id: "economy",
    title: "categoryEconomy",
    cards: [card("inflation"), card("earnings"), card("spending"), card("income")],
  },
  { id: "housing", title: "categoryHousing", cards: [card("house-prices"), card("permits")] },
  { id: "transport", title: "categoryTransport", cards: [card("vehicles"), card("fuel")] },
  { id: "tourism", title: "categoryTourism", cards: [card("tourism"), card("tourist-spending"), card("trips")] },
  { id: "safety", title: "categorySafety", cards: [card("crime")] },
];

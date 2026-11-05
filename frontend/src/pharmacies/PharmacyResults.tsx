import { type RefObject } from "react";
import { ArrowUpRight, Navigation, Phone } from "lucide-react";
import {
  NearestFirst,
  ResultRow,
  ResultsMessage,
  ResultsPanel,
  SelectedResult,
} from "../components/tool/Tool";
import { googleDirections } from "../geo/directions";
import { useI18n } from "../i18n";
import { type Pharmacy, usePharmacySchedule } from "./api";

const LOCAL_PHONE = /^[0-9]{8}$/;
/** Greek search prefix (owner-approved) so Google Maps finds the pharmacy listing rather than a street address. */
const PHARMACY_SEARCH_PREFIX = "Φαρμακείο";

/** Address with the municipality appended unless the address already names it. */
export const pharmacyAddress = ({ address, municipality }: Pharmacy) =>
  !municipality || address.includes(municipality) ? address : `${address}, ${municipality}`;

/**
 * Route to the map point when it is exact; otherwise let Google find the pharmacy by name, street and
 * municipality (shop numbers and other notes after the first comma confuse its geocoder).
 */
function pharmacyDirections(pharmacy: Pharmacy) {
  const { location } = pharmacy;
  if (location?.precision === "exact") {
    return googleDirections(`${location.latitude},${location.longitude}`);
  }
  const street = pharmacy.address.split(",")[0].trim();
  return googleDirections(
    [`${PHARMACY_SEARCH_PREFIX} ${pharmacy.name}`, street, pharmacy.municipality, "Κύπρος"]
      .filter(Boolean)
      .join(", "),
  );
}

export function PharmacyResults({
  schedule,
  pharmacies,
  mapped,
  selected,
  sortedByDistance,
  distanceTo,
  results,
  onSelect,
  onClearLocation,
}: {
  schedule: ReturnType<typeof usePharmacySchedule>;
  pharmacies: Pharmacy[];
  mapped: number;
  selected: Pharmacy | undefined;
  sortedByDistance: boolean;
  distanceTo: (pharmacy: Pharmacy) => number | null;
  results: RefObject<HTMLDivElement | null>;
  onSelect: (id: string) => void;
  onClearLocation: () => void;
}) {
  const { t, language } = useI18n();
  const heading = schedule.isPending
    ? t("loading")
    : schedule.data
      ? t("pharmacyCount", { count: pharmacies.length })
      : t("pharmacies");
  const status = pharmacies.length
    ? schedule.data?.onCall
      ? t("pharmacyOnCall")
      : t("dutyHoursUnconfirmed")
    : null;
  const coverage =
    pharmacies.length > mapped
      ? t("pharmacyMapCoverage", { mapped, total: pharmacies.length })
      : null;
  const detail = (pharmacy: Pharmacy) => {
    const distance = distanceTo(pharmacy);
    if (distance !== null) {
      return t("distance", {
        distance: distance.toLocaleString(language, {
          minimumFractionDigits: 1,
          maximumFractionDigits: 1,
        }),
      });
    }
    return pharmacy.location && pharmacy.location.precision !== "exact"
      ? t("approximateLocation")
      : null;
  };
  const phone = selected?.phone.replace(/\D/g, "") ?? "";

  return (
    <ResultsPanel
      panelRef={results}
      heading={heading}
      action={sortedByDistance && <NearestFirst onClear={onClearLocation} />}
      notes={[status, coverage]}
    >
      {schedule.isError && (
        <ResultsMessage onRetry={() => void schedule.refetch()}>
          {t("pharmacyLoadError")}
        </ResultsMessage>
      )}
      {schedule.data && pharmacies.length === 0 && (
        <ResultsMessage>{t("scheduleUnavailable")}</ResultsMessage>
      )}
      {selected && (
        <SelectedResult label={t("selectedPharmacy")} title={selected.name} detail={pharmacyAddress(selected)} lang="el">
          {LOCAL_PHONE.test(phone) && (
            <a href={"tel:+357" + phone}>
              <Phone size={15} aria-hidden="true" />
              {phone}
            </a>
          )}
          <a
            href={pharmacyDirections(selected)}
            target="_blank"
            rel="noopener noreferrer"
          >
            <Navigation size={15} aria-hidden="true" />
            {t("directions")}
            <ArrowUpRight size={14} aria-hidden="true" />
          </a>
        </SelectedResult>
      )}
      <ol className="result-list" aria-label={t("pharmacies")}>
        {pharmacies.map((pharmacy) => (
          <ResultRow
            key={pharmacy.id}
            icon="+"
            title={pharmacy.name}
            lang="el"
            details={[<span lang="el">{pharmacyAddress(pharmacy)}</span>, detail(pharmacy)]}
            selected={selected?.id === pharmacy.id}
            onSelect={() => onSelect(pharmacy.id)}
          />
        ))}
      </ol>
    </ResultsPanel>
  );
}

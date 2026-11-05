import { Navigation, Phone } from "lucide-react";
import { googleDirections } from "../geo/directions";
import { useI18n } from "../i18n";
import { type Pharmacy } from "./api";

const LOCAL_PHONE = /^[0-9]{8}$/;

export const pharmacyElementId = (id: string) => "pharmacy-" + id;

export function PharmacyList({
  pharmacies,
  distanceTo,
}: {
  pharmacies: Pharmacy[];
  distanceTo: (pharmacy: Pharmacy) => number | null;
}) {
  const { t, language } = useI18n();
  return (
    <ul className="pharmacy-list">
      {pharmacies.map((pharmacy) => {
        const address = [pharmacy.address, pharmacy.municipality]
          .filter(Boolean)
          .join(", ");
        const phone = pharmacy.phone.replace(/\D/g, "");
        const distance = distanceTo(pharmacy);
        return (
          <li key={pharmacy.id} id={pharmacyElementId(pharmacy.id)}>
            <h2 lang="el">{pharmacy.name}</h2>
            <p lang="el">{address}</p>
            {pharmacy.location && pharmacy.location.precision !== "exact" && (
              <p>{t("approximateLocation")}</p>
            )}
            {distance !== null && (
              <p>
                {t("distance", {
                  distance: distance.toLocaleString(language, {
                    maximumFractionDigits: 1,
                  }),
                })}
              </p>
            )}
            <div className="pharmacy-actions">
              {LOCAL_PHONE.test(phone) && (
                <a href={"tel:+357" + phone}>
                  <Phone size={16} aria-hidden="true" />
                  {phone}
                </a>
              )}
              <a
                href={googleDirections(address + ", Cyprus")}
                target="_blank"
                rel="noreferrer"
              >
                <Navigation size={16} aria-hidden="true" />
                {t("directions")}
              </a>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

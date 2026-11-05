import { useCallback } from "react";
import { useI18n } from "../i18n";
import { UNNAMED_PARKING, type ParkingPlace } from "./api";

/** Returns a display name that translates the service placeholder for unnamed car parks. */
export function useParkingName() {
  const { t } = useI18n();
  return useCallback(
    (place: ParkingPlace) =>
      place.name === UNNAMED_PARKING ? t("carPark") : place.name,
    [t],
  );
}

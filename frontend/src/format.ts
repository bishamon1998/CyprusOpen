import { useCallback } from "react";
import { useI18n } from "./i18n";

/** Formats a timestamp as a Cyprus calendar date, e.g. "6 Nov 2026"; "—" when missing. */
export function useCyprusDate() {
  const { language } = useI18n();
  return useCallback(
    (value: string | null | undefined) =>
      value
        ? new Date(value).toLocaleDateString(language === "en" ? "en-GB" : language, {
            day: "numeric",
            month: "short",
            year: "numeric",
            timeZone: "Asia/Nicosia",
          })
        : "—",
    [language],
  );
}

/** Straight-line distance in kilometres with one decimal, e.g. "1.4 km away". */
export function useDistanceLabel() {
  const { t, language } = useI18n();
  return useCallback(
    (kilometres: number) =>
      t("distance", {
        distance: kilometres.toLocaleString(language, {
          minimumFractionDigits: 1,
          maximumFractionDigits: 1,
        }),
      }),
    [t, language],
  );
}

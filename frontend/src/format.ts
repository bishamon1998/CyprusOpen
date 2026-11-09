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

/** Formats a timestamp as Cyprus date and time, e.g. "9 Nov 2026, 05:20"; "—" when missing. */
export function useCyprusDateTime() {
  const { language } = useI18n();
  return useCallback(
    (value: string | null | undefined) =>
      value
        ? new Date(value).toLocaleString(language === "en" ? "en-GB" : language, {
            day: "numeric",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            timeZone: "Asia/Nicosia",
          })
        : "—",
    [language],
  );
}

/**
 * The two dates every tool shows under its data: when the source last changed the data, and when this site last
 * checked the source for changes. The first is often much older: many sources are rarely updated.
 */
export function useSourceDates() {
  const { t } = useI18n();
  const date = useCyprusDate();
  const dateTime = useCyprusDateTime();
  return useCallback(
    (published: string | null | undefined, checked: string | null | undefined, publishedWithTime = false) => [
      t("dataFrom", { date: (publishedWithTime ? dateTime : date)(published) }),
      t("lastChecked", { date: dateTime(checked) }),
    ],
    [t, date, dateTime],
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

import { useMemo } from "react";
import { useI18n } from "../i18n";

/**
 * How a series is measured; decides number formatting in axes, tooltips and summaries.
 * "permille" values are per 1,000 and shown as %; "per10k" values are € of every 10,000 € and shown per 100 €.
 */
export type Unit =
  | "count"
  | "thousands"
  | "percent"
  | "permille"
  | "euro"
  | "per10k"
  | "index"
  | "tonnes"
  | "years"
  | "nights";

/** Short axis labels: 12K, 1.2M, 3%, €2K. */
export function useAxisFormat() {
  const { language } = useI18n();
  return useMemo(() => {
    const compact = new Intl.NumberFormat(language, { notation: "compact", maximumFractionDigits: 1 });
    const euro = new Intl.NumberFormat(language, {
      style: "currency",
      currency: "EUR",
      notation: "compact",
      maximumFractionDigits: 1,
    });
    return (value: number, unit: Unit) => {
      switch (unit) {
        case "percent":
          return compact.format(value) + "%";
        case "permille":
          return compact.format(value / 10) + "%";
        case "euro":
          return euro.format(value);
        case "per10k":
          return euro.format(value / 100);
        case "thousands":
          return compact.format(value * 1000);
        default:
          return compact.format(value);
      }
    };
  }, [language]);
}

/** Formats values of a unit in the current language. "thousands" values are shown as whole numbers. */
export function useValueFormat() {
  const { t, language } = useI18n();
  return useMemo(() => {
    const whole = new Intl.NumberFormat(language, { maximumFractionDigits: 0 });
    const decimal = new Intl.NumberFormat(language, { maximumFractionDigits: 1 });
    const euro = new Intl.NumberFormat(language, {
      style: "currency",
      currency: "EUR",
      maximumFractionDigits: 0,
    });
    const cents = new Intl.NumberFormat(language, { style: "currency", currency: "EUR" });
    return (value: number | null, unit: Unit) => {
      if (value === null) return "–";
      switch (unit) {
        case "thousands":
          return whole.format(value * 1000);
        case "percent":
          return decimal.format(value) + "%";
        case "permille":
          return decimal.format(value / 10) + "%";
        case "euro":
          return euro.format(value);
        case "per10k":
          return cents.format(value / 100);
        case "index":
          return decimal.format(value);
        case "nights":
          return t("nights", { value: decimal.format(value) });
        case "tonnes":
          return t("tonnes", { value: whole.format(value) });
        case "years":
          return t("years", { value: decimal.format(value) });
        default:
          return whole.format(value);
      }
    };
  }, [t, language]);
}

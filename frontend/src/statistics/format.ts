import { useMemo } from "react";
import { useI18n } from "../i18n";

/** How a series is measured; decides number formatting in axes, tooltips and summaries. */
export type Unit = "count" | "thousands" | "percent" | "euro" | "tonnes" | "years";

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
    return (value: number, unit: Unit) =>
      unit === "percent"
        ? compact.format(value) + "%"
        : unit === "euro"
          ? euro.format(value)
          : compact.format(unit === "thousands" ? value * 1000 : value);
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
    return (value: number | null, unit: Unit) => {
      if (value === null) return "–";
      switch (unit) {
        case "thousands":
          return whole.format(value * 1000);
        case "percent":
          return decimal.format(value) + "%";
        case "euro":
          return euro.format(value);
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

import { CalendarDays, RotateCcw } from "lucide-react";
import { NearMeButton, ToolFilters } from "../components/tool/Tool";
import { useI18n } from "../i18n";

export function PharmacyFilters({
  date,
  scheduleDate,
  locating,
  onDate,
  onNearMe,
}: {
  date: string;
  scheduleDate: string | undefined;
  locating: boolean;
  onDate: (date: string) => void;
  onNearMe: () => void;
}) {
  const { t } = useI18n();
  return (
    <ToolFilters>
      <label className="tool-search">
        <CalendarDays size={17} aria-hidden="true" />
        <input
          type="date"
          aria-label={t("scheduleDate")}
          value={date || scheduleDate || ""}
          onChange={(event) => onDate(event.target.value)}
        />
      </label>
      {date ? (
        <button onClick={() => onDate("")}>
          <RotateCcw size={16} aria-hidden="true" />
          {t("pharmacyCurrent")}
        </button>
      ) : (
        <NearMeButton locating={locating} onClick={onNearMe} />
      )}
    </ToolFilters>
  );
}

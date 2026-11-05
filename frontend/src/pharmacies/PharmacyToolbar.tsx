import { useI18n } from "../i18n";
import { pharmacyScheduleUrl } from "../sources";

export function PharmacyToolbar({
  date,
  scheduleDate,
  source,
  canLocate,
  locating,
  onDate,
  onNearMe,
}: {
  date: string;
  scheduleDate: string | undefined;
  source: string | undefined;
  canLocate: boolean;
  locating: boolean;
  onDate: (date: string) => void;
  onNearMe: () => void;
}) {
  const { t } = useI18n();
  return (
    <div className="pharmacy-toolbar">
      <label>
        {t("scheduleDate")}
        <input
          type="date"
          value={date || scheduleDate || ""}
          onChange={(event) => onDate(event.target.value)}
        />
      </label>
      {date && (
        <button onClick={() => onDate("")}>{t("pharmacyCurrent")}</button>
      )}
      {!date && canLocate && (
        <button onClick={onNearMe} disabled={locating}>
          {t(locating ? "locating" : "nearMe")}
        </button>
      )}
      <a href={source ?? pharmacyScheduleUrl} target="_blank" rel="noreferrer">
        {t("source")}
      </a>
    </div>
  );
}

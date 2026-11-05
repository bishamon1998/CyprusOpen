import { type ParkingReport } from "../api";
import { useI18n } from "../i18n";
export function ParkingAttribution({
  directory,
}: {
  directory: ParkingReport["directory"] | undefined;
}) {
  const { t, language } = useI18n();
  const formatDate = (value: string | null | undefined) =>
    value
      ? new Date(value).toLocaleDateString(
          language === "en" ? "en-GB" : language,
          { day: "numeric", month: "short", year: "numeric", timeZone: "Asia/Nicosia" },
        )
      : "—";
  return (
    <div className="parking-attribution">
      <a
        href={
          directory?.url ||
          "https://www.traffic4cyprus.org.cy/en/dataset/parking_areas"
        }
        target="_blank"
        rel="noopener noreferrer"
      >
        <img src="/cynap-logo.png" alt="CYNAP" />
        {t("department")}
      </a>
      <span>
        {t("published", {
          date: formatDate(directory?.publishedAt),
        })}{" "}
        ·{" "}
        {t("checked", {
          date: formatDate(directory?.checkedAt),
        })}{" "}
        ·{" "}
        <a
          href="https://creativecommons.org/licenses/by/4.0/"
          target="_blank"
          rel="noopener noreferrer"
        >
          CC BY 4.0
        </a>{" "}
        ·{" "}
        <a
          href="https://www.openstreetmap.org/copyright"
          target="_blank"
          rel="noopener noreferrer"
        >
          {t("osm")}
        </a>
      </span>
    </div>
  );
}

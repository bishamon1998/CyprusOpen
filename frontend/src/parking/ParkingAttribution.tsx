import { useI18n } from "../i18n";
import { ccByLicenseUrl, osmCopyrightUrl, parkingDirectoryUrl } from "../sources";
import { type ParkingReport } from "./api";

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
          {
            day: "numeric",
            month: "short",
            year: "numeric",
            timeZone: "Asia/Nicosia",
          },
        )
      : "—";
  return (
    <div className="parking-attribution">
      <a
        href={directory?.url || parkingDirectoryUrl}
        target="_blank"
        rel="noopener noreferrer"
      >
        <img src="/cynap-logo.png" alt="CYNAP" />
        {t("department")}
      </a>
      <span>
        {t("published", { date: formatDate(directory?.publishedAt) })} ·{" "}
        {t("checked", { date: formatDate(directory?.checkedAt) })} ·{" "}
        <a href={ccByLicenseUrl} target="_blank" rel="noopener noreferrer">
          CC BY 4.0
        </a>{" "}
        ·{" "}
        <a href={osmCopyrightUrl} target="_blank" rel="noopener noreferrer">
          {t("osm")}
        </a>
      </span>
    </div>
  );
}

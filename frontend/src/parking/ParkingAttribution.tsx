import { ExternalLink, SourceAttribution } from "../components/tool/Tool";
import { useCyprusDate } from "../format";
import { useI18n } from "../i18n";
import { ccByLicenseUrl, osmCopyrightUrl, parkingDirectoryUrl } from "../sources";
import { type ParkingReport } from "./api";

export function ParkingAttribution({
  directory,
}: {
  directory: ParkingReport["directory"] | undefined;
}) {
  const { t } = useI18n();
  const date = useCyprusDate();
  return (
    <SourceAttribution
      href={directory?.url || parkingDirectoryUrl}
      badge={<img src="/cynap-logo.png" alt="CYNAP" />}
      name={t("department")}
      facts={[
        t("published", { date: date(directory?.publishedAt) }),
        t("checked", { date: date(directory?.checkedAt) }),
        <ExternalLink href={ccByLicenseUrl}>CC BY 4.0</ExternalLink>,
        <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
      ]}
    />
  );
}

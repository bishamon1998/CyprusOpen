import { type GlyphName, SoftTile } from "../components/icons/SoftTile";
import { PageTitle } from "../components/PageTitle";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import {
  airQualityUrl,
  bathingWaterUrl,
  catalogUrl,
  bankHolidaysUrl,
  hospitalsSourceUrl,
  natureSourceUrl,
  nicosiaParkingUrl,
  parkingDirectoryUrl,
  pharmacyScheduleUrl,
  repositoryUrl,
} from "../sources";

const rows: {
  title: MessageKey;
  description: MessageKey;
  href: string;
  icon: GlyphName;
}[] = [
  {
    title: "statistics",
    description: "statisticsSource",
    href: catalogUrl,
    icon: "statistics",
  },
  {
    title: "parkingLocations",
    description: "directorySource",
    href: parkingDirectoryUrl,
    icon: "parking",
  },
  {
    title: "nicosiaParking",
    description: "nicosiaSource",
    href: nicosiaParkingUrl,
    icon: "parking",
  },
  {
    title: "dutyPharmacies",
    description: "pharmacySource",
    href: pharmacyScheduleUrl,
    icon: "pharmacies",
  },
  {
    title: "beaches",
    description: "beachSource",
    href: bathingWaterUrl,
    icon: "beaches",
  },
  {
    title: "airQuality",
    description: "airQualitySource",
    href: airQualityUrl,
    icon: "air",
  },
  {
    title: "nature",
    description: "natureSource",
    href: natureSourceUrl,
    icon: "nature",
  },
];

export default function About() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("sources")} path="/about" />
      <section className="about-card" aria-labelledby="about-title">
        <h2 id="about-title">{t("aboutTitle")}</h2>
        <p>{t("aboutWhat")}</p>
        <p>{t("aboutHow")}</p>
        <SourceLink href={repositoryUrl}>{t("sourceCode")}</SourceLink>
      </section>
      <h2 className="about-heading" id="data-sources">
        {t("dataSources")}
      </h2>
      <section className="sources-list" aria-labelledby="data-sources">
        {rows.map((row) => (
          <div className="source-row" key={row.title}>
            <SoftTile icon={row.icon} size={42} />
            <div>
              <h2>{t(row.title)}</h2>
              <p>{t(row.description)}</p>
            </div>
            <SourceLink href={row.href}>{t("open")}</SourceLink>
          </div>
        ))}
      </section>
    </>
  );
}

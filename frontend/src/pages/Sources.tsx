import { type ReactNode } from "react";
import { ChartColumn, MapPin, ParkingCircle, Pill, Wind } from "lucide-react";
import { PageTitle } from "../components/PageTitle";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import {
  airQualityUrl,
  catalogUrl,
  nicosiaParkingUrl,
  parkingDirectoryUrl,
  pharmacyScheduleUrl,
} from "../sources";

const rows: {
  title: MessageKey;
  description: MessageKey;
  href: string;
  icon: ReactNode;
}[] = [
  {
    title: "statistics",
    description: "statisticsSource",
    href: catalogUrl,
    icon: <ChartColumn size={21} />,
  },
  {
    title: "parkingLocations",
    description: "directorySource",
    href: parkingDirectoryUrl,
    icon: <MapPin size={21} />,
  },
  {
    title: "nicosiaParking",
    description: "nicosiaSource",
    href: nicosiaParkingUrl,
    icon: <ParkingCircle size={23} />,
  },
  {
    title: "dutyPharmacies",
    description: "pharmacySource",
    href: pharmacyScheduleUrl,
    icon: <Pill size={22} />,
  },
  {
    title: "airQuality",
    description: "airQualitySource",
    href: airQualityUrl,
    icon: <Wind size={22} />,
  },
];

export default function Sources() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("sources")} />
      <section className="sources-list" aria-label={t("dataSources")}>
        {rows.map((row) => (
          <div className="source-row" key={row.title}>
            <span className="quick-icon" aria-hidden="true">
              {row.icon}
            </span>
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

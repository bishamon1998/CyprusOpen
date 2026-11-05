import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";
import { ImmigrationChart } from "./ImmigrationChart";
import { TourismChart } from "./TourismChart";
import "./statistics.css";

export default function Statistics() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("statistics")} />
      <section className="dataset-grid" aria-label={t("datasets")}>
        <ImmigrationChart />
        <TourismChart />
      </section>
    </>
  );
}

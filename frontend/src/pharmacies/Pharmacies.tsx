import { Illustration } from "../components/Illustration";
import { PageTitle } from "../components/PageTitle";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { pharmacyScheduleUrl } from "../sources";

export default function Pharmacies() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("pharmacies")} />
      <section className="tool-panel">
        <div className="tool-empty">
          <Illustration kind="pharmacy" />
          <h2>{t("pharmacySoon")}</h2>
          <p>{t("pharmacyDescription")}</p>
        </div>
        <div className="card-footer">
          <SourceLink href={pharmacyScheduleUrl}>{t("source")}</SourceLink>
        </div>
      </section>
    </>
  );
}

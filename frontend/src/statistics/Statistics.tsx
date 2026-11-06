import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";
import { cards } from "./cards";
import { StatisticCard } from "./StatisticCard";
import "./statistics.css";

export default function Statistics() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("statistics")} />
      <section className="dataset-grid" aria-label={t("datasets")}>
        {cards.map((card) => (
          <StatisticCard key={card.id} card={card} />
        ))}
      </section>
    </>
  );
}

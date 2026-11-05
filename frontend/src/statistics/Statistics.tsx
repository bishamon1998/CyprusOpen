import { RefreshCw } from "lucide-react";
import { Illustration, type IllustrationKind } from "../components/Illustration";
import { PageTitle } from "../components/PageTitle";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { catalogUrl } from "../sources";
import { useDatasets } from "./api";

const cards: {
  kind: IllustrationKind;
  title: MessageKey;
  description: MessageKey;
}[] = [
  { kind: "people", title: "immigration", description: "immigrationDescription" },
  { kind: "travel", title: "tourism", description: "tourismDescription" },
];

export default function Statistics() {
  const datasets = useDatasets();
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("statistics")} />
      {datasets.isError && (
        <div className="error-banner" role="alert">
          <span>{t("statisticsError")}</span>
          <button
            onClick={() => void datasets.refetch()}
            disabled={datasets.isFetching}
          >
            <RefreshCw size={15} aria-hidden="true" />
            {t("retry")}
          </button>
        </div>
      )}
      <section className="dataset-grid" aria-label={t("datasets")}>
        {cards.map((card) => (
          <article className="dataset-card" key={card.kind}>
            <div className="card-heading">
              <h2>{t(card.title)}</h2>
            </div>
            <p className="card-description">{t(card.description)}</p>
            <div
              className="chart-empty"
              aria-live="polite"
              aria-busy={datasets.isPending}
            >
              <Illustration kind={card.kind} />
              <p>{t(datasets.isPending ? "loading" : "noData")}</p>
            </div>
            <div className="card-footer">
              <SourceLink href={catalogUrl}>{t("source")}</SourceLink>
            </div>
          </article>
        ))}
      </section>
    </>
  );
}

import { lazy, Suspense, type ReactNode } from "react";
import { RefreshCw } from "lucide-react";
import { type ChartOption } from "../components/Chart";
import { Illustration, type IllustrationKind } from "../components/Illustration";
import { SourceLink } from "../components/SourceLink";
import { useCyprusDate } from "../format";
import { useI18n } from "../i18n";
import { type DatasetSource } from "./api";

const Chart = lazy(() => import("../components/Chart"));

export type ChartState =
  | { status: "loading" }
  | { status: "error"; retry: () => void; retrying: boolean }
  | { status: "empty" }
  | { status: "ready"; option: ChartOption; label: string; height: number };

/**
 * Shared frame for every statistics chart: heading, controls, the chart or its
 * loading/empty/error state, a text summary and the source attribution.
 */
export function ChartCard({
  title,
  description,
  illustration,
  controls,
  state,
  summary,
  source,
}: {
  title: string;
  description: string;
  illustration: IllustrationKind;
  controls?: ReactNode;
  state: ChartState;
  summary?: ReactNode;
  source?: DatasetSource;
}) {
  const { t } = useI18n();
  const date = useCyprusDate();
  // Same Cyprus-time date as the map tools; the browser's own zone could show another day.
  const updated = source?.updatedAt ? date(source.updatedAt) : null;
  return (
    <article className="dataset-card">
      <div className="card-heading">
        <h3>{title}</h3>
      </div>
      <p className="card-description">{description}</p>
      {state.status !== "error" && controls && (
        <div className="chart-controls">{controls}</div>
      )}
      {state.status === "ready" ? (
        <Suspense fallback={<div className="chart" style={{ height: state.height }} />}>
          <Chart option={state.option} height={state.height} label={state.label} />
        </Suspense>
      ) : (
        <div className="chart-empty" aria-live="polite" aria-busy={state.status === "loading"}>
          <Illustration kind={illustration} />
          <p>
            {t(
              state.status === "loading"
                ? "loading"
                : state.status === "error"
                  ? "statisticsError"
                  : "noData",
            )}
          </p>
          {state.status === "error" && (
            <button className="text-button" onClick={state.retry} disabled={state.retrying}>
              <RefreshCw size={15} aria-hidden="true" />
              {t("retry")}
            </button>
          )}
        </div>
      )}
      {state.status === "ready" && summary && <p className="chart-summary">{summary}</p>}
      <div className="card-footer">
        {updated && <span className="card-updated">{t("sourceUpdated", { date: updated })}</span>}
        {source && (
          <SourceLink href={source.url}>
            {t("cystat")} · {source.license}
          </SourceLink>
        )}
      </div>
    </article>
  );
}

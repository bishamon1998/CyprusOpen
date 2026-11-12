import { lazy, Suspense, type ReactNode } from "react";
import { RefreshCw } from "lucide-react";
import { type ChartOption } from "../components/Chart";
import { Illustration, type IllustrationKind } from "../components/Illustration";
import { InfoPopover } from "../components/InfoPopover";
import { SourceLink } from "../components/SourceLink";
import { useSourceDates } from "../format";
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
 * loading/empty/error state. The text summary and the source sit in an info
 * popover at the bottom of the card.
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
  const sourceDates = useSourceDates();
  const showSummary = state.status === "ready" && summary;
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
      {(source || showSummary) && (
        <div className="info-bar">
          <InfoPopover>
            {showSummary && <p className="info-popover-lead">{summary}</p>}
            {source && sourceDates(source.updatedAt, source.checkedAt).map((line) => <p key={line}>{line}</p>)}
            {source && (
              <SourceLink href={source.url}>
                {t("cystat")}, {source.license}
              </SourceLink>
            )}
          </InfoPopover>
        </div>
      )}
    </article>
  );
}

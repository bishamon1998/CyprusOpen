import { lazy, Suspense, useEffect, useId, useRef, useState, type ReactNode } from "react";
import { Info, RefreshCw } from "lucide-react";
import { type ChartOption } from "../components/Chart";
import { Illustration, type IllustrationKind } from "../components/Illustration";
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
 * loading/empty/error state. The text summary and the source attribution sit in
 * an info popover next to the title.
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
        {(source || showSummary) && (
          <DataInfo>
            {showSummary && <p className="chart-summary">{summary}</p>}
            {source && (
              <p className="card-updated">{sourceDates(source.updatedAt, source.checkedAt).join(" · ")}</p>
            )}
            {source && (
              <SourceLink href={source.url}>
                {t("cystat")} · {source.license}
              </SourceLink>
            )}
          </DataInfo>
        )}
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
    </article>
  );
}

/** An "i" button that opens a small panel with the data's summary, dates and source. */
function DataInfo({ children }: { children: ReactNode }) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const panel = useId();

  useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div className="data-info" ref={root}>
      <button
        type="button"
        className="data-info-button"
        aria-label={t("aboutData")}
        aria-expanded={open}
        aria-controls={open ? panel : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        <Info size={17} aria-hidden="true" />
      </button>
      {open && (
        <div id={panel} className="data-info-panel" role="dialog" aria-label={t("aboutData")}>
          {children}
        </div>
      )}
    </div>
  );
}

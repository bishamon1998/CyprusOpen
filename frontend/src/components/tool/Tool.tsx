import { type ReactNode, type RefObject } from "react";
import { ArrowUpRight, LocateFixed, X } from "lucide-react";
import { useI18n } from "../../i18n";
import { type MessageKey } from "../../locales";
import { Mouflon, type MouflonScene } from "../Mouflon";
import "./tool.css";

/**
 * Building blocks shared by the map tools (Parking, Pharmacies): a framed workspace with
 * filters, a map beside a results panel, selectable rows and a source attribution footer.
 */
export function ToolWorkspace({ label, children }: { label: string; children: ReactNode }) {
  return (
    <section className="tool-workspace" aria-label={label}>
      {children}
    </section>
  );
}

export function ToolFilters({ children }: { children: ReactNode }) {
  return <div className="tool-filters">{children}</div>;
}

export function NearMeButton({ locating, onClick }: { locating: boolean; onClick: () => void }) {
  const { t } = useI18n();
  return (
    <button onClick={onClick} disabled={locating}>
      <LocateFixed size={17} aria-hidden="true" />
      {t(locating ? "locating" : "nearMe")}
    </button>
  );
}

/** Shown in the results heading while results are sorted by distance; clears the location. */
export function NearestFirst({ onClear }: { onClear: () => void }) {
  const { t } = useI18n();
  return (
    <button onClick={onClear} aria-label={t("clearLocation")}>
      <X size={15} />
      {t("nearest")}
    </button>
  );
}

export function ToolFeedback({ children }: { children: ReactNode }) {
  return (
    <p className="tool-feedback" role="status">
      {children}
    </p>
  );
}

export function ToolLayout({ map, children }: { map: ReactNode; children: ReactNode }) {
  return (
    <div className="tool-layout">
      {map}
      {children}
    </div>
  );
}

export function ResultsPanel({
  heading,
  action,
  notes,
  panelRef,
  children,
}: {
  heading: string;
  action?: ReactNode;
  notes?: ReactNode[];
  panelRef?: RefObject<HTMLDivElement | null>;
  children: ReactNode;
}) {
  return (
    <div ref={panelRef} className="tool-results">
      <div className="tool-results-heading">
        <h2>{heading}</h2>
        {action}
      </div>
      {notes?.filter(Boolean).map((note, index) => (
        <p className="tool-note" key={index}>
          {note}
        </p>
      ))}
      {children}
    </div>
  );
}

const messageLooks: Record<"empty" | "pending" | "error" | "offline", { scene: MouflonScene; title: MessageKey }> = {
  empty: { scene: "empty", title: "emptyTitle" },
  pending: { scene: "loading", title: "pendingTitle" },
  error: { scene: "error", title: "errorTitle" },
  offline: { scene: "offline", title: "offlineTitle" },
};

/**
 * A results panel with nothing to list: no matches ("empty"), data not imported yet ("pending") or a failed load,
 * which is a retryable error, or "offline" when the browser has no network.
 */
export function ResultsMessage({
  children,
  kind,
  onRetry,
  onReset,
}: {
  children: ReactNode;
  kind?: "empty" | "pending";
  onRetry?: () => void;
  /** Clears the search and filters behind a no-match result. */
  onReset?: () => void;
}) {
  const { t } = useI18n();
  const offline = onRetry !== undefined && typeof navigator !== "undefined" && !navigator.onLine;
  const look = messageLooks[onRetry ? (offline ? "offline" : "error") : (kind ?? "empty")];
  return (
    <div className="result-list-empty" role={onRetry ? "alert" : undefined}>
      <Mouflon scene={look.scene} className="result-list-mouflon" />
      <strong>{t(look.title)}</strong>
      <p>{offline ? t("offlineText") : children}</p>
      {onRetry && <button onClick={onRetry}>{t("retry")}</button>}
      {onReset && <button onClick={onReset}>{t("resetSearch")}</button>}
    </div>
  );
}

export function ResultRow({
  icon,
  tone,
  title,
  details,
  selected,
  lang,
  onSelect,
}: {
  /** A short symbol, or a schematic icon (Glyph). */
  icon: ReactNode;
  /** Colours the icon (a CSS class "tone-…"), e.g. an air quality level. */
  tone?: string;
  title: string;
  details: ReactNode[];
  selected: boolean;
  lang?: string;
  onSelect: () => void;
}) {
  return (
    <li>
      <button
        className={`result-row${selected ? " selected" : ""}`}
        aria-pressed={selected}
        onClick={onSelect}
      >
        <span className={"result-row-icon" + (tone ? " tone-" + tone : "")} aria-hidden="true">
          {icon}
        </span>
        <span>
          <strong lang={lang}>{title}</strong>
          {details.filter(Boolean).map((detail, index) => (
            <small key={index}>{detail}</small>
          ))}
        </span>
        <ArrowUpRight size={16} aria-hidden="true" />
      </button>
    </li>
  );
}

export function SelectedResult({
  label,
  title,
  detail,
  lang,
  children,
}: {
  label: string;
  title: string;
  detail?: string;
  lang?: string;
  children: ReactNode;
}) {
  return (
    <div className="selected-result" aria-live="polite">
      <span>{label}</span>
      <h3 lang={lang}>{title}</h3>
      {detail && <p lang={lang}>{detail}</p>}
      <div className="selected-result-actions">{children}</div>
    </div>
  );
}

export function ShowMore({ onClick }: { onClick: () => void }) {
  const { t } = useI18n();
  return (
    <button className="show-more" onClick={onClick}>
      {t("more")}
    </button>
  );
}

/** Footer naming the publisher, followed by facts such as check dates and licence links. */
export function SourceAttribution({
  href,
  badge,
  name,
  facts,
}: {
  href: string;
  badge: ReactNode;
  name: string;
  facts: ReactNode[];
}) {
  return (
    <div className="tool-attribution">
      <a href={href} target="_blank" rel="noopener noreferrer">
        {badge}
        {name}
      </a>
      <span>
        {facts.map((fact, index) => (
          <span key={index}>
            {index > 0 && " · "}
            {fact}
          </span>
        ))}
      </span>
    </div>
  );
}

export function ExternalLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  );
}

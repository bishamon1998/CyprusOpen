import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { Info } from "lucide-react";
import { useI18n } from "../i18n";
import "./info-popover.css";

/**
 * An "i" button in the bottom corner of a card or tool that opens a small panel above it with details such as
 * data dates, the publisher and the licence. Closes on a click outside or Escape.
 */
export function InfoPopover({ children }: { children: ReactNode }) {
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
    <div className="info-popover" ref={root}>
      {open && (
        <div id={panel} className="info-popover-panel" role="dialog" aria-label={t("aboutData")}>
          {children}
        </div>
      )}
      <button
        type="button"
        className="info-popover-button"
        aria-label={t("aboutData")}
        aria-expanded={open}
        aria-controls={open ? panel : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        <Info size={17} aria-hidden="true" />
      </button>
    </div>
  );
}

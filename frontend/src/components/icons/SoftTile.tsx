import { type CSSProperties, type ReactNode } from "react";
import "./icons.css";

/**
 * Schematic icons drawn for this site on a 24px grid with a 2px stroke in the current colour, so a tile tints
 * them. Each section has its own colour (see icons.css).
 */
const glyphs = {
  statistics: (
    <>
      <path d="M3.5 20h17" />
      <path d="M6.5 20v-7" />
      <path d="M12 20V6" />
      <path d="M17.5 20v-10" />
    </>
  ),
  parking: (
    <>
      <rect x="3.5" y="3.5" width="17" height="17" rx="5" />
      <path d="M9.5 16.5v-9h3.25a2.75 2.75 0 0 1 0 5.5H9.5" />
    </>
  ),
  pharmacies: (
    <>
      <path d="M10.2 3.8a4.6 4.6 0 0 1 6.5 6.5l-6.4 6.4a4.6 4.6 0 0 1-6.5-6.5z" />
      <path d="M7 7l6.5 6.5" />
      <path d="M18.5 15v5M16 17.5h5" />
    </>
  ),
  beaches: (
    <>
      <path d="M12 4.5a7.5 7.5 0 0 1 7.5 7.5h-15A7.5 7.5 0 0 1 12 4.5z" />
      <path d="M12 12v5.5" />
      <path d="M3 20.5q2.25-1.6 4.5 0t4.5 0 4.5 0 4.5 0" />
    </>
  ),
  air: (
    <>
      <path d="M3 8.5h10a3 3 0 1 0-3-3" />
      <path d="M3 12.5h14.5a3 3 0 1 1-3 3" />
      <path d="M3 16.5h7" />
    </>
  ),
  nature: (
    <>
      <path d="M12 3l5 7h-3l4 6.5H6l4-6.5H7z" />
      <path d="M12 16.5V21" />
    </>
  ),
  about: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 11v5.5" />
      <path d="M12 7.7h.01" strokeWidth="2.8" />
    </>
  ),
  // Statistics categories.
  people: (
    <>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3.5 19.5a5.5 5.5 0 0 1 11 0" />
      <circle cx="16.8" cy="9.2" r="2.5" />
      <path d="M15.6 14.3a4.6 4.6 0 0 1 5.2 5.2" />
    </>
  ),
  family: <path d="M12 20s-7.5-4.4-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.6-7.5 10-7.5 10z" />,
  economy: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M15.2 8.7a4 4 0 1 0 0 6.6" />
      <path d="M7.8 11h5.4M7.8 13.2h5.4" />
    </>
  ),
  housing: (
    <>
      <path d="M3.8 11.2L12 4.5l8.2 6.7" />
      <path d="M6 9.5V20h12V9.5" />
      <path d="M10 20v-5h4v5" />
    </>
  ),
  transport: (
    <>
      <path d="M5.5 11.5l1.6-4a2 2 0 0 1 1.9-1.3h6a2 2 0 0 1 1.9 1.3l1.6 4" />
      <rect x="3.5" y="11.5" width="17" height="5.5" rx="2" />
      <path d="M6.5 17v2M17.5 17v2" />
      <path d="M7 14.2h.01M17 14.2h.01" strokeWidth="2.6" />
    </>
  ),
  tourism: (
    <>
      <rect x="4" y="8" width="16" height="12" rx="2.5" />
      <path d="M9 8V5.5h6V8" />
      <path d="M9 8v12M15 8v12" />
    </>
  ),
  safety: (
    <>
      <path d="M12 3.5l7 3v5c0 4.6-3 7.6-7 9-4-1.4-7-4.4-7-9v-5z" />
      <path d="M9 12l2.2 2.2L15.2 10" />
    </>
  ),
  // A pharmacy in a list.
  cross: <path d="M9.5 4h5v5.5H20v5h-5.5V20h-5v-5.5H4v-5h5.5z" />,
} satisfies Record<string, ReactNode>;

export type GlyphName = keyof typeof glyphs;

export function Glyph({ name }: { name: GlyphName }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {glyphs[name]}
    </svg>
  );
}

/** A schematic icon on a light tile of its section's colour. Decorative: the label next to it names it. */
export function SoftTile({ icon, tone = icon, size }: { icon: GlyphName; tone?: GlyphName; size?: number }) {
  return (
    <span
      className={`soft-tile tone-${tone}-tile`}
      style={size ? ({ "--tile-size": `${size}px` } as CSSProperties) : undefined}
      aria-hidden="true"
    >
      <Glyph name={icon} />
    </span>
  );
}

import airQuality from "../assets/mouflons/mouflon-air-quality.webp";
import beaches from "../assets/mouflons/mouflon-beaches.webp";
import empty from "../assets/mouflons/mouflon-empty.webp";
import error from "../assets/mouflons/mouflon-error.webp";
import loading from "../assets/mouflons/mouflon-loading.webp";
import nature from "../assets/mouflons/mouflon-nature.webp";
import notFound from "../assets/mouflons/mouflon-not-found.webp";
import offline from "../assets/mouflons/mouflon-offline.webp";
import parking from "../assets/mouflons/mouflon-parking.webp";
import pharmacies from "../assets/mouflons/mouflon-pharmacies.webp";
import sources from "../assets/mouflons/mouflon-sources.webp";
import statistics from "../assets/mouflons/mouflon-statistics.webp";

/** The site's mouflon in its scenes: one per section and one per empty or failed state (362 px WebP). */
const scenes = {
  "air-quality": airQuality,
  beaches,
  empty,
  error,
  loading,
  nature,
  "not-found": notFound,
  offline,
  parking,
  pharmacies,
  sources,
  statistics,
};

export type MouflonScene = keyof typeof scenes;

/** Decorative: the text beside it always says the same thing. */
export function Mouflon({ scene, className }: { scene: MouflonScene; className?: string }) {
  return (
    <img
      className={className}
      src={scenes[scene]}
      alt=""
      width={362}
      height={362}
      decoding="async"
    />
  );
}

import { type GlyphName } from "./components/icons/SoftTile";
import { type MouflonScene } from "./components/Mouflon";
import { type MessageKey } from "./locales";

/**
 * The site's sections in menu order: the menu, page headers, the not-found page and the home page shortcuts all
 * read them from here.
 */
export const sections: {
  path: string;
  label: MessageKey;
  icon: GlyphName;
  /** One line under the page title. */
  lead: MessageKey;
  /** A few words on a home page shortcut. */
  short: MessageKey;
  mouflon: MouflonScene;
}[] = [
  { path: "/", label: "statistics", icon: "statistics", lead: "statisticsLead", short: "statisticsShort", mouflon: "statistics" },
  { path: "/parking", label: "parking", icon: "parking", lead: "parkingLead", short: "parkingShort", mouflon: "parking" },
  { path: "/pharmacies", label: "pharmacies", icon: "pharmacies", lead: "pharmaciesLead", short: "pharmaciesShort", mouflon: "pharmacies" },
  { path: "/beaches", label: "beaches", icon: "beaches", lead: "beachesLead", short: "beachesShort", mouflon: "beaches" },
  { path: "/air", label: "airQuality", icon: "air", lead: "airLead", short: "airShort", mouflon: "air-quality" },
  { path: "/nature", label: "nature", icon: "nature", lead: "natureLead", short: "natureShort", mouflon: "nature" },
  { path: "/about", label: "sources", icon: "about", lead: "aboutLead", short: "aboutShort", mouflon: "sources" },
];

export function section(path: string) {
  return sections.find((item) => item.path === path)!;
}

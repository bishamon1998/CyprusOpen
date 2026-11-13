import { type GlyphName } from "./components/icons/SoftTile";
import { type MouflonScene } from "./components/Mouflon";
import { type MessageKey } from "./locales";

/**
 * The site's sections in menu order: the menu, page headers, and the not-found page all read them
 * from here.
 */
export const sections: {
  path: string;
  label: MessageKey;
  icon: GlyphName;
  /** One line under the page title. */
  lead: MessageKey;
  mouflon: MouflonScene;
}[] = [
  { path: "/", label: "statistics", icon: "statistics", lead: "statisticsLead", mouflon: "statistics" },
  { path: "/parking", label: "parking", icon: "parking", lead: "parkingLead", mouflon: "parking" },
  { path: "/pharmacies", label: "pharmacies", icon: "pharmacies", lead: "pharmaciesLead", mouflon: "pharmacies" },
  { path: "/beaches", label: "beaches", icon: "beaches", lead: "beachesLead", mouflon: "beaches" },
  { path: "/air", label: "airQuality", icon: "air", lead: "airLead", mouflon: "air-quality" },
  { path: "/nature", label: "nature", icon: "nature", lead: "natureLead", mouflon: "nature" },
  { path: "/services", label: "services", icon: "services", lead: "servicesLead", mouflon: "pharmacies" },
  { path: "/recycling", label: "recycling", icon: "recycling", lead: "recyclingLead", mouflon: "nature" },
  { path: "/tools", label: "tools", icon: "tools", lead: "toolsLead", mouflon: "sources" },
  { path: "/about", label: "sources", icon: "about", lead: "aboutLead", mouflon: "sources" },
];

export function section(path: string) {
  return sections.find((item) => item.path === path)!;
}

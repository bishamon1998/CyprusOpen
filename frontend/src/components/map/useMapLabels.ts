import { useMemo } from "react";
import { useI18n } from "../../i18n";
import { type MessageKey } from "../../locales";
import { type MapLabels } from "./MapView";

/**
 * Translated map texts for a tool: `region` names the map for screen readers and `count` labels a
 * cluster ("{count} car parks"). Stable per language, so markers are not rebuilt on every render.
 */
export function useMapLabels(region: MessageKey, count: MessageKey): MapLabels {
  const { t } = useI18n();
  return useMemo(
    () => ({
      region: t(region),
      unavailable: t("tilesUnavailable"),
      location: t("yourLocation"),
      cluster: (total: number) => t(count, { count: total }),
    }),
    [t, region, count],
  );
}

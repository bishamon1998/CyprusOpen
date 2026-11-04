import { useMemo } from "react";
import { MapView } from "../components/map/MapView";
import { type ParkingPlace } from "../api";
import { type Area } from "../geo/cyprusAreas";
import { type Position } from "../geo/coordinates";
import { useI18n } from "../i18n";

export function ParkingMap({
  places,
  area,
  selected,
  position,
  onSelect,
}: {
  places: ParkingPlace[];
  area: Area;
  selected: ParkingPlace | undefined;
  position: Position | null;
  onSelect: (id: string) => void;
}) {
  const { t } = useI18n();
  const points = useMemo(
    () =>
      places.map((place) => ({
        id: place.id,
        label: place.name === "Car park" ? t("carPark") : place.name,
        latitude: place.latitude,
        longitude: place.longitude,
      })),
    [places, t],
  );
  const labels = useMemo(
    () => ({
      region: t("mapLabel"),
      unavailable: t("tilesUnavailable"),
      hint: t("mapHint"),
      location: t("yourLocation"),
      cluster: (count: number) => t("carParkCount", { count }),
    }),
    [t],
  );
  return (
    <MapView
      points={points}
      bounds={area.bounds}
      selectedId={selected?.id}
      position={position}
      onSelect={onSelect}
      markerSymbol="P"
      labels={labels}
    />
  );
}

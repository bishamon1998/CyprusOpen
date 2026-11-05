import { useMemo } from "react";
import { MapView } from "../components/map/MapView";
import { type Position } from "../geo/coordinates";
import { type Area } from "../geo/cyprusAreas";
import { useI18n } from "../i18n";
import { type ParkingPlace } from "./api";
import { useParkingName } from "./useParkingName";

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
  const name = useParkingName();
  const points = useMemo(
    () =>
      places.map((place) => ({
        id: place.id,
        label: name(place),
        latitude: place.latitude,
        longitude: place.longitude,
      })),
    [places, name],
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

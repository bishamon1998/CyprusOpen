import { useMemo } from "react";
import { MapView } from "../components/map/MapView";
import { useMapLabels } from "../components/map/useMapLabels";
import { type Position } from "../geo/coordinates";
import { type Area } from "../geo/cyprusAreas";
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
  const labels = useMapLabels("mapLabel", "carParkCount");
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

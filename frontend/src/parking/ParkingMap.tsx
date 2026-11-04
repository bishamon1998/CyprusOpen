import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet.markercluster";
import "leaflet/dist/leaflet.css";
import "leaflet.markercluster/dist/MarkerCluster.css";
import { type ParkingPlace, useMapConfig } from "../api";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";

export type Area = {
  id: string;
  name: MessageKey;
  bounds: [[number, number], [number, number]];
};
export type Position = { latitude: number; longitude: number };

export const areas: Area[] = [
  {
    id: "cyprus",
    name: "areaCyprus",
    bounds: [
      [34.5, 32.25],
      [35.75, 34.65],
    ],
  },
  {
    id: "nicosia",
    name: "areaNicosia",
    bounds: [
      [35.04, 33.21],
      [35.24, 33.49],
    ],
  },
  {
    id: "limassol",
    name: "areaLimassol",
    bounds: [
      [34.61, 32.9],
      [34.78, 33.2],
    ],
  },
  {
    id: "larnaca",
    name: "areaLarnaca",
    bounds: [
      [34.84, 33.51],
      [35.01, 33.72],
    ],
  },
  {
    id: "paphos",
    name: "areaPaphos",
    bounds: [
      [34.7, 32.35],
      [34.87, 32.52],
    ],
  },
  {
    id: "ayia-napa",
    name: "areaAyiaNapa",
    bounds: [
      [34.95, 33.9],
      [35.09, 34.1],
    ],
  },
];

const pin = (selected: boolean) =>
  L.divIcon({
    className: `parking-pin${selected ? " is-selected" : ""}`,
    html: '<span aria-hidden="true">P</span>',
    iconSize: [30, 34],
    iconAnchor: [15, 34],
  });

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
  const { t, language } = useI18n();
  const container = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const markers = useRef(new Map<string, L.Marker>());
  const clusters = useRef<L.MarkerClusterGroup | null>(null);
  const select = useRef(onSelect);
  select.current = onSelect;
  const config = useMapConfig();
  const [tileError, setTileError] = useState(false);

  useEffect(() => {
    const instance = L.map(container.current!, {
      scrollWheelZoom: false,
      zoomControl: false,
      maxZoom: 19,
    });
    instance.fitBounds(areas[0].bounds);
    L.control.zoom({ position: "topright" }).addTo(instance);
    map.current = instance;
    const resize = new ResizeObserver(() => instance.invalidateSize());
    resize.observe(container.current!);
    return () => {
      resize.disconnect();
      instance.remove();
      map.current = null;
    };
  }, []);

  useEffect(() => {
    if (!map.current || !config.data) return;
    const attribution = document.createElement("a");
    attribution.href = config.data.attributionUrl;
    attribution.textContent = config.data.attribution;
    attribution.target = "_blank";
    attribution.rel = "noopener noreferrer";
    const tiles = L.tileLayer(config.data.tileUrl, {
      attribution: attribution.outerHTML,
      maxZoom: 19,
      updateWhenIdle: true,
      keepBuffer: 0,
    });
    tiles.on("tileerror", () => setTileError(true));
    tiles.addTo(map.current);
    return () => {
      tiles.remove();
    };
  }, [config.data]);

  useEffect(() => {
    map.current?.fitBounds(area.bounds, { padding: [24, 24], animate: false });
  }, [area]);

  useEffect(() => {
    if (!map.current) return;
    const group = L.markerClusterGroup({
      showCoverageOnHover: false,
      maxClusterRadius: 45,
      animate: false,
      iconCreateFunction: (cluster) => {
        const content = document.createElement("span");
        content.textContent = String(cluster.getChildCount());
        content.setAttribute(
          "aria-label",
          t("carParkCount", { count: cluster.getChildCount() }),
        );
        return L.divIcon({
          html: content,
          className: "parking-cluster",
          iconSize: [40, 40],
        });
      },
    }).addTo(map.current);
    clusters.current = group;
    markers.current.clear();
    for (const place of places) {
      const name = place.name === "Car park" ? t("carPark") : place.name;
      const marker = L.marker([place.latitude, place.longitude], {
        icon: pin(false),
        title: name,
        alt: name,
        keyboard: true,
      }).addTo(group);
      const tooltip = document.createElement("span");
      tooltip.textContent = name;
      marker.bindTooltip(tooltip).on("click", () => select.current(place.id));
      markers.current.set(place.id, marker);
    }
    return () => {
      group.remove();
      markers.current.clear();
      clusters.current = null;
    };
  }, [places, language]);

  useEffect(() => {
    for (const [id, marker] of markers.current)
      marker.setIcon(pin(id === selected?.id));
    if (selected && map.current) {
      map.current.setView(
        [selected.latitude, selected.longitude],
        Math.max(15, map.current.getZoom()),
        { animate: false },
      );
      const marker = markers.current.get(selected.id);
      if (marker)
        clusters.current?.zoomToShowLayer(marker, () => marker.openTooltip());
    }
  }, [selected, places, language]);

  useEffect(() => {
    if (!position || !map.current) return;
    const marker = L.circleMarker([position.latitude, position.longitude], {
      radius: 7,
      color: "white",
      weight: 3,
      fillColor: "#4263eb",
      fillOpacity: 1,
    })
      .addTo(map.current)
      .bindTooltip(t("yourLocation"));
    map.current.setView([position.latitude, position.longitude], 14, {
      animate: false,
    });
    return () => {
      marker.remove();
    };
  }, [position, language]);

  return (
    <div className="parking-map-shell">
      <div
        ref={container}
        className="parking-map"
        role="region"
        aria-label={t("mapLabel")}
      />
      {(tileError || config.isError) && (
        <p className="map-notice" role="status">
          {t("tilesUnavailable")}
        </p>
      )}
      <span className="map-hint">{t("mapHint")}</span>
    </div>
  );
}

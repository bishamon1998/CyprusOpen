import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet.markercluster";
import "leaflet/dist/leaflet.css";
import "leaflet.markercluster/dist/MarkerCluster.css";
import { type Bounds, type Position } from "../../geo/coordinates";
import { useMapConfig } from "./useMapConfig";
import "./map.css";

const pin = (symbol: string, selected: boolean, tone?: string) => {
  const content = document.createElement("span");
  content.textContent = symbol;
  content.setAttribute("aria-hidden", "true");
  return L.divIcon({
    className: "map-pin" + (selected ? " is-selected" : "") + (tone ? " tone-" + tone : ""),
    html: content,
    iconSize: [30, 34],
    iconAnchor: [15, 34],
  });
};

/** `tone` colours the pin (a CSS class "tone-…"), e.g. an air quality level. */
export type MapPoint = Position & { id: string; label: string; tone?: string };
export type MapLabels = {
  region: string;
  unavailable: string;
  hint: string;
  location: string;
  cluster: (count: number) => string;
};
export function MapView({
  points,
  bounds,
  selectedId,
  position,
  onSelect,
  markerSymbol,
  labels,
}: {
  points: MapPoint[];
  bounds: Bounds;
  selectedId: string | undefined;
  position: Position | null;
  onSelect: (id: string) => void;
  markerSymbol: string;
  labels: MapLabels;
}) {
  const selected = points.find((point) => point.id === selectedId);
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
    instance.fitBounds(bounds);
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
    map.current?.fitBounds(bounds, { padding: [24, 24], animate: false });
  }, [bounds]);

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
          labels.cluster(cluster.getChildCount()),
        );
        return L.divIcon({
          html: content,
          className: "map-cluster",
          iconSize: [40, 40],
        });
      },
    }).addTo(map.current);
    clusters.current = group;
    markers.current.clear();
    for (const place of points) {
      const name = place.label;
      const marker = L.marker([place.latitude, place.longitude], {
        icon: pin(markerSymbol, false, place.tone),
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
  }, [points, markerSymbol, labels.cluster]);

  useEffect(() => {
    for (const [id, marker] of markers.current)
      marker.setIcon(pin(markerSymbol, id === selected?.id, points.find((point) => point.id === id)?.tone));
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
  }, [selected, points, markerSymbol, labels.cluster]);

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
      .bindTooltip(labels.location);
    map.current.setView([position.latitude, position.longitude], 14, {
      animate: false,
    });
    return () => {
      marker.remove();
    };
  }, [position, labels.location]);

  return (
    <div className="map-shell">
      <div
        ref={container}
        className="map-surface"
        role="region"
        aria-label={labels.region}
      />
      {(tileError || config.isError) && (
        <p className="map-notice" role="status">
          {labels.unavailable}
        </p>
      )}
      <span className="map-hint">{labels.hint}</span>
    </div>
  );
}

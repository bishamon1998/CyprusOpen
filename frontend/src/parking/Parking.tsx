import { ParkingResults } from "./ParkingResults";
import { ParkingAttribution } from "./ParkingAttribution";
import { useMemo, useRef, useState } from "react";
import { ParkingFilters } from "./ParkingFilters";
import { useParking } from "../api";
import { ParkingMap } from "./ParkingMap";
import { areas } from "../geo/cyprusAreas";
import "./parking.css";
import { useI18n } from "../i18n";
import { distance } from "../geo/coordinates";
import { useBrowserLocation } from "../hooks/useBrowserLocation";

export default function Parking() {
  const { t } = useI18n();
  const report = useParking();
  const results = useRef<HTMLDivElement>(null);
  const [areaId, setAreaId] = useState("cyprus");
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } =
    useBrowserLocation();
  const area = areas.find((item) => item.id === areaId)!;
  const places = useMemo(() => {
    const query = search.trim().toLowerCase();
    const matches = (report.data?.items || []).filter((place) => {
      const [[south, west], [north, east]] = area.bounds;
      return (
        (area.id === "cyprus" ||
          (place.latitude >= south &&
            place.latitude <= north &&
            place.longitude >= west &&
            place.longitude <= east)) &&
        place.name.toLowerCase().includes(query)
      );
    });
    return matches.sort((a, b) =>
      position
        ? distance(position, a) - distance(position, b)
        : Number(a.name === "Car park") - Number(b.name === "Car park") ||
          a.name.localeCompare(b.name) ||
          a.id.localeCompare(b.id),
    );
  }, [report.data, area, search, position]);
  const selected = places.find((place) => place.id === selectedId);
  const [visibleCount, setVisibleCount] = useState(30);

  function nearMe() {
    locate(() => {
      setAreaId("cyprus");
      setSearch("");
      setSelectedId(null);
      setVisibleCount(30);
    });
  }

  function choose(id: string) {
    setSelectedId(id);
    const index = places.findIndex((place) => place.id === id);
    setVisibleCount((count) => Math.max(count, index + 1));
    results.current?.scrollTo({ top: 0 });
  }

  return (
    <>
      <div className="page-title">
        <h1>{t("parking")}</h1>
      </div>
      <section className="parking-workspace" aria-label={t("findParking")}>
        <ParkingFilters
          areaId={areaId}
          search={search}
          locating={locating}
          onSearch={(value) => {
            setSearch(value);
            setSelectedId(null);
            setVisibleCount(30);
          }}
          onArea={(value) => {
            setAreaId(value);
            setSelectedId(null);
            setVisibleCount(30);
          }}
          onNearMe={nearMe}
        />
        {locationError && (
          <p className="parking-feedback" role="status">
            {t(locationError)}
          </p>
        )}
        <div className="parking-layout">
          <ParkingMap
            places={places}
            area={area}
            selected={selected}
            position={position}
            onSelect={choose}
          />
          <ParkingResults
            report={report}
            places={places}
            selected={selected}
            selectedId={selectedId}
            position={position}
            visibleCount={visibleCount}
            results={results}
            onSelect={choose}
            onMore={() => setVisibleCount((count) => count + 30)}
            onClearLocation={clearLocation}
          />
        </div>
        <ParkingAttribution directory={report.data?.directory} />
      </section>
    </>
  );
}

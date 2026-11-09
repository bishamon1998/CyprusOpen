import { useMemo, useRef, useState } from "react";
import { PageTitle } from "../components/PageTitle";
import { ToolFeedback, ToolLayout, ToolWorkspace } from "../components/tool/Tool";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { UNNAMED_PARKING, useParking } from "./api";
import { ParkingAttribution } from "./ParkingAttribution";
import { ParkingFilters } from "./ParkingFilters";
import { ParkingMap } from "./ParkingMap";
import { ParkingResults } from "./ParkingResults";
import { useParkingName } from "./useParkingName";

const PAGE_SIZE = 30;

export default function Parking() {
  const { t } = useI18n();
  const report = useParking();
  const results = useRef<HTMLDivElement>(null);
  const [areaId, setAreaId] = useState("cyprus");
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const { position, locating, locationError, locate, clearLocation } =
    useBrowserLocation();
  const name = useParkingName();
  const area = areas.find((item) => item.id === areaId)!;
  const places = useMemo(() => {
    const query = search.trim().toLowerCase();
    const [[south, west], [north, east]] = area.bounds;
    // Search the name as shown: unnamed car parks appear under the translated "Car park".
    const matches = (report.data?.items || []).filter(
      (place) =>
        (area.id === "cyprus" ||
          (place.latitude >= south &&
            place.latitude <= north &&
            place.longitude >= west &&
            place.longitude <= east)) &&
        name(place).toLowerCase().includes(query),
    );
    return matches.sort((a, b) =>
      position
        ? distance(position, a) - distance(position, b)
        : Number(a.name === UNNAMED_PARKING) -
            Number(b.name === UNNAMED_PARKING) ||
          a.name.localeCompare(b.name) ||
          a.id.localeCompare(b.id),
    );
  }, [report.data, area, search, position, name]);
  const selected = places.find((place) => place.id === selectedId);

  function resetSelection() {
    setSelectedId(null);
    setVisibleCount(PAGE_SIZE);
  }

  function nearMe() {
    locate(() => {
      setAreaId("cyprus");
      setSearch("");
      resetSelection();
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
      <PageTitle title={t("parking")} path="/parking" />
      <ToolWorkspace label={t("findParking")}>
        <ParkingFilters
          areaId={areaId}
          search={search}
          locating={locating}
          onSearch={(value) => {
            setSearch(value);
            resetSelection();
          }}
          onArea={(value) => {
            setAreaId(value);
            resetSelection();
          }}
          onNearMe={nearMe}
        />
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <ParkingMap
              places={places}
              area={area}
              selected={selected}
              position={position}
              onSelect={choose}
            />
          }
        >
          <ParkingResults
            report={report}
            places={places}
            selected={selected}
            position={position}
            visibleCount={visibleCount}
            results={results}
            onSelect={choose}
            onMore={() => setVisibleCount((count) => count + PAGE_SIZE)}
            onClearLocation={clearLocation}
            onReset={() => {
              setSearch("");
              setAreaId("cyprus");
            }}
          />
        </ToolLayout>
        <ParkingAttribution directory={report.data?.directory} />
      </ToolWorkspace>
    </>
  );
}

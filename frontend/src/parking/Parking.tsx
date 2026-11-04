import { useMemo, useRef, useState } from "react";
import { ArrowUpRight, LocateFixed, Navigation, Search, X } from "lucide-react";
import { type ParkingPlace, useParking } from "../api";
import { areas, ParkingMap } from "./ParkingMap";
import "./parking.css";
import { useI18n } from "../i18n";
import { distance, useParkingLocation } from "./useParkingLocation";

function directions(place: ParkingPlace) {
  return `https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}&travelmode=driving`;
}

export default function Parking() {
  const { t, language } = useI18n();
  const report = useParking();
  const results = useRef<HTMLDivElement>(null);
  const [areaId, setAreaId] = useState("cyprus");
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } =
    useParkingLocation();
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
        <div className="parking-filters">
          <label className="parking-search">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              placeholder={t("search")}
              aria-label={t("search")}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setSelectedId(null);
                setVisibleCount(30);
              }}
            />
          </label>
          <select
            aria-label={t("area")}
            value={areaId}
            onChange={(event) => {
              setAreaId(event.target.value);
              setSelectedId(null);
              setVisibleCount(30);
            }}
          >
            {areas.map((item) => (
              <option key={item.id} value={item.id}>
                {t(item.name)}
              </option>
            ))}
          </select>
          <button className="near-me" onClick={nearMe} disabled={locating}>
            <LocateFixed size={17} aria-hidden="true" />
            {t(locating ? "locating" : "nearMe")}
          </button>
        </div>
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
          <div ref={results} className="parking-results">
            <div className="parking-result-heading">
              <h2>
                {report.isPending
                  ? t("loading")
                  : report.isError && !report.data
                    ? t("carParks")
                    : t("carParkCount", { count: places.length })}
              </h2>
              {position && (
                <button onClick={clearLocation} aria-label={t("clearLocation")}>
                  <X size={15} />
                  {t("nearest")}
                </button>
              )}
            </div>
            <p className="availability-note">{t("availability")}</p>
            {report.isError && (
              <div className="parking-list-empty" role="alert">
                <p>{t("loadError")}</p>
                <button onClick={() => void report.refetch()}>
                  {t("retry")}
                </button>
              </div>
            )}
            {!report.isPending && !report.isError && places.length === 0 && (
              <p className="parking-list-empty">{t("noMatches")}</p>
            )}
            {selected && (
              <div className="selected-parking" aria-live="polite">
                <span>{t("selected")}</span>
                <h3>
                  {selected.name === "Car park" ? t("carPark") : selected.name}
                </h3>
                <a
                  href={directions(selected)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Navigation size={15} aria-hidden="true" />
                  {t("directions")}
                  <ArrowUpRight size={14} aria-hidden="true" />
                </a>
              </div>
            )}
            <ol className="parking-list" aria-label={t("carParks")}>
              {places.slice(0, visibleCount).map((place) => (
                <li key={place.id}>
                  <button
                    className={`parking-row${selectedId === place.id ? " selected" : ""}`}
                    aria-pressed={selectedId === place.id}
                    onClick={() => choose(place.id)}
                  >
                    <span className="parking-row-icon" aria-hidden="true">
                      P
                    </span>
                    <span>
                      <strong>
                        {place.name === "Car park" ? t("carPark") : place.name}
                      </strong>
                      <small>
                        {position
                          ? t("distance", {
                              distance: distance(
                                position,
                                place,
                              ).toLocaleString(language, {
                                minimumFractionDigits: 1,
                                maximumFractionDigits: 1,
                              }),
                            })
                          : `${place.latitude.toFixed(4)}, ${place.longitude.toFixed(4)}`}
                      </small>
                    </span>
                    <ArrowUpRight size={16} aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ol>
            {visibleCount < places.length && (
              <button
                className="show-more"
                onClick={() => setVisibleCount((count) => count + 30)}
              >
                {t("more")}
              </button>
            )}
          </div>
        </div>
        <div className="parking-attribution">
          <a
            href={
              report.data?.directory.url ||
              "https://www.traffic4cyprus.org.cy/en/dataset/parking_areas"
            }
            target="_blank"
            rel="noopener noreferrer"
          >
            <img src="/cynap-logo.png" alt="CYNAP" />
            {t("department")}
          </a>
          <span>
            {t("checked", {
              date: report.data
                ? new Date(
                    report.data.directory.retrievedAt,
                  ).toLocaleDateString(language === "en" ? "en-GB" : language, {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                  })
                : "…",
            })}{" "}
            ·{" "}
            <a
              href="https://creativecommons.org/licenses/by/4.0/"
              target="_blank"
              rel="noopener noreferrer"
            >
              CC BY 4.0
            </a>{" "}
            ·{" "}
            <a
              href="https://www.openstreetmap.org/copyright"
              target="_blank"
              rel="noopener noreferrer"
            >
              {t("osm")}
            </a>
          </span>
        </div>
      </section>
    </>
  );
}

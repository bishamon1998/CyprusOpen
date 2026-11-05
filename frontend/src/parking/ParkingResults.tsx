import { type RefObject } from "react";
import { ArrowUpRight, Navigation, X } from "lucide-react";
import { useI18n } from "../i18n";
import { type ParkingPlace, useParking } from "../api";
import { distance, type Position } from "../geo/coordinates";
import { googleDirections } from "../geo/directions";

export function ParkingResults({
  report,
  places,
  selected,
  selectedId,
  position,
  visibleCount,
  results,
  onSelect,
  onMore,
  onClearLocation,
}: {
  report: ReturnType<typeof useParking>;
  places: ParkingPlace[];
  selected: ParkingPlace | undefined;
  selectedId: string | null;
  position: Position | null;
  visibleCount: number;
  results: RefObject<HTMLDivElement | null>;
  onSelect: (id: string) => void;
  onMore: () => void;
  onClearLocation: () => void;
}) {
  const { t, language } = useI18n();
  return (
    <div ref={results} className="parking-results">
      <div className="parking-result-heading">
        <h2>
          {report.isPending
            ? t("loading")
            : (report.isError && !report.data) ||
                !report.data?.directory.publishedAt
              ? t("carParks")
              : t("carParkCount", { count: places.length })}
        </h2>
        {position && (
          <button onClick={onClearLocation} aria-label={t("clearLocation")}>
            <X size={15} />
            {t("nearest")}
          </button>
        )}
      </div>
      <p className="availability-note">{t("availability")}</p>
      {report.isError && (
        <div className="parking-list-empty" role="alert">
          <p>{t("loadError")}</p>
          <button onClick={() => void report.refetch()}>{t("retry")}</button>
        </div>
      )}
      {!report.isPending && !report.isError && places.length === 0 && (
        <p className="parking-list-empty">
          {t(
            report.data?.directory.publishedAt
              ? "noMatches"
              : "directoryPending",
          )}
        </p>
      )}
      {selected && (
        <div className="selected-parking" aria-live="polite">
          <span>{t("selected")}</span>
          <h3>{selected.name === "Car park" ? t("carPark") : selected.name}</h3>
          <a
            href={googleDirections(selected.latitude + "," + selected.longitude)}
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
              onClick={() => onSelect(place.id)}
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
                        distance: distance(position, place).toLocaleString(
                          language,
                          {
                            minimumFractionDigits: 1,
                            maximumFractionDigits: 1,
                          },
                        ),
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
        <button className="show-more" onClick={onMore}>
          {t("more")}
        </button>
      )}
    </div>
  );
}

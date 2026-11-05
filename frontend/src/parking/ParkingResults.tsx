import { type RefObject } from "react";
import { ArrowUpRight, Navigation, X } from "lucide-react";
import { distance, type Position } from "../geo/coordinates";
import { googleDirections } from "../geo/directions";
import { useI18n } from "../i18n";
import { type ParkingPlace, useParking } from "./api";
import { useParkingName } from "./useParkingName";

export function ParkingResults({
  report,
  places,
  selected,
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
  position: Position | null;
  visibleCount: number;
  results: RefObject<HTMLDivElement | null>;
  onSelect: (id: string) => void;
  onMore: () => void;
  onClearLocation: () => void;
}) {
  const { t, language } = useI18n();
  const name = useParkingName();
  const published = Boolean(report.data?.directory.publishedAt);
  const heading = report.isPending
    ? t("loading")
    : (report.isError && !report.data) || !published
      ? t("carParks")
      : t("carParkCount", { count: places.length });
  const detail = (place: ParkingPlace) =>
    position
      ? t("distance", {
          distance: distance(position, place).toLocaleString(language, {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1,
          }),
        })
      : `${place.latitude.toFixed(4)}, ${place.longitude.toFixed(4)}`;

  return (
    <div ref={results} className="parking-results">
      <div className="parking-result-heading">
        <h2>{heading}</h2>
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
          {t(published ? "noMatches" : "directoryPending")}
        </p>
      )}
      {selected && (
        <div className="selected-parking" aria-live="polite">
          <span>{t("selected")}</span>
          <h3>{name(selected)}</h3>
          <a
            href={googleDirections(`${selected.latitude},${selected.longitude}`)}
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
        {places.slice(0, visibleCount).map((place) => {
          const isSelected = selected?.id === place.id;
          return (
            <li key={place.id}>
              <button
                className={`parking-row${isSelected ? " selected" : ""}`}
                aria-pressed={isSelected}
                onClick={() => onSelect(place.id)}
              >
                <span className="parking-row-icon" aria-hidden="true">
                  P
                </span>
                <span>
                  <strong>{name(place)}</strong>
                  <small>{detail(place)}</small>
                </span>
                <ArrowUpRight size={16} aria-hidden="true" />
              </button>
            </li>
          );
        })}
      </ol>
      {visibleCount < places.length && (
        <button className="show-more" onClick={onMore}>
          {t("more")}
        </button>
      )}
    </div>
  );
}

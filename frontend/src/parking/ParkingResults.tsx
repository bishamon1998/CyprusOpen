import { type RefObject } from "react";
import { ArrowUpRight, Navigation } from "lucide-react";
import {
  NearestFirst,
  ResultRow,
  ResultsMessage,
  ResultsPanel,
  SelectedResult,
  ShowMore,
} from "../components/tool/Tool";
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
    <ResultsPanel
      panelRef={results}
      heading={heading}
      action={position && <NearestFirst onClear={onClearLocation} />}
      notes={[t("availability")]}
    >
      {report.isError && (
        <ResultsMessage onRetry={() => void report.refetch()}>{t("loadError")}</ResultsMessage>
      )}
      {!report.isPending && !report.isError && places.length === 0 && (
        <ResultsMessage>{t(published ? "noMatches" : "directoryPending")}</ResultsMessage>
      )}
      {selected && (
        <SelectedResult label={t("selected")} title={name(selected)}>
          <a
            href={googleDirections(`${selected.latitude},${selected.longitude}`)}
            target="_blank"
            rel="noopener noreferrer"
          >
            <Navigation size={15} aria-hidden="true" />
            {t("directions")}
            <ArrowUpRight size={14} aria-hidden="true" />
          </a>
        </SelectedResult>
      )}
      <ol className="result-list" aria-label={t("carParks")}>
        {places.slice(0, visibleCount).map((place) => (
          <ResultRow
            key={place.id}
            icon="P"
            title={name(place)}
            details={[detail(place)]}
            selected={selected?.id === place.id}
            onSelect={() => onSelect(place.id)}
          />
        ))}
      </ol>
      {visibleCount < places.length && <ShowMore onClick={onMore} />}
    </ResultsPanel>
  );
}

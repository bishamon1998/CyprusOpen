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
import { useDistanceLabel } from "../format";
import { Glyph } from "../components/icons/SoftTile";
import { distance, type Position } from "../geo/coordinates";
import { townOf } from "../geo/cyprusAreas";
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
  onReset,
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
  onReset: () => void;
}) {
  const { t } = useI18n();
  const name = useParkingName();
  const distanceLabel = useDistanceLabel();
  const published = Boolean(report.data?.directory.publishedAt);
  const heading = report.isPending
    ? t("loading")
    : (report.isError && !report.data) || !published
      ? t("carParks")
      : t("carParkCount", { count: places.length });
  // The town says more to people than coordinates; the distance follows once the location is known.
  const detail = (place: ParkingPlace) => {
    const { town, inside } = townOf(place);
    const where = inside ? t(town) : t("aroundArea", { town: t(town) });
    return position ? `${where} · ${distanceLabel(distance(position, place))}` : where;
  };

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
        published ? (
          <ResultsMessage kind="empty" onReset={onReset}>{t("noMatches")}</ResultsMessage>
        ) : (
          <ResultsMessage kind="pending">{t("directoryPending")}</ResultsMessage>
        )
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
            icon={<Glyph name="parking" />}
            tone="parking-tile"
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

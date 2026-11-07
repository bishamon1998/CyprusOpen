import { useMemo, useRef, useState } from "react";
import { Pill } from "lucide-react";
import { MapView } from "../components/map/MapView";
import { useMapLabels } from "../components/map/useMapLabels";
import { PageTitle } from "../components/PageTitle";
import {
  ExternalLink,
  SourceAttribution,
  ToolFeedback,
  ToolLayout,
  ToolWorkspace,
} from "../components/tool/Tool";
import { useCyprusDate } from "../format";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { ccByLicenseUrl, osmCopyrightUrl, pharmacyScheduleUrl } from "../sources";
import { type Pharmacy, usePharmacySchedule } from "./api";
import { PharmacyFilters } from "./PharmacyFilters";
import { PharmacyResults } from "./PharmacyResults";

export default function Pharmacies() {
  const { t } = useI18n();
  const date = useCyprusDate();
  const results = useRef<HTMLDivElement>(null);
  const [scheduleDate, setScheduleDate] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const schedule = usePharmacySchedule(scheduleDate);
  const items = useMemo(() => schedule.data?.items ?? [], [schedule.data]);

  const distanceTo = (pharmacy: Pharmacy) =>
    position && pharmacy.location ? distance(position, pharmacy.location) : null;
  const pharmacies = position
    ? [...items].sort((a, b) => (distanceTo(a) ?? Infinity) - (distanceTo(b) ?? Infinity))
    : items;
  const selected = items.find((pharmacy) => pharmacy.id === selectedId);
  const points = useMemo(
    () =>
      items.flatMap((pharmacy) =>
        pharmacy.location ? [{ ...pharmacy.location, id: pharmacy.id, label: pharmacy.name }] : [],
      ),
    [items],
  );
  const labels = useMapLabels("pharmacies", "pharmacyCount");

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  return (
    <>
      <PageTitle title={t("pharmacies")} />
      <ToolWorkspace label={t("dutyPharmacies")}>
        <PharmacyFilters
          date={scheduleDate}
          scheduleDate={schedule.data?.date}
          locating={locating}
          onDate={(value) => {
            setScheduleDate(value);
            setSelectedId(null);
          }}
          onNearMe={() => locate(() => setSelectedId(null))}
        />
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <MapView
              points={points}
              bounds={areas[0].bounds}
              selectedId={selectedId ?? undefined}
              position={position}
              onSelect={choose}
              markerSymbol="+"
              labels={labels}
            />
          }
        >
          <PharmacyResults
            schedule={schedule}
            pharmacies={pharmacies}
            mapped={points.length}
            selected={selected}
            sortedByDistance={Boolean(position)}
            distanceTo={distanceTo}
            results={results}
            onSelect={choose}
            onClearLocation={clearLocation}
          />
        </ToolLayout>
        <SourceAttribution
          href={schedule.data?.source ?? pharmacyScheduleUrl}
          badge={<Pill size={20} aria-hidden="true" />}
          name={t("pharmaceuticalServices")}
          facts={[
            t("scheduleChecked", { date: date(schedule.data?.checkedAt) }),
            <ExternalLink href={ccByLicenseUrl}>CC BY 4.0</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

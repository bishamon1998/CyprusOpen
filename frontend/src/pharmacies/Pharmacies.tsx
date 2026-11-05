import { useMemo, useState } from "react";
import { MapView, type MapLabels } from "../components/map/MapView";
import { PageTitle } from "../components/PageTitle";
import { distance } from "../geo/coordinates";
import { areas } from "../geo/cyprusAreas";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type Pharmacy, usePharmacySchedule } from "./api";
import { PharmacyList, pharmacyElementId } from "./PharmacyList";
import { PharmacyToolbar } from "./PharmacyToolbar";
import "./pharmacies.css";

export default function Pharmacies() {
  const { t, language } = useI18n();
  const [date, setDate] = useState("");
  const [selectedId, setSelectedId] = useState<string>();
  const location = useBrowserLocation();
  const schedule = usePharmacySchedule(date);
  const items = schedule.data?.items ?? [];

  const points = useMemo(
    () =>
      items.flatMap((pharmacy) =>
        pharmacy.location
          ? [{ ...pharmacy.location, id: pharmacy.id, label: pharmacy.name }]
          : [],
      ),
    [items],
  );
  const distanceTo = (pharmacy: Pharmacy) =>
    location.position && pharmacy.location
      ? distance(location.position, pharmacy.location)
      : null;
  const results = location.position
    ? [...items].sort(
        (a, b) => (distanceTo(a) ?? Infinity) - (distanceTo(b) ?? Infinity),
      )
    : items;
  const labels = useMemo<MapLabels>(
    () => ({
      region: t("pharmacies"),
      unavailable: t("tilesUnavailable"),
      hint: t("mapHint"),
      location: t("yourLocation"),
      cluster: (count) => `${count} ${t("pharmacies")}`,
    }),
    [t],
  );
  const status = schedule.data?.items.length
    ? schedule.data.onCall
      ? "pharmacyOnCall"
      : "dutyHoursUnconfirmed"
    : "scheduleUnavailable";

  return (
    <>
      <PageTitle title={t("pharmacies")} />
      <section className="tool-panel pharmacy-panel">
        <PharmacyToolbar
          date={date}
          scheduleDate={schedule.data?.date}
          source={schedule.data?.source}
          canLocate={points.length > 0}
          locating={location.locating}
          onDate={setDate}
          onNearMe={() => location.locate(() => setSelectedId(undefined))}
        />
        {schedule.isPending && <p role="status">{t("loading")}</p>}
        {location.locationError && (
          <p role="status">{t(location.locationError)}</p>
        )}
        {schedule.isError && (
          <div role="alert">
            <p>{t("pharmacyLoadError")}</p>
            <button onClick={() => void schedule.refetch()}>
              {t("retry")}
            </button>
          </div>
        )}
        {schedule.data && (
          <>
            <p>{t(status)}</p>
            {schedule.data.checkedAt && (
              <p>
                {t("pharmacyChecked", {
                  time: new Date(schedule.data.checkedAt).toLocaleString(
                    language,
                    {
                      timeZone: "Asia/Nicosia",
                      dateStyle: "medium",
                      timeStyle: "short",
                    },
                  ),
                })}
              </p>
            )}
            {items.length > points.length && (
              <p>
                {t("pharmacyMapCoverage", {
                  mapped: points.length,
                  total: items.length,
                })}
              </p>
            )}
            {points.length > 0 && (
              <MapView
                points={points}
                bounds={areas[0].bounds}
                selectedId={selectedId}
                position={location.position}
                onSelect={(id) => {
                  setSelectedId(id);
                  document
                    .getElementById(pharmacyElementId(id))
                    ?.scrollIntoView({ block: "nearest" });
                }}
                markerSymbol="+"
                labels={labels}
              />
            )}
            <PharmacyList pharmacies={results} distanceTo={distanceTo} />
          </>
        )}
      </section>
    </>
  );
}

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigation, Phone } from "lucide-react";
import { get } from "../api/http";
import { MapView } from "../components/map/MapView";
import { areas } from "../geo/cyprusAreas";
import { distance, type Position } from "../geo/coordinates";
import { googleDirections } from "../geo/directions";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import "./pharmacies.css";

type Entry = {
  district: string;
  registrationNumber: string;
  name: string;
  address: string;
  municipality: string;
  phone: string;
};

type Schedule = { date: string; source: string; checkedAt: string | null; onCall: boolean; items: Entry[]; locations: Record<string, Position> };
const entryId = (entry: Entry) => entry.district + ":" + entry.registrationNumber;
const officialSchedule =
  "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";

export default function Pharmacies() {
  const { t, language } = useI18n();
  const [date, setDate] = useState("");
  const location = useBrowserLocation();
  const [selectedId, setSelectedId] = useState<string>();
  const schedule = useQuery({
    queryKey: ["pharmacies", date],
    refetchInterval: date ? false : 30_000,
    refetchOnWindowFocus: !date,
    queryFn: () =>
      get<Schedule>("/api/v1/pharmacies" + (date ? "?date=" + encodeURIComponent(date) : "")),
  });

  const positions = schedule.data?.locations ?? {};
  const items = schedule.data?.items ?? [];
  const points = items.flatMap((entry) => {
    const id = entryId(entry);
    return positions[id] ? [{ ...positions[id], id, label: entry.name }] : [];
  });
  const distanceTo = (entry: Entry) => {
    const point = positions[entryId(entry)];
    return location.position && point ? distance(location.position, point) : Infinity;
  };
  const results = location.position
    ? [...items].sort((a, b) => distanceTo(a) - distanceTo(b) || 0)
    : items;
  return (
    <>
      <div className="page-title">
        <h1>{t("pharmacies")}</h1>
      </div>
      <section className="tool-panel pharmacy-panel">
        <div className="pharmacy-toolbar">
          <label>
            {t("scheduleDate")}
            <input type="date" value={date || schedule.data?.date || ""}
              onChange={(event) => setDate(event.target.value)} />
          </label>
          {date && <button onClick={() => setDate("")}>{t("pharmacyCurrent")}</button>}
          {!date && points.length > 0 && (
            <button onClick={() => location.locate(() => setSelectedId(undefined))}
              disabled={location.locating}>{t("nearMe")}</button>
          )}
          <a href={schedule.data?.source ?? officialSchedule} target="_blank" rel="noreferrer">
            {t("source")}
          </a>
        </div>
        {schedule.isPending && <p role="status">{t("loading")}</p>}
        {location.locationError && <p role="status">{t(location.locationError)}</p>}
        {schedule.isError && (
          <div role="alert">
            <p>{t("loadError")}</p>
            <button onClick={() => void schedule.refetch()}>{t("retry")}</button>
          </div>
        )}
        {schedule.data && (
          <>
            <p>{t(schedule.data.items.length ? (schedule.data.onCall ? "pharmacyOnCall" : "dutyHoursUnconfirmed") : "scheduleUnavailable")}</p>
            {schedule.data.checkedAt && <p>{t("pharmacyChecked", { time: new Date(schedule.data.checkedAt).toLocaleString(language, { timeZone: "Asia/Nicosia", dateStyle: "medium", timeStyle: "short" }) })}</p>}
            {items.length > points.length && <p>{t("pharmacyMapCoverage", { mapped: points.length, total: items.length })}</p>}
            {points.length > 0 && (
              <MapView points={points} bounds={areas[0].bounds} selectedId={selectedId}
                position={location.position} onSelect={(id) => {
                  setSelectedId(id);
                  document.getElementById("pharmacy-" + id)?.scrollIntoView({ block: "nearest" });
                }} markerSymbol="+" labels={{
                  region: t("pharmacies"), unavailable: t("tilesUnavailable"),
                  hint: t("mapHint"), location: t("yourLocation"),
                  cluster: (count) => count + " " + t("pharmacies"),
                }} />
            )}
            <ul className="pharmacy-list">
              {results.map((entry) => {
                const address = [entry.address, entry.municipality].filter(Boolean).join(", ");
                const phone = entry.phone.replace(/\D/g, "");
                return (
                  <li key={entryId(entry)} id={"pharmacy-" + entryId(entry)}>
                    <h2 lang="el">{entry.name}</h2>
                    <p lang="el">{address}</p>
                    {Number.isFinite(distanceTo(entry)) && (
                      <p>{t("distance", { distance: distanceTo(entry).toLocaleString(language, { maximumFractionDigits: 1 }) })}</p>
                    )}
                    <div className="pharmacy-actions">
                      {/^[0-9]{8}$/.test(phone) && (
                        <a href={"tel:+357" + phone}><Phone size={16} aria-hidden="true" />{phone}</a>
                      )}
                      <a href={googleDirections(address + ", Cyprus")} target="_blank" rel="noreferrer">
                        <Navigation size={16} aria-hidden="true" />{t("directions")}
                      </a>
                    </div>
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </section>
    </>
  );
}

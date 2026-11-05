import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigation, Phone } from "lucide-react";
import { get } from "../api/http";
import { googleDirections } from "../geo/directions";
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

type Schedule = { date: string; source: string; items: Entry[] };
const officialSchedule =
  "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";

export default function Pharmacies() {
  const { t } = useI18n();
  const [date, setDate] = useState("");
  const schedule = useQuery({
    queryKey: ["pharmacies", date],
    queryFn: () =>
      get<Schedule>("/api/v1/pharmacies" + (date ? "?date=" + encodeURIComponent(date) : "")),
  });

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
          <a href={schedule.data?.source ?? officialSchedule} target="_blank" rel="noreferrer">
            {t("source")}
          </a>
        </div>
        {schedule.isPending && <p role="status">{t("loading")}</p>}
        {schedule.isError && (
          <div role="alert">
            <p>{t("loadError")}</p>
            <button onClick={() => void schedule.refetch()}>{t("retry")}</button>
          </div>
        )}
        {schedule.data && (
          <>
            <p>{t(schedule.data.items.length ? "dutyHoursUnconfirmed" : "scheduleUnavailable")}</p>
            <ul className="pharmacy-list">
              {schedule.data.items.map((entry) => {
                const address = [entry.address, entry.municipality].filter(Boolean).join(", ");
                const phone = entry.phone.replace(/\D/g, "");
                return (
                  <li key={entry.district + ":" + entry.registrationNumber}>
                    <h2 lang="el">{entry.name}</h2>
                    <p lang="el">{address}</p>
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

import { useState } from "react";
import { InfoPopover } from "../components/InfoPopover";
import { SegmentedControl } from "../components/SegmentedControl";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { bankHolidaysUrl } from "../sources";
import { cyprusToday, type Day, daysBetween, holidays, orthodoxEaster, weekday } from "./holidayDates";

/** This year's and next year's bank and public service holidays, with the next one highlighted. */
export function Holidays() {
  const { t, language } = useI18n();
  const today = cyprusToday();
  const [year, setYear] = useState(today.year);
  const list = holidays(year);
  const next = holidays(today.year)
    .concat(holidays(today.year + 1))
    .find((holiday) => daysBetween(today, holiday.date) >= 0);
  const weekdaysOff = list.filter((holiday) => ![0, 6].includes(weekday(holiday.date))).length;

  const format = (date: Day, options: Intl.DateTimeFormatOptions) =>
    new Date(Date.UTC(date.year, date.month - 1, date.day)).toLocaleDateString(language, {
      ...options,
      timeZone: "UTC",
    });
  const until = (date: Day) => {
    const days = daysBetween(today, date);
    return new Intl.RelativeTimeFormat(language, { numeric: "auto" }).format(days, "day");
  };

  return (
    <article className="dataset-card tool-card" aria-labelledby="holidays-title">
      <div className="card-heading">
        <h3 id="holidays-title">{t("holidays")}</h3>
      </div>
      <p className="card-description">{t("holidaysDescription")}</p>
      {next && (
        <p className="holiday-next">
          <span>{t("nextHoliday")}</span>
          <strong>{t(next.name)}</strong>
          <span>
            {format(next.date, { weekday: "long", day: "numeric", month: "long" })}, {until(next.date)}
          </span>
        </p>
      )}
      <div className="chart-controls">
        <SegmentedControl
          label={t("holidayYear")}
          value={String(year)}
          options={[today.year, today.year + 1].map((item) => ({ value: String(item), label: String(item) }))}
          onChange={(value) => setYear(Number(value))}
        />
      </div>
      <ol className="holiday-list" aria-label={t("holidaysOf", { year })}>
        {list.map((holiday) => {
          const weekend = [0, 6].includes(weekday(holiday.date));
          const past = year === today.year && daysBetween(today, holiday.date) < 0;
          return (
            <li key={holiday.name} className={(weekend ? "is-weekend" : "") + (past ? " is-past" : "")}>
              <time dateTime={`${holiday.date.year}-${pad(holiday.date.month)}-${pad(holiday.date.day)}`}>
                <span>{format(holiday.date, { day: "numeric", month: "short" })}</span>
                <small>{format(holiday.date, { weekday: "short" })}</small>
              </time>
              <span className="holiday-name">
                {t(holiday.name)}
                {weekend && <small>{t("onWeekend")}</small>}
              </span>
            </li>
          );
        })}
      </ol>
      <p className="tool-card-summary">
        {t("holidaysSummary", {
          count: weekdaysOff,
          easter: format(orthodoxEaster(year), { day: "numeric", month: "long" }),
        })}
      </p>
      <div className="info-bar">
        <InfoPopover>
          <p>{t("holidaysSourceNote")}</p>
          <p>{t("holidaysEmployerNote")}</p>
          <SourceLink href={bankHolidaysUrl}>{t("centralBank")}</SourceLink>
        </InfoPopover>
      </div>
    </article>
  );
}

const pad = (value: number) => String(value).padStart(2, "0");

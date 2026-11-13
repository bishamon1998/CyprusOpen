import { useCallback, useMemo, useRef, useState } from "react";
import { ArrowUpRight, CalendarCheck, Landmark, Navigation, Phone, Search } from "lucide-react";
import { MapView } from "../components/map/MapView";
import { useMapLabels } from "../components/map/useMapLabels";
import { PageTitle } from "../components/PageTitle";
import {
  ExternalLink,
  NearestFirst,
  NearMeButton,
  ResultRow,
  ResultsMessage,
  ResultsPanel,
  SelectedResult,
  SourceAttribution,
  ToolFeedback,
  ToolFilters,
  ToolLayout,
  ToolWorkspace,
} from "../components/tool/Tool";
import { useCyprusDate, useCyprusDateTime, useDistanceLabel } from "../format";
import { distance } from "../geo/coordinates";
import { areas, townOf } from "../geo/cyprusAreas";
import { googleDirections } from "../geo/directions";
import { useBrowserLocation } from "../hooks/useBrowserLocation";
import { useI18n } from "../i18n";
import { type MessageKey } from "../locales";
import { ccByLicenseUrl, hospitalsSourceUrl, osmCopyrightUrl, serviceCentresSourceUrl } from "../sources";
import { type Kind, kinds, type ServicePlace, useServices } from "./api";
import "./services.css";

const kindLabels: Record<Kind, MessageKey> = { hospital: "stateHospital", centre: "serviceCentre" };
const filterLabels: Record<Kind, MessageKey> = { hospital: "stateHospitals", centre: "serviceCentres" };
/** Marker and list symbols; the colour comes from the "tone-…" class. */
const symbols: Record<Kind, string> = { hospital: "H", centre: "■" };
const LOCAL_PHONE = /^[0-9]{8}$/;

export default function Services() {
  const { t, language } = useI18n();
  const results = useRef<HTMLDivElement>(null);
  const [search, setSearch] = useState("");
  const [kind, setKind] = useState<Kind | "all">("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { position, locating, locationError, locate, clearLocation } = useBrowserLocation();
  const query = useServices();
  const places = useMemo(() => query.data?.places ?? [], [query.data]);
  const distanceLabel = useDistanceLabel();
  const date = useCyprusDate();
  const dateTime = useCyprusDateTime();

  // The files are Greek only, so the translated town is what a visitor can search and read in their language.
  const town = useCallback(
    (place: ServicePlace) => {
      const { town, inside } = townOf(place);
      return inside ? t(town) : t("aroundArea", { town: t(town) });
    },
    [t],
  );
  const needle = search.trim().toLocaleLowerCase();
  const matching = useMemo(
    () =>
      places.filter(
        (place) =>
          (kind === "all" || place.kind === kind) &&
          (!needle ||
            [place.name, place.address ?? "", town(place)].some((text) =>
              text.toLocaleLowerCase().includes(needle),
            )),
      ),
    [places, kind, needle, town],
  );
  const sorted = position
    ? [...matching].sort((a, b) => distance(position, a) - distance(position, b))
    : matching;
  const selected = places.find((place) => place.id === selectedId);

  const points = useMemo(
    () =>
      matching.map((place) => ({
        id: place.id,
        label: place.name,
        latitude: place.latitude,
        longitude: place.longitude,
        tone: place.kind,
        symbol: symbols[place.kind],
      })),
    [matching],
  );
  const labels = useMapLabels("servicesMap", "servicesCount");

  const weekday = (day: number) =>
    // 1 January 2024 was a Monday, so ISO day d is 2024-01-0d.
    new Date(Date.UTC(2024, 0, day)).toLocaleDateString(language, { weekday: "short", timeZone: "UTC" });
  const hours = (place: ServicePlace) => {
    if (!place.hours) return null;
    const { days, opens, closes } = place.hours;
    const range = days.length === 1 ? weekday(days[0]) : `${weekday(days[0])}–${weekday(days[days.length - 1])}`;
    return t("openingHours", { days: range, opens, closes });
  };

  function choose(id: string) {
    setSelectedId(id);
    results.current?.scrollTo({ top: 0 });
  }

  const detail = (place: ServicePlace) =>
    [t(kindLabels[place.kind]), town(place), position ? distanceLabel(distance(position, place)) : null]
      .filter(Boolean)
      .join(", ");
  const phone = selected?.phone?.split(/[;,]/)[0].replace(/\s/g, "") ?? "";
  const sources = query.data?.sources;

  return (
    <>
      <PageTitle title={t("services")} path="/services" />
      <ToolWorkspace label={t("services")}>
        <ToolFilters>
          <label className="tool-search">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              placeholder={t("searchServices")}
              aria-label={t("searchServices")}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <select
            aria-label={t("serviceKind")}
            value={kind}
            onChange={(event) => {
              setKind(event.target.value as Kind | "all");
              setSelectedId(null);
            }}
          >
            <option value="all">{t("allServices")}</option>
            {kinds.map((item) => (
              <option key={item} value={item}>
                {t(filterLabels[item])}
              </option>
            ))}
          </select>
          <NearMeButton locating={locating} onClick={() => locate(() => setSelectedId(null))} />
        </ToolFilters>
        {locationError && <ToolFeedback>{t(locationError)}</ToolFeedback>}
        <ToolLayout
          map={
            <MapView
              points={points}
              bounds={areas[0].bounds}
              selectedId={selectedId ?? undefined}
              position={position}
              onSelect={choose}
              markerSymbol={symbols.hospital}
              labels={labels}
            />
          }
        >
          <ResultsPanel
            panelRef={results}
            heading={query.isPending ? t("loading") : t("servicesCount", { count: matching.length })}
            action={position && <NearestFirst onClear={clearLocation} />}
            notes={[t("servicesNote")]}
          >
            {query.isError && (
              <ResultsMessage onRetry={() => void query.refetch()}>{t("servicesLoadError")}</ResultsMessage>
            )}
            {query.data && places.length === 0 && (
              <ResultsMessage kind="pending">{t("servicesUnavailable")}</ResultsMessage>
            )}
            {query.data && places.length > 0 && matching.length === 0 && (
              <ResultsMessage
                kind="empty"
                onReset={() => {
                  setSearch("");
                  setKind("all");
                }}
              >
                {t("servicesNoMatches")}
              </ResultsMessage>
            )}
            {selected && (
              <SelectedResult
                label={t(kindLabels[selected.kind])}
                title={selected.name}
                detail={[selected.address, hours(selected) ?? selected.hoursText].filter(Boolean).join(". ") || undefined}
                lang="el"
              >
                {LOCAL_PHONE.test(phone) && (
                  <a href={"tel:+357" + phone}>
                    <Phone size={15} aria-hidden="true" />
                    {phone}
                  </a>
                )}
                {selected.appointmentUrl && (
                  <a href={selected.appointmentUrl} target="_blank" rel="noopener noreferrer">
                    <CalendarCheck size={15} aria-hidden="true" />
                    {t("bookAppointment")}
                    <ArrowUpRight size={14} aria-hidden="true" />
                  </a>
                )}
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
            <ol className="result-list" aria-label={t("servicesPlaces")}>
              {sorted.map((place) => (
                <ResultRow
                  key={place.id}
                  icon={symbols[place.kind]}
                  tone={place.kind}
                  title={place.name}
                  lang="el"
                  details={[detail(place), hours(place)]}
                  selected={selected?.id === place.id}
                  onSelect={() => choose(place.id)}
                />
              ))}
            </ol>
          </ResultsPanel>
        </ToolLayout>
        <SourceAttribution
          href={sources?.hospitals.url ?? hospitalsSourceUrl}
          badge={<Landmark size={20} aria-hidden="true" />}
          name={t("healthMinistry")}
          facts={[
            t("hospitalsDataFrom", { date: date(sources?.hospitals.publishedAt) }),
            <ExternalLink href={sources?.centres.url ?? serviceCentresSourceUrl}>{t("publicAdministration")}</ExternalLink>,
            t("centresDataFrom", { date: date(sources?.centres.publishedAt) }),
            t("lastChecked", { date: dateTime(sources?.hospitals.retrievedAt) }),
            <ExternalLink href={ccByLicenseUrl}>CC BY 4.0</ExternalLink>,
            <ExternalLink href={osmCopyrightUrl}>{t("osm")}</ExternalLink>,
          ]}
        />
      </ToolWorkspace>
    </>
  );
}

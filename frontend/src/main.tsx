import React, { lazy, Suspense, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  BrowserRouter,
  Link,
  NavLink,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import {
  ArrowUpRight,
  ChartColumn,
  Database,
  Globe2,
  LocateFixed,
  MapPin,
  ParkingCircle,
  Pill,
  RefreshCw,
  Users,
} from "lucide-react";
import { catalogUrl, useDatasets } from "./api";
import { I18nProvider, useI18n } from "./i18n";
import { type Language, type MessageKey } from "./locales";
import "./styles.css";

const parkingSource =
  "https://www.data.gov.cy/el/dataset/trehoysa-diathesimotita-horon-stathmeysis-dimoy-leykosias";
const pharmacySource =
  "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";
const Parking = lazy(() => import("./parking/Parking"));
const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30000, refetchOnWindowFocus: false },
  },
});

function SourceLink({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) {
  return (
    <a
      className="source-link"
      href={href}
      target="_blank"
      rel="noopener noreferrer"
    >
      {children}
      <ArrowUpRight size={15} aria-hidden="true" />
    </a>
  );
}

function App() {
  const location = useLocation();
  const { language, setLanguage, t } = useI18n();
  useEffect(() => {
    const titles: Record<string, MessageKey> = {
      "/": "statistics",
      "/parking": "parking",
      "/pharmacies": "pharmacies",
      "/about": "sources",
    };
    document.title = `${t(titles[location.pathname] || "notFound")} · Cyprus Open`;
  }, [location.pathname, language]);
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);
  return (
    <>
      <a className="skip-link" href="#main">
        {t("skip")}
      </a>
      <header className="site-header">
        <div className="wrap header-inner">
          <Link to="/" className="wordmark" aria-label={t("home")}>
            <img className="mouflon-mark" src="/mouflon.svg" alt="" />
            cyprus<span className="wordmark-light">open</span>
          </Link>
          <nav aria-label={t("navigation")}>
            <NavLink to="/" end>
              <ChartColumn size={17} aria-hidden="true" />
              <span>{t("statistics")}</span>
            </NavLink>
            <NavLink to="/parking">
              <ParkingCircle size={18} aria-hidden="true" />
              <span>{t("parking")}</span>
            </NavLink>
            <NavLink to="/pharmacies">
              <Pill size={17} aria-hidden="true" />
              <span>{t("pharmacies")}</span>
            </NavLink>
            <NavLink to="/about">
              <Database size={16} aria-hidden="true" />
              <span>{t("sources")}</span>
            </NavLink>
          </nav>
          <select
            id="language"
            className="language-select"
            aria-label={t("language")}
            value={language}
            onChange={(event) => setLanguage(event.target.value as Language)}
          >
            <option value="en">EN</option>
            <option value="ru">RU</option>
            <option value="el">EL</option>
          </select>
        </div>
      </header>
      <main id="main" className="wrap">
        <Suspense fallback={<p className="route-loading">{t("loading")}</p>}>
          <Routes>
            <Route path="/" element={<Statistics />} />
            <Route path="/parking" element={<Parking />} />
            <Route path="/pharmacies" element={<Pharmacies />} />
            <Route path="/about" element={<Sources />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
    </>
  );
}

function Illustration({ kind }: { kind: "people" | "travel" | "pharmacy" }) {
  const MainIcon = { people: Users, travel: Globe2, pharmacy: Pill }[kind];
  const SmallIcon =
    kind === "pharmacy" ? LocateFixed : kind === "people" ? Globe2 : MapPin;
  return (
    <div className={`illustration ${kind}`} aria-hidden="true">
      <span className="orbit orbit-one" />
      <span className="orbit orbit-two" />
      <span className="illustration-line" />
      <span className="illustration-dot dot-one" />
      <span className="illustration-dot dot-two" />
      <span className="icon-tile main-tile">
        <MainIcon size={43} strokeWidth={1.4} />
      </span>
      <span className="icon-tile small-tile">
        <SmallIcon size={19} strokeWidth={1.6} />
      </span>
      <span className="illustration-spark">+</span>
    </div>
  );
}

function Statistics() {
  const datasets = useDatasets();
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("statistics")} />
      {datasets.isError && (
        <div className="error-banner" role="alert">
          <span>{t("statisticsError")}</span>
          <button
            onClick={() => void datasets.refetch()}
            disabled={datasets.isFetching}
          >
            <RefreshCw size={15} aria-hidden="true" />
            {t("retry")}
          </button>
        </div>
      )}
      <section className="dataset-grid" aria-label={t("datasets")}>
        <DatasetCard
          kind="people"
          title={t("immigration")}
          description={t("immigrationDescription")}
          loading={datasets.isPending}
        />
        <DatasetCard
          kind="travel"
          title={t("tourism")}
          description={t("tourismDescription")}
          loading={datasets.isPending}
        />
      </section>
    </>
  );
}

function DatasetCard({
  kind,
  title,
  description,
  loading,
}: {
  kind: "people" | "travel";
  title: string;
  description: string;
  loading: boolean;
}) {
  const { t } = useI18n();
  return (
    <article className="dataset-card">
      <div className="card-heading">
        <h2>{title}</h2>
      </div>
      <p className="card-description">{description}</p>
      <div className="chart-empty" aria-live="polite" aria-busy={loading}>
        <Illustration kind={kind} />
        <p>{t(loading ? "loading" : "noData")}</p>
      </div>
      <div className="card-footer">
        <SourceLink href={catalogUrl}>{t("source")}</SourceLink>
      </div>
    </article>
  );
}

function Pharmacies() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("pharmacies")} />
      <section className="tool-panel">
        <div className="tool-empty">
          <Illustration kind="pharmacy" />
          <h2>{t("pharmacySoon")}</h2>
          <p>{t("pharmacyDescription")}</p>
        </div>
        <div className="card-footer">
          <SourceLink href={pharmacySource}>{t("source")}</SourceLink>
        </div>
      </section>
    </>
  );
}

function Sources() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("sources")} />
      <section className="sources-list" aria-label={t("dataSources")}>
        <SourceRow
          title={t("statistics")}
          description={t("statisticsSource")}
          href={catalogUrl}
          icon={<ChartColumn size={21} />}
        />
        <SourceRow
          title={t("parkingLocations")}
          description={t("directorySource")}
          href="https://www.traffic4cyprus.org.cy/en/dataset/parking_areas"
          icon={<MapPin size={21} />}
        />
        <SourceRow
          title={t("nicosiaParking")}
          description={t("nicosiaSource")}
          href={parkingSource}
          icon={<ParkingCircle size={23} />}
        />
        <SourceRow
          title={t("dutyPharmacies")}
          description={t("pharmacySource")}
          href={pharmacySource}
          icon={<Pill size={22} />}
        />
      </section>
    </>
  );
}

function SourceRow({
  title,
  description,
  href,
  icon,
}: {
  title: string;
  description: string;
  href: string;
  icon: React.ReactNode;
}) {
  const { t } = useI18n();
  return (
    <div className="source-row">
      <span className="quick-icon" aria-hidden="true">
        {icon}
      </span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <SourceLink href={href}>{t("open")}</SourceLink>
    </div>
  );
}

function PageTitle({ title }: { title: string }) {
  return (
    <div className="page-title">
      <h1>{title}</h1>
    </div>
  );
}
function NotFound() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("notFound")} />
      <Link className="back-link" to="/">
        {t("back")}
      </Link>
    </>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <I18nProvider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </I18nProvider>
  </React.StrictMode>,
);

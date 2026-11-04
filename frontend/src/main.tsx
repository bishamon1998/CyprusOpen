import React, { useEffect } from "react";
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
  Clock3,
  Database,
  Globe2,
  LocateFixed,
  MapPin,
  ParkingCircle,
  Pill,
  RefreshCw,
  Users,
} from "lucide-react";
import { catalogUrl, useDatasets, useParking } from "./api";
import "./styles.css";

const parkingSource =
  "https://www.data.gov.cy/el/dataset/trehoysa-diathesimotita-horon-stathmeysis-dimoy-leykosias";
const pharmacySource =
  "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";
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
  useEffect(() => {
    const title =
      {
        "/": "Statistics",
        "/parking": "Parking",
        "/pharmacies": "Pharmacies",
        "/about": "Sources",
      }[location.pathname] || "Not found";
    document.title = `${title} · Cyprus Open`;
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="site-header">
        <div className="wrap header-inner">
          <Link to="/" className="wordmark" aria-label="Cyprus Open home">
            <span className="logo-mark" aria-hidden="true">
              c<span />
            </span>
            cyprus<span className="wordmark-light">open</span>
          </Link>
          <nav aria-label="Main navigation">
            <NavLink to="/" end>
              <ChartColumn size={17} aria-hidden="true" />
              <span>Statistics</span>
            </NavLink>
            <NavLink to="/parking">
              <ParkingCircle size={18} aria-hidden="true" />
              <span>Parking</span>
            </NavLink>
            <NavLink to="/pharmacies">
              <Pill size={17} aria-hidden="true" />
              <span>Pharmacies</span>
            </NavLink>
            <NavLink to="/about">
              <Database size={16} aria-hidden="true" />
              <span>Sources</span>
            </NavLink>
          </nav>
          <span className="preview-badge">Preview</span>
        </div>
      </header>
      <main id="main" className="wrap">
        <Routes>
          <Route path="/" element={<Statistics />} />
          <Route path="/parking" element={<Parking />} />
          <Route path="/pharmacies" element={<Pharmacies />} />
          <Route path="/about" element={<Sources />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      <footer className="wrap">
        <span>Cyprus Open</span>
        <Link to="/about">
          Official sources. Open data.
          <ArrowUpRight size={13} aria-hidden="true" />
        </Link>
      </footer>
    </>
  );
}

function Illustration({
  kind,
}: {
  kind: "people" | "travel" | "parking" | "pharmacy";
}) {
  const MainIcon = {
    people: Users,
    travel: Globe2,
    parking: ParkingCircle,
    pharmacy: Pill,
  }[kind];
  const SmallIcon =
    kind === "parking"
      ? MapPin
      : kind === "pharmacy"
        ? LocateFixed
        : kind === "people"
          ? Globe2
          : MapPin;
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
  return (
    <>
      <PageTitle
        title="Statistics"
        subtitle="People & travel"
        icon={<ChartColumn size={19} />}
      />
      {datasets.isError && (
        <div className="error-banner" role="alert">
          <span>Statistics are temporarily unavailable.</span>
          <button
            onClick={() => void datasets.refetch()}
            disabled={datasets.isFetching}
          >
            <RefreshCw size={15} aria-hidden="true" />
            Retry
          </button>
        </div>
      )}
      <section className="dataset-grid" aria-label="Statistical datasets">
        <DatasetCard
          kind="people"
          title="Immigration"
          description="Country of previous residence · annual"
          loading={datasets.isPending}
        />
        <DatasetCard
          kind="travel"
          title="Tourist arrivals"
          description="Country of usual residence · seasonal"
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
  return (
    <article className="dataset-card">
      <div className="card-heading">
        <h2>{title}</h2>
        <span className="status-label">
          <span />
          Not published
        </span>
      </div>
      <p className="card-description">{description}</p>
      <div className="chart-empty" aria-live="polite" aria-busy={loading}>
        <Illustration kind={kind} />
        <p>{loading ? "Loading…" : "No verified data yet"}</p>
      </div>
      <div className="card-footer">
        <span>
          <Clock3 size={14} aria-hidden="true" />
          Awaiting first import
        </span>
        <SourceLink href={catalogUrl}>Source</SourceLink>
      </div>
    </article>
  );
}

function Parking() {
  const report = useParking();
  return (
    <>
      <PageTitle
        title="Parking"
        subtitle="Nicosia"
        icon={<ParkingCircle size={20} />}
      />
      <section className="tool-panel">
        <div className="tool-toolbar">
          <span>
            <MapPin size={16} aria-hidden="true" />
            Municipal car parks
          </span>
          <span className="status-label">
            <span />
            Feed not connected
          </span>
        </div>
        <div className="tool-empty">
          <Illustration kind="parking" />
          <h2>No availability reports yet</h2>
          <p>The official feed is being verified.</p>
          <SourceLink href={parkingSource}>View official dataset</SourceLink>
        </div>
        <div className="tool-bottom">
          <span role="status">
            {report.isPending
              ? "Loading…"
              : report.isError
                ? "Connection unavailable"
                : "No live readings"}
          </span>
          <SourceLink href="https://www.nicosia.org.cy/en-GB/municipality/services/cashier/parking/">
            Municipal parking guide
          </SourceLink>
        </div>
      </section>
      {report.isError && (
        <button
          className="retry-button"
          onClick={() => void report.refetch()}
          disabled={report.isFetching}
        >
          <RefreshCw size={15} aria-hidden="true" />
          Retry connection
        </button>
      )}
    </>
  );
}

function Pharmacies() {
  return (
    <>
      <PageTitle
        title="Pharmacies"
        subtitle="On duty & nearby"
        icon={<Pill size={19} />}
      />
      <section className="tool-panel">
        <div className="tool-toolbar">
          <span>
            <LocateFixed size={16} aria-hidden="true" />
            Map & nearby search
          </span>
          <span className="status-label">
            <span />
            Coming next
          </span>
        </div>
        <div className="tool-empty">
          <Illustration kind="pharmacy" />
          <h2>Duty schedules are being checked</h2>
          <p>Nearby search will use your location only when requested.</p>
          <SourceLink href={pharmacySource}>View official dataset</SourceLink>
        </div>
      </section>
    </>
  );
}

function Sources() {
  return (
    <>
      <PageTitle
        title="Sources"
        subtitle="Cyprus Government Open Data Portal"
        icon={<Database size={19} />}
      />
      <section className="sources-list" aria-label="Data sources">
        <SourceRow
          title="Statistics"
          description="Dataset selection in progress"
          href={catalogUrl}
          icon={<ChartColumn size={21} />}
        />
        <SourceRow
          title="Nicosia parking"
          description="Nicosia Municipality · XML · CC BY 4.0"
          href={parkingSource}
          icon={<ParkingCircle size={23} />}
        />
        <SourceRow
          title="On-duty pharmacies"
          description="Pharmaceutical Services · CSV · CC BY 4.0"
          href={pharmacySource}
          icon={<Pill size={22} />}
        />
      </section>
      <p className="source-note">
        Data imports are not connected in this preview. No sample readings are
        shown.
      </p>
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
  return (
    <div className="source-row">
      <span className="quick-icon" aria-hidden="true">
        {icon}
      </span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <SourceLink href={href}>Open</SourceLink>
    </div>
  );
}

function PageTitle({
  title,
  subtitle,
  icon,
}: {
  title: string;
  subtitle: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="page-title">
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      <span className="page-icon" aria-hidden="true">
        {icon}
      </span>
    </div>
  );
}

function NotFound() {
  return (
    <>
      <PageTitle
        title="Page not found"
        subtitle="404"
        icon={<MapPin size={19} />}
      />
      <Link className="back-link" to="/">
        Back to statistics
      </Link>
    </>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);

import { lazy, Suspense, useEffect } from "react";
import { Link, NavLink, Route, Routes, useLocation } from "react-router-dom";
import { SoftTile } from "./components/icons/SoftTile";
import { sections } from "./navigation";
import { useI18n } from "./i18n";
import { languages, type Language, type MessageKey } from "./locales";
import NotFound from "./pages/NotFound";
import About from "./pages/About";
import Statistics from "./statistics/Statistics";

const Parking = lazy(() => import("./parking/Parking"));
const Pharmacies = lazy(() => import("./pharmacies/Pharmacies"));
const AirQuality = lazy(() => import("./air/AirQuality"));
const Beaches = lazy(() => import("./beaches/Beaches"));
const Nature = lazy(() => import("./nature/Nature"));
const Services = lazy(() => import("./services/Services"));
const Recycling = lazy(() => import("./recycling/Recycling"));
const Tools = lazy(() => import("./tools/Tools"));


export function App() {
  const { pathname } = useLocation();
  const { language, setLanguage, t } = useI18n();
  const title: MessageKey =
    sections.find((item) => item.path === pathname)?.label ?? "notFound";

  useEffect(() => {
    document.title = `${t(title)} · Cyprus Open`;
  }, [title, t]);
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <>
      <a className="skip-link" href="#main">
        {t("skip")}
      </a>
      <header className="site-header">
        <div className="wrap header-inner">
          <Link to="/" className="wordmark" aria-label={t("home")}>
            <img className="mouflon-mark" src="/mouflon-friendly.png" alt="" />
            cyprus<span className="wordmark-light">open</span>
          </Link>
          <nav aria-label={t("navigation")}>
            {sections.map(({ path, label, icon }) => (
              <NavLink key={path} to={path} end={path === "/"}>
                <SoftTile icon={icon} />
                <span>{t(label)}</span>
              </NavLink>
            ))}
          </nav>
          <select
            id="language"
            className="language-select"
            aria-label={t("language")}
            value={language}
            onChange={(event) => setLanguage(event.target.value as Language)}
          >
            {languages.map((code) => (
              <option key={code} value={code}>
                {code.toUpperCase()}
              </option>
            ))}
          </select>
        </div>
      </header>
      <main id="main" className="wrap">
        <Suspense fallback={<p className="route-loading">{t("loading")}</p>}>
          <Routes>
            <Route path="/" element={<Statistics />} />
            <Route path="/parking" element={<Parking />} />
            <Route path="/pharmacies" element={<Pharmacies />} />
            <Route path="/beaches" element={<Beaches />} />
            <Route path="/air" element={<AirQuality />} />
            <Route path="/nature" element={<Nature />} />
            <Route path="/services" element={<Services />} />
            <Route path="/recycling" element={<Recycling />} />
            <Route path="/tools" element={<Tools />} />
            <Route path="/about" element={<About />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
    </>
  );
}

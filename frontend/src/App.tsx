import { lazy, Suspense, useEffect } from "react";
import { Link, NavLink, Route, Routes, useLocation } from "react-router-dom";
import {
  ChartColumn,
  Database,
  ParkingCircle,
  Pill,
  Wind,
  type LucideIcon,
} from "lucide-react";
import { useI18n } from "./i18n";
import { languages, type Language, type MessageKey } from "./locales";
import NotFound from "./pages/NotFound";
import Sources from "./pages/Sources";
import Statistics from "./statistics/Statistics";

const Parking = lazy(() => import("./parking/Parking"));
const Pharmacies = lazy(() => import("./pharmacies/Pharmacies"));
const AirQuality = lazy(() => import("./air/AirQuality"));

const navigation: {
  path: string;
  label: MessageKey;
  icon: LucideIcon;
  iconSize: number;
}[] = [
  { path: "/", label: "statistics", icon: ChartColumn, iconSize: 17 },
  { path: "/parking", label: "parking", icon: ParkingCircle, iconSize: 18 },
  { path: "/pharmacies", label: "pharmacies", icon: Pill, iconSize: 17 },
  { path: "/air", label: "airQuality", icon: Wind, iconSize: 17 },
  { path: "/about", label: "sources", icon: Database, iconSize: 16 },
];

export function App() {
  const { pathname } = useLocation();
  const { language, setLanguage, t } = useI18n();
  const title =
    navigation.find((item) => item.path === pathname)?.label ?? "notFound";

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
            {navigation.map(({ path, label, icon: Icon, iconSize }) => (
              <NavLink key={path} to={path} end={path === "/"}>
                <Icon size={iconSize} aria-hidden="true" />
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
            <Route path="/air" element={<AirQuality />} />
            <Route path="/about" element={<Sources />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
    </>
  );
}

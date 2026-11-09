import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { ErrorBoundary } from "../components/ErrorBoundary";
import { type GlyphName, SoftTile } from "../components/icons/SoftTile";
import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";
import { categories } from "./cards";
import { ChartCard } from "./ChartCard";
import { StatisticCard } from "./StatisticCard";
import "./statistics.css";

const COLLAPSED_KEY = "cyprus-open-statistics-collapsed";

const categoryIcons: Record<string, GlyphName> = {
  population: "people",
  family: "family",
  economy: "economy",
  housing: "housing",
  transport: "transport",
  tourism: "tourism",
  safety: "safety",
};

/** Category ids the viewer folded away; a convenience only, so unreadable storage means "all open". */
function savedCollapsed(): Set<string> {
  try {
    const saved: unknown = JSON.parse(localStorage.getItem(COLLAPSED_KEY) ?? "[]");
    return new Set(Array.isArray(saved) ? saved.filter((id): id is string => typeof id === "string") : []);
  } catch {
    return new Set();
  }
}

export default function Statistics() {
  const { t } = useI18n();
  const [collapsed, setCollapsed] = useState(savedCollapsed);

  const toggle = (id: string) =>
    setCollapsed((current) => {
      const next = new Set(current);
      if (!next.delete(id)) next.add(id);
      try {
        localStorage.setItem(COLLAPSED_KEY, JSON.stringify([...next]));
      } catch {
        // Storage can be unavailable (private mode); the choice then lasts for this visit only.
      }
      return next;
    });

  return (
    <>
      <PageTitle title={t("statistics")} path="/" />
      {categories.map((category) => {
        const open = !collapsed.has(category.id);
        const panel = "statistics-" + category.id;
        return (
          <section key={category.id} className="statistics-category" aria-labelledby={panel + "-title"}>
            <h2 id={panel + "-title"} className="category-heading">
              <button type="button" aria-expanded={open} aria-controls={open ? panel : undefined} onClick={() => toggle(category.id)}>
                {categoryIcons[category.id] && <SoftTile icon={categoryIcons[category.id]} size={32} />}
                <span>{t(category.title)}</span>
                <span className="category-count">{category.cards.length}</span>
                <ChevronDown size={18} aria-hidden="true" className="category-chevron" />
              </button>
            </h2>
            {/* Folded categories render nothing, so their datasets are not requested either. */}
            {open && (
              <div id={panel} className="dataset-grid">
                {category.cards.map((card) => (
                  <ErrorBoundary
                    key={card.id}
                    fallback={(reset) => (
                      <ChartCard
                        title={t(card.title)}
                        description={t(card.description)}
                        illustration={card.illustration}
                        state={{ status: "error", retry: reset, retrying: false }}
                      />
                    )}
                  >
                    <StatisticCard card={card} />
                  </ErrorBoundary>
                ))}
              </div>
            )}
          </section>
        );
      })}
    </>
  );
}

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { messages, type Language, type MessageKey } from "./locales";

type Translate = (
  key: MessageKey,
  values?: Record<string, string | number>,
) => string;
const I18nContext = createContext<{
  language: Language;
  setLanguage: (language: Language) => void;
  t: Translate;
} | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>(() => {
    try {
      const saved = localStorage.getItem("cyprus-open-language");
      return saved === "ru" || saved === "el" ? saved : "en";
    } catch {
      return "en";
    }
  });
  useEffect(() => {
    document.documentElement.lang = language;
    try {
      localStorage.setItem("cyprus-open-language", language);
    } catch {
      /* The current tab still supports language selection without storage. */
    }
  }, [language]);
  const t: Translate = (key, values = {}) =>
    messages[language][key].replace(/\{(\w+)\}/g, (match, name: string) =>
      String(values[name] ?? match),
    );
  return (
    <I18nContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) throw new Error("Missing language provider");
  return context;
}

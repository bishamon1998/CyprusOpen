import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  languages,
  messages,
  type Language,
  type MessageKey,
} from "./locales";

const STORAGE_KEY = "cyprus-open-language";

type Translate = (
  key: MessageKey,
  values?: Record<string, string | number>,
) => string;
type I18n = {
  language: Language;
  setLanguage: (language: Language) => void;
  t: Translate;
};
const I18nContext = createContext<I18n | null>(null);

function savedLanguage(): Language {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return languages.find((language) => language === saved) ?? "en";
  } catch {
    return "en";
  }
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>(savedLanguage);
  useEffect(() => {
    document.documentElement.lang = language;
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      /* The current tab still supports language selection without storage. */
    }
  }, [language]);
  const value = useMemo<I18n>(
    () => ({
      language,
      setLanguage,
      t: (key, values = {}) =>
        messages[language][key].replace(/\{(\w+)\}/g, (match, name: string) =>
          String(values[name] ?? match),
        ),
    }),
    [language],
  );
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) throw new Error("Missing language provider");
  return context;
}

import React from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import { I18nProvider } from "./i18n";
import "@fontsource-variable/manrope";
import "./styles.css";

// After a deploy the previous build's hashed chunks are gone. A tab opened before it fails to load a page chunk;
// reload once to pick up the new build (at most once a minute, so a real outage does not loop).
const RELOADED_AT = "cyprus-open-chunk-reload";
window.addEventListener("vite:preloadError", (event) => {
  try {
    const last = Number(sessionStorage.getItem(RELOADED_AT) ?? 0);
    if (Date.now() - last < 60_000) return;
    sessionStorage.setItem(RELOADED_AT, String(Date.now()));
  } catch {
    // Without storage, still reload once: the event fires again only if the new build also fails.
  }
  event.preventDefault();
  window.location.reload();
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30000, refetchOnWindowFocus: false },
  },
});

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

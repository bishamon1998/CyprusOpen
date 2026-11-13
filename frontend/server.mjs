import express from "express";
import { createProxyMiddleware } from "http-proxy-middleware";
import { fileURLToPath } from "node:url";

const app = express();
const tileUrl =
  process.env.MAP_TILE_URL || "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const tileOrigin = new URL(tileUrl);
const attributionUrl =
  process.env.MAP_ATTRIBUTION_URL || "https://www.openstreetmap.org/copyright";
if (
  tileOrigin.protocol !== "https:" ||
  tileOrigin.username ||
  tileOrigin.password ||
  new URL(attributionUrl).protocol !== "https:"
)
  throw new Error("Invalid map configuration");
app.disable("x-powered-by");
app.use((_request, response, next) => {
  response.set({
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "X-Frame-Options": "DENY",
    "Content-Security-Policy": `default-src 'self'; script-src 'self'; style-src 'self'; style-src-attr 'unsafe-inline'; img-src 'self' data: ${tileOrigin.origin}; connect-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'self'`,
  });
  next();
});
app.get("/healthz", (_request, response) => response.json({ status: "UP" }));
app.get("/map-config.json", (_request, response) =>
  response.json({
    tileUrl,
    attribution: process.env.MAP_ATTRIBUTION || "© OpenStreetMap contributors",
    attributionUrl,
  }),
);

for (const [prefix, target] of [
  [
    "/api/v1/statistics",
    process.env.STATISTICS_UPSTREAM || "http://localhost:8080",
  ],
  ["/api/v1/parking", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  // Beaches, air quality, nature sites, public services and recycling points are served by places-service with parking: one machine for the map tools.
  ["/api/v1/air-quality", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  ["/api/v1/beaches", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  ["/api/v1/nature", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  ["/api/v1/services", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  ["/api/v1/recycling", process.env.PARKING_UPSTREAM || "http://localhost:8080"],
  ["/api/v1/pharmacies", process.env.PHARMACIES_UPSTREAM || "http://localhost:8080"],
]) {
  const upstream = new URL(target);
  if (
    !["http:", "https:"].includes(upstream.protocol) ||
    upstream.username ||
    upstream.password
  ) {
    throw new Error("Invalid upstream configuration");
  }
  app.use(
    createProxyMiddleware({
      pathFilter: (pathname) =>
        pathname === prefix || pathname.startsWith(`${prefix}/`),
      target,
      changeOrigin: true,
      proxyTimeout: 5000,
      timeout: 10000,
      on: {
        error: (_error, _request, response) => {
          if ("writeHead" in response && !response.headersSent) {
            response.writeHead(502, {
              "Content-Type": "application/problem+json",
              "Cache-Control": "no-store",
            });
            response.end(
              JSON.stringify({
                title: "Service unavailable",
                status: 502,
                code: "service_unavailable",
              }),
            );
          }
        },
      },
    }),
  );
}
app.use("/api", (_request, response) =>
  response.status(404).json({ title: "Not found", status: 404 }),
);
const directory = fileURLToPath(new URL("./dist/", import.meta.url));
// Vite names built files by content hash, so browsers may keep them; index.html stays no-cache.
app.use("/assets", express.static(`${directory}assets`, { index: false, immutable: true, maxAge: "1y" }));
app.use(express.static(directory, { index: false }));
// Browsers and crawlers ask for /favicon.ico regardless of the <link rel="icon">; answer with the same PNG.
app.get("/favicon.ico", (_request, response) => {
  response.type("png").sendFile(`${directory}mouflon-friendly.png`);
});
app.get(["/", "/parking", "/pharmacies", "/beaches", "/air", "/nature", "/about"], (_request, response) => {
  response.set("Cache-Control", "no-cache").sendFile(`${directory}index.html`);
});
// A mistyped page address gets the app's own "page not found" page; files and other requests get JSON.
app.get(/^\/[^.]*$/, (request, response, next) => {
  if (!request.accepts("html")) return next();
  response.status(404).set("Cache-Control", "no-cache").sendFile(`${directory}index.html`);
});
app.use((_request, response) =>
  response.status(404).json({ title: "Not found", status: 404 }),
);

const server = app.listen(Number(process.env.PORT || 3000), "0.0.0.0");
server.requestTimeout = 15000;
server.headersTimeout = 10000;
for (const signal of ["SIGTERM", "SIGINT"]) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(1), 10000).unref();
  });
}

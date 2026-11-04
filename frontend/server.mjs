import express from "express";
import { createProxyMiddleware } from "http-proxy-middleware";
import { fileURLToPath } from "node:url";

const app = express();
app.disable("x-powered-by");
app.use((_request, response, next) => {
  response.set({
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "X-Frame-Options": "DENY",
    "Content-Security-Policy":
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'self'",
  });
  next();
});
app.get("/healthz", (_request, response) => response.json({ status: "UP" }));

for (const [prefix, target] of [
  [
    "/api/v1/statistics",
    process.env.STATISTICS_UPSTREAM || "http://localhost:8081",
  ],
  ["/api/v1/parking", process.env.PARKING_UPSTREAM || "http://localhost:8082"],
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
app.use(express.static(directory, { index: false }));
app.get(["/", "/parking", "/pharmacies", "/about"], (_request, response) => {
  response.set("Cache-Control", "no-cache").sendFile(`${directory}index.html`);
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

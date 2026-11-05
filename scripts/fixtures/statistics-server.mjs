import http from "node:http";
import { readFileSync } from "node:fs";

// Serves the statistics-service integration fixtures in place of the CYSTAT PxWeb API.
const tables = {
  "/api/Tourism/Tourists/Monthly/2021010E.px": "tourist-arrivals.json",
  "/api/Population/Migration/1840030E.px": "immigrant-groups.json",
  "/api/Population/Migration/1840010E.px": "migration-movements.json",
};
http.createServer((request, response) => {
  const fixture = tables[request.url];
  if (request.method !== "POST" || !fixture) { response.writeHead(404).end(); return; }
  request.resume();
  request.on("end", () => {
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(readFileSync(new URL(fixture, "file:///cystat/")));
  });
}).listen(9091, "0.0.0.0");

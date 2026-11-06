import http from "node:http";
import { readFileSync } from "node:fs";

// Serves the statistics-service integration fixtures in place of the CYSTAT PxWeb API.
// The table list is shared with StatisticsApiIT (cystat/tables.properties).
const tables = Object.fromEntries(
  readFileSync("/cystat/tables.properties", "utf8")
    .split(/\r?\n/)
    .filter((line) => line && !line.startsWith("#"))
    .map((line) => {
      const separator = line.indexOf("=");
      return ["/api/" + line.slice(0, separator), line.slice(separator + 1)];
    }),
);
http.createServer((request, response) => {
  const fixture = tables[decodeURIComponent(new URL(request.url, "http://fixture").pathname)];
  if (request.method !== "POST" || !fixture) { response.writeHead(404).end(); return; }
  request.resume();
  request.on("end", () => {
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(readFileSync("/cystat/" + fixture));
  });
}).listen(9091, "0.0.0.0");

import http from "node:http";
import fs, { readFileSync } from "node:fs";

const xml = readFileSync(new URL("./parking-directory.xml", import.meta.url));
// Trimmed copy of the real air quality response (three stations in Cyprus and one misplaced).
const air = readFileSync(new URL("./air-quality.json", import.meta.url));
// Trimmed copies of the real EEA Discodata tables, chosen by the table named in the SQL query.
const beachTables = {
  "[spatial_ProtectedArea]": "beach-spatial.json",
  "[assessment_BathingWaterStatus]": "beach-status.json",
  "[assessment_BathingWaterQuality]": "beach-quality.json",
};
// Trimmed copies of the real Department of Forests files, served by file name like data.gov.cy.
const natureFiles = new Set(fs.readdirSync(new URL("./nature/", import.meta.url)));
// Copies of the real hospital and Citizen Service Centre files and a trimmed Green Dot file, by data.gov.cy name.
const dataGovFiles = {
  "/services/Κατάλογος Νοσοκομείων Κύπρου_1.csv": ["./services/hospitals.csv", "Mon, 23 Oct 2023 03:56:55 GMT"],
  "/services/CSC Info and Location Data_5.csv": ["./services/centres.csv", "Tue, 20 Jan 2026 09:07:22 GMT"],
  "/recycling/db-greendot-pois_190624_toOpenData.csv": ["./recycling/points.csv", "Mon, 23 Oct 2023 03:57:06 GMT"],
};
http.createServer((request, response) => {
  const url = new URL(request.url, "http://fixture");
  const dataGovFile = dataGovFiles[decodeURIComponent(url.pathname)];
  if (dataGovFile) {
    response.writeHead(200, { "Content-Type": "text/csv", "Last-Modified": dataGovFile[1] });
    response.end(readFileSync(new URL(dataGovFile[0], import.meta.url)));
    return;
  }
  if (url.pathname.startsWith("/nature/")) {
    const file = url.pathname.slice("/nature/".length);
    if (!natureFiles.has(file)) { response.writeHead(404).end(); return; }
    response.writeHead(200, { "Last-Modified": "Mon, 23 Oct 2023 03:56:43 GMT" });
    response.end(readFileSync(new URL("./nature/" + file, import.meta.url)));
    return;
  }
  if (url.pathname === "/sql") {
    const query = url.searchParams.get("query") ?? "";
    const table = Object.keys(beachTables).find((name) => query.includes(name));
    if (!table) { response.writeHead(404).end(); return; }
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(readFileSync(new URL("./" + beachTables[table], import.meta.url)));
    return;
  }
  if (request.url === "/air") {
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(air);
    return;
  }
  if (request.url !== "/directory") { response.writeHead(404).end(); return; }
  if (request.headers["if-none-match"] === '"fixture-1"') { response.writeHead(304).end(); return; }
  response.writeHead(200, { "Content-Type": "application/xml", ETag: '"fixture-1"' });
  response.end(xml);
}).listen(9090, "0.0.0.0");

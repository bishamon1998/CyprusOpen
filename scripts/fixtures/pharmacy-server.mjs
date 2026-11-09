// Serves catalog metadata and one duty per district for the current Cyprus day,
// so end-to-end checks exercise the pharmacy refresh without contacting data.gov.cy.
import http from "node:http";

const origin = process.env.FIXTURE_ORIGIN || "http://pharmacy-fixture:9091";
const districts = {
  nicosia: "Λευκωσία",
  larnaca: "Λάρνακα",
  limassol: "Λεμεσός",
  paphos: "Πάφος",
  famagusta: "Αμμόχωστος",
};
const header =
  "Date,Day,Reg. No.,Surmame,Name,Address,Additional Address Info,Muniuciplity / Community,Pharmacy Tel. No.,House Tel. No.\n";

function rosterDays() {
  // Cover yesterday to tomorrow so the roster is current at any hour.
  return [-1, 0, 1].map((offset) =>
    new Date(Date.now() + offset * 86_400_000).toLocaleDateString("en-GB", {
      timeZone: "Asia/Nicosia",
    }),
  );
}

http
  .createServer((request, response) => {
    if (request.url === "/metadata") {
      const distributions = Object.entries(districts).map(([code, title]) => ({
        "dct:title": title,
        "dcat:downloadURL": { "@rdf:resource": `${origin}/csv/${code}` },
      }));
      response.writeHead(200, { "Content-Type": "application/json" });
      response.end(JSON.stringify({
        "dcat:Catalog": { "dcat:dataset": [{ "dcat:Dataset": { "dct:modified": { "#": "2026-10-03T10:42:38+03:00" } } }] },
        "dcat:Distribution": distributions,
      }));
      return;
    }
    const code = request.url?.match(/^\/csv\/([a-z]+)$/)?.[1];
    if (!code || !districts[code]) {
      response.writeHead(404).end();
      return;
    }
    const index = Object.keys(districts).indexOf(code) + 1;
    const rows = rosterDays()
      .map(
        (day) =>
          `${day},,${index}0,Example,Pharmacy ${index},Main Street ${index},,${code},2212345${index},\n`,
      )
      .join("");
    response.writeHead(200, { "Content-Type": "text/csv" });
    response.end(header + rows);
  })
  .listen(9091, "0.0.0.0");

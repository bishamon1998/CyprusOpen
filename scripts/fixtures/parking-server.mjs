import http from "node:http";
import { readFileSync } from "node:fs";

const xml = readFileSync(new URL("./parking-directory.xml", import.meta.url));
http.createServer((request, response) => {
  if (request.url !== "/directory") { response.writeHead(404).end(); return; }
  if (request.headers["if-none-match"] === '"fixture-1"') { response.writeHead(304).end(); return; }
  response.writeHead(200, { "Content-Type": "application/xml", ETag: '"fixture-1"' });
  response.end(xml);
}).listen(9090, "0.0.0.0");

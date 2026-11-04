# Cyprus Open

Public statistics and everyday tools for people living in Cyprus.

**Live preview: https://cyprus-open.up.railway.app**

The app includes English, Russian and Greek interfaces, Statistics and Parking APIs, source links, health checks and Railway-ready Docker builds. Parking has a searchable map/list of 651 source-backed locations across Cyprus, area filters, optional nearby sorting and directions in Google Maps. Statistics imports and live parking availability are not connected yet. Pharmacies are planned.

## Run locally

With Docker Desktop running, copy `.env.example` to `.env` only if it does not exist and set three distinct database passwords. Then:

```sh
docker compose up -d --build frontend statistics-service parking-service
```

Open **http://localhost:3000**. The app does not require PostgreSQL until data imports are implemented. To start the optional isolated development databases, run `docker compose up -d postgres`. Never remove the existing volume to update the app.

## Development

Java 25, Maven Wrapper, React, TypeScript, Vite and pnpm 11.25.0. Backends build independently:

```sh
./mvnw -B -pl backend/statistics-service -am verify
./mvnw -B -pl backend/parking-service -am verify
cd frontend
pnpm install --frozen-lockfile
pnpm build
```

The frontend development server uses `pnpm dev`; it proxies to local backends on ports 8081/8082. The production server uses `pnpm start`, listens on `PORT` (default 3000), and reads `STATISTICS_UPSTREAM` and `PARKING_UPSTREAM` as server-only HTTP origins. Health endpoints are `/healthz` and backend `/actuator/health/readiness`.

## Parking data and maps

The reviewed directory snapshot comes from the [Cyprus National Access Point / Public Works Department](https://www.traffic4cyprus.org.cy/en/dataset/parking_areas), under CC BY 4.0. Its [DATEX II XML resource](https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/) provides identifiers, names and coordinates, with no occupancy, prices or opening hours. The bundled snapshot records publication/retrieval times and transformations. Missing English names and OSM-origin placeholders are displayed as unnamed car parks. Area filters are approximate map viewports, not municipal boundaries. Original coordinates are preserved; they are not verified entrance points.

The [owner-selected Nicosia dataset](https://www.data.gov.cy/el/dataset/trehoysa-diathesimotita-horon-stathmeysis-dimoy-leykosias) links to a public [RSS XML feed](https://www.nicosia.org.cy/el-GR/rss/parkingspaces/). On 4 November 2026, all four observations were dated 5 December 2022; negative counts were also present. These are not usable live readings. The API deliberately returns `freshness: unavailable`, a null occupancy timestamp and null counts. Directory retrieval never makes occupancy fresh. No upstream requests occur during application startup, API reads or tests.

To prepare a replacement directory, download the verified resource once after checking its terms, then run `./scripts/prepare-parking-directory.ps1 -InputPath <local-xml-file>` in PowerShell. The script only reads a local file, disables XML external entities and retains English names. Review the diff and metadata before publishing. No automatic refresh is enabled.

Leaflet uses clustered markers and standard OpenStreetMap tiles with visible attribution, normal browser caching and no prefetch. Tiles are best-effort; the list and directions remain usable if tiles fail. Production can configure `MAP_TILE_URL`, `MAP_ATTRIBUTION` and `MAP_ATTRIBUTION_URL` without rebuilding; use HTTPS and honor the chosen provider's terms. The map configuration contains public client information, so do not put secrets there. Run `pnpm start` alongside Vite to serve `/map-config.json` during development.

Geolocation is requested only through Near me, used in memory for straight-line distances, and not sent to our API or stored. Map tiles reveal the viewed area to the map provider. Google Maps directions links need no API key and open only on user action; an embedded Google Maps replacement would require a separately configured API key and billing.

`pnpm test:e2e` checks real APIs, area coverage, search/map/list selection, directions, geolocation/denial, tile failure and responsive layouts/language persistence. External map requests are intercepted locally; no government or tile-provider requests leave CI.

## Railway

Keep the repository root as build context for three independent Railway services. Configure each service's Dockerfile path, port and health check in Railway:

| Service | Dockerfile | PORT | Health check |
| --- | --- | --- | --- |
| Frontend | `frontend/Dockerfile` | 3000 | `/healthz` |
| Statistics | `backend/statistics-service/Dockerfile` | 8081 | `/actuator/health/readiness` |
| Parking | `backend/parking-service/Dockerfile` | 8082 | `/actuator/health/readiness` |

Railway detects the Docker build from the Dockerfile path; do not set an unsupported builder enum. The current API rejects new legacy `railway.toml` config paths, so service settings are managed directly in Railway. All builds run their checks before creating the runtime image.

Set backend `PORT` to 8081 for Statistics and 8082 for Parking. Set the frontend upstream variables to the corresponding Railway private HTTP origins including those ports; use the actual internal hostnames assigned to your services. Only the frontend needs a public domain. The frontend honors Railway's injected `PORT`. No scheduler or external data fetching is implemented or enabled in this release.

Submit related changes together through a PR. The owner reviews and merges; never push directly to main. README is published; other Markdown documentation stays local. The primary data catalog is [data.gov.cy](https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset).

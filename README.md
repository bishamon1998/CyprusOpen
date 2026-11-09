# Cyprus Open

Public statistics and everyday tools for people living in Cyprus.

[cyprus-open.cy](https://cyprus-open.cy) · [Source catalog](https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset)

React/TypeScript with EN/RU/EL interfaces, a Spring Boot backend made of separate Statistics, Places (parking, beaches, air quality, nature) and Pharmacies modules, and a searchable map of 651 verified parking locations. Statistics charts (migration, tourism, inflation, population, marriages, crime, earnings, fuel) use official CYSTAT tables; live parking occupancy is not connected yet.

## Local development

Use Docker Desktop. Preserve the existing `.env`; for a new checkout copy `.env.example` and set three distinct database passwords. Run:

```sh
docker compose up -d --build frontend backend
```

Open http://localhost:3000. Parking now requires PostgreSQL; Compose starts it automatically and preserves the existing volume. The Parking role owns its database/schema, separately from Statistics. Liquibase owns schema migrations. Never remove the database volume to update the app.

Java 25, Spring Boot 4.1.2, Maven Wrapper, Node 24, React, TypeScript, Vite and pnpm 11.25.0. Build/test independently:

```sh
./mvnw -B -ntp -pl backend/places-service -am verify
./mvnw -B -ntp -pl backend/statistics-service -am verify
./mvnw -B -ntp -pl backend/pharmacies-service -am verify
./mvnw -B -ntp -pl backend/app -am verify
cd frontend
pnpm install --frozen-lockfile
pnpm build
pnpm test:e2e
```

Tests are integration and end-to-end only. Each backend module has `*IT` tests that call its HTTP API and run its refresh jobs against an isolated PostgreSQL Testcontainer and a local HTTP fixture server; `BackendApplicationIT` starts all modules in one process and checks that each migrates only its own changelog. `pnpm test:e2e` drives the real app in a browser: CI starts it with `docker-compose.test.yml` and private fixture servers; the real refresh jobs fill the databases from them. These small English-only protocol fixtures are never a production data source. No government or tile-provider requests leave CI. Docker image packaging skips tests that require a Docker daemon; CI runs those integration tests separately before review.

For Vite development, run the backend locally (port 8080) and `pnpm start` alongside `pnpm dev` for runtime map configuration. The production frontend listens on `PORT` (3000 by default), proxies `STATISTICS_UPSTREAM`, `PARKING_UPSTREAM` and `PHARMACIES_UPSTREAM` (all the backend's address), and exposes `/healthz`. Backend readiness is `/actuator/health/readiness`.

## Parking data and refresh

The [CYNAP / Public Works Department directory](https://www.traffic4cyprus.org.cy/en/dataset/parking_areas) links to the [DATEX II XML resource](https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/) under CC BY 4.0. It contains locations, not occupancy, prices, opening hours or verified entrances. Its declared publication frequency is less frequent than yearly. Some entries originate from OpenStreetMap. Missing English names and OSM-origin placeholders display as unnamed car parks; source identifiers and coordinates are preserved. Area filters are approximate viewports, not municipal boundaries.

PostgreSQL holds the latest normalized directory document (JSONB) and sync metadata in `parking.parking_directory`. One atomic document matches the upstream publication and current whole-directory API; no per-location joins or occupancy history are needed. `JdbcClient` serves persisted data. There is no bundled JSON or seed data. A new database stays empty until the shared HTTP refresh job loads its first successful publication. The same job handles every later update; no source-file edits or rebuild are needed.

The UI distinguishes the source publication timestamp, successful retrieval and last successful check. Displayed dates use Cyprus time (Asia/Nicosia). Publication time is the timestamp on the source's XML package, not a verified modification time for every parking location. On HTTP 304 only the successful-check time changes. Failed requests change nothing in the database: the previous document and successful-check date are kept and the job logs the reason. Attribution (source name, links, license) always comes from the service code, so a correction does not wait for a new publication.

Automatic checking is **disabled by default**. When enabled, the job also runs after startup if the directory is empty. Setting `PARKING_DIRECTORY_SYNC_ENABLED=true` enables one check at **05:00 Cyprus time on the first day of each month** (see [Refresh schedule](#refresh-schedule)). This conservative internal schedule is for the slow-changing directory, not live occupancy. It is not a provider-issued quota. A manual one-shot check uses the same guarded code after PostgreSQL is running:

```sh
docker compose run --rm --no-deps backend --spring.profiles.active=directory-refresh
```

The job uses Spring `RestClient`, conditional ETag/Last-Modified headers, server-configured endpoint (the verified official HTTPS resource by default), no redirects, 5-second connection / 15-second read timeout, and a 2 MiB response limit. The job makes one request per run. If the source is unavailable or returns invalid data, the job logs the outcome and keeps the last successful snapshot. Only server configuration supplies the endpoint; user API input cannot change it. Tests replace that configuration with their HTTP server, without test flags or hostnames in production code. There is no public refresh endpoint. User API requests never call upstream. The browser polls our API every 10 seconds while the directory is empty and every 5 minutes after initialization.

A valid, non-empty publication replaces the previous directory atomically. Invalid XML/DOCTYPE, duplicate identifiers, invalid coordinates or older publication times preserve the previous directory. A 304 preserves publication/retrieval times. Database migrations never reset existing directory data.

The owner's [Nicosia availability dataset](https://www.data.gov.cy/el/dataset/trehoysa-diathesimotita-horon-stathmeysis-dimoy-leykosias) points to [RSS XML](https://www.nicosia.org.cy/el-GR/rss/parkingspaces/). On 4 November 2026 all four observations were dated 5 December 2022, with negative counts also present. Those are not live readings: the API keeps occupancy freshness unavailable and counts/timestamp null. Research into usable live sources is tracked in issue #13; implementation remains in #6.

## Statistics data and refresh

Charts read official [CYSTAT](https://www.cystat.gov.cy) tables, linked from data.gov.cy under CC BY 4.0: immigration ([2174](https://www.data.gov.cy/en/dataset/2174), [2172](https://www.data.gov.cy/en/dataset/2172)), tourist arrivals ([2285](https://www.data.gov.cy/en/dataset/2285)), inflation ([2201](https://www.data.gov.cy/en/dataset/2201), [2314](https://www.data.gov.cy/en/dataset/2314)), population ([2176](https://www.data.gov.cy/en/dataset/2176), [2177](https://www.data.gov.cy/en/dataset/2177), [2142](https://www.data.gov.cy/en/dataset/2142)), marriages and civil unions ([2157](https://www.data.gov.cy/en/dataset/2157), [2352](https://www.data.gov.cy/en/dataset/2352)), serious offences ([1933](https://www.data.gov.cy/en/dataset/1933)), earnings ([1773](https://www.data.gov.cy/en/dataset/1773)), fuel sales ([1535](https://www.data.gov.cy/en/dataset/1535)), births and weddings by calendar date ([2079](https://www.data.gov.cy/en/dataset/2079), [2171](https://www.data.gov.cy/en/dataset/2171)), births and deaths by month ([2078](https://www.data.gov.cy/en/dataset/2078), [2141](https://www.data.gov.cy/en/dataset/2141)), causes of death ([1596](https://www.data.gov.cy/en/dataset/1596)), divorces by years of marriage ([2151](https://www.data.gov.cy/en/dataset/2151)), marriages by nationality of groom and bride ([2163](https://www.data.gov.cy/en/dataset/2163)), age at marriage and at childbirth and births outside marriage ([2160](https://www.data.gov.cy/en/dataset/2160), [2089](https://www.data.gov.cy/en/dataset/2089), [2080](https://www.data.gov.cy/en/dataset/2080)), household spending and income from the 2023 budget survey ([1961](https://www.data.gov.cy/en/dataset/1961), [1990](https://www.data.gov.cy/en/dataset/1990)), the house price index ([2368](https://www.data.gov.cy/en/dataset/2368)), dwellings in building permits ([1467](https://www.data.gov.cy/en/dataset/1467)), vehicles by energy type ([2241](https://www.data.gov.cy/en/dataset/2241)), tourist spending by country ([2262](https://www.data.gov.cy/en/dataset/2262)) and residents' trips ([2268](https://www.data.gov.cy/en/dataset/2268)). The immigrant table publishes only Cyprus / EU / non-EU groups, not individual countries; the UI says so instead of substituting another measure.

`statistics-service` downloads each whole table as JSON-stat 2.0 from the PxWeb API (`STATISTICS_CYSTAT_API_URL`, default `https://cystatdb.cystat.gov.cy/api/v1/en/8.CYSTAT-DB/`). CYSTAT answers bursts with HTTP 429, so tables are fetched `STATISTICS_CYSTAT_REQUEST_INTERVAL` apart (3 s by default) and a rejected request is retried later. Observations live in `statistics.dataset` and `statistics.observation`; a table is replaced atomically and only when CYSTAT reports a newer update time. A failed, invalid or older table keeps its last stored values, and one failing table never stops the others. A table that is not imported yet is served with `Cache-Control: no-store`; imported tables may be cached for an hour.

Adding a chart: declare the table in `StatisticsDatasets` (code, table path, data.gov.cy page, category-to-series mapping), add a fixture line to `src/test/resources/cystat/tables.properties`, then add a card or view to `frontend/src/statistics/cards.ts` with its labels in `locales.ts`. Fetching, storage, refresh, the API, chart drawing, controls and source labels are shared. Country series use ISO codes so the browser localizes names. Periods are years, months (`2024-07`) or quarters (`2024-Q3`); revision markers such as `2023 (r)` are dropped, and survey tables without a time dimension are stored under their survey year. Calendar tables use one series per date (`08-15`) and one period per year; the page draws them as a month × day heatmap. Cards are grouped into collapsible categories in `cards.ts`; a folded category requests no data, and the viewer's choice is kept in browser storage.

Automatic refresh is **disabled by default**. `STATISTICS_SYNC_ENABLED=true` imports missing tables on startup and checks every **Monday at 04:30 Cyprus time** (see [Refresh schedule](#refresh-schedule)). User requests never call CYSTAT. Integration tests and browser CI use local JSON-stat fixtures.

## Refresh schedule

Every scheduled import is listed in one file, [`cyprus-open-refresh.properties`](backend/common-library/src/main/resources/cyprus-open-refresh.properties): job name, cron (Cyprus time) and the environment variable that enables it. Each service implements `RefreshJob` for its own source; the shared `RefreshScheduler` in `common-library` runs it once after startup when data is missing or stale, then on its cron, and logs the schedule and the outcome and duration of every run.

| Job | Service | When | Enabled by |
| --- | --- | --- | --- |
| `parking-directory` | places | 1st of each month, 05:00 | `PARKING_DIRECTORY_SYNC_ENABLED` |
| `pharmacy-schedule` | pharmacies | daily, 06:15 | `PHARMACY_SYNC_ENABLED` |
| `statistics` | statistics | Mondays, 04:30 | `STATISTICS_SYNC_ENABLED` |
| `air-quality` | places | hourly, at :20 | `AIR_QUALITY_SYNC_ENABLED` |
| `beaches` | places | Tuesdays, 05:40 | `BEACHES_SYNC_ENABLED` |
| `nature` | places | 1st of each month, 05:20 | `NATURE_SYNC_ENABLED` |

Upstream clients share timeouts, no redirects, size limits and the user agent (`Upstream`). `Upstream.fetch` reads a body only for 200 OK and within the size limit, and the read timeout also bounds a slowly sent body. Parsers stay with their services because the formats differ: DATEX II XML (parking), DCAT JSON and CSV (pharmacies), JSON-stat (statistics).

## Air quality

The Air quality page shows the latest hourly readings of the Department of Labour Inspection monitoring stations ([data.gov.cy](https://www.data.gov.cy/el/dataset/i/5dc97c12-735d-4cc7-9030-101c735ce1a3), CC BY-SA 4.0). places-service serves it at `/api/v1/air-quality`, so the map tools share one machine; the frontend proxies that path to `PARKING_UPSTREAM`.

The hourly `air-quality` job reads `AIR_QUALITY_RESOURCE_URL` (default `https://www.airquality.dli.mlsi.gov.cy/all_stations_data_PM`), keeps PM2.5, PM10, O3, NO2, SO2 and CO, drops stations outside Cyprus, and stores the snapshot in `parking.air_quality` (JSONB, Liquibase changeset `4-air-quality`). A failed or invalid response keeps the previous snapshot. Measurement times are Cyprus local time in the source and are stored as instants.

Each pollutant and station gets a level of the [European Air Quality Index](https://airindex.eea.europa.eu/AQI/index.html) from its hourly concentration, using the EEA bands (µg/m³, upper bounds of Good … Very poor): PM2.5 5/15/50/90/140, PM10 15/45/120/195/270, O3 60/100/120/160/180, NO2 10/25/60/100/150, SO2 20/40/125/190/275. A station's level is its worst pollutant; CO has no index level. The page shows the EEA health advice for the selected station and warns when the newest reading is more than three hours old.

## Beaches

The Beaches page lists the bathing waters designated under the Bathing Water Directive with their water quality class. places-service serves them at `/api/v1/beaches`.

The weekly `beaches` job reads three tables for Cyprus from the [EEA Discodata](https://discodata.eea.europa.eu/) SQL endpoint (`BEACHES_DISCODATA_URL`, default `https://discodata.eea.europa.eu/sql`, database `WISE_BWD`, schema `latest`): `spatial_ProtectedArea` (names, coordinates, profile document), `assessment_BathingWaterStatus` (class per season) and `assessment_BathingWaterQuality` (E. coli and intestinal enterococci 95th percentiles and sample counts). Queries are fixed `SELECT * … WHERE countryCode='CY'` statements because the endpoint's firewall rejects some statements that list columns or aggregate. Only bathing waters of the latest reporting year with coordinates in Cyprus are kept; the snapshot is stored in `parking.beaches` (JSONB, changeset `5-beaches`), and failures keep it. EEA data is reused under its legal notice (CC BY 4.0); Cyprus reports it through the Department of Environment.

The page shows the latest season's class (EEA publishes one season a year, in spring), up to ten seasons of history, the bacteria results against the limits for "excellent" coastal water (E. coli 250, enterococci 100 cfu/100 ml, 95th percentile) and the official bathing water profile.

## Nature

The Nature page maps the Department of Forests' picnic sites, camping sites and nature trails in the state forests, with trail routes drawn on the map. places-service serves them at `/api/v1/nature`.

The monthly `nature` job downloads six files by name from the data.gov.cy files folder (`NATURE_FILES_URL`, default `https://www.data.gov.cy/sites/default/files/`), all CC BY 4.0: picnic sites ([245](https://www.data.gov.cy/el/dataset/245), `PicnicSites.kml`), camping sites ([233](https://www.data.gov.cy/el/dataset/233), `CampingSites.kmz`, and the catalogue [236](https://www.data.gov.cy/el/dataset/236) for full names and capacity), trail routes ([228](https://www.data.gov.cy/el/dataset/228), `NatureTrailsRoutes.kml`), and the trail starting points ([229](https://www.data.gov.cy/el/dataset/229)) and trail catalogue ([235](https://www.data.gov.cy/el/dataset/235)), used only for full Greek trail names. The files last changed in October 2023.

The layers keep their attributes in an HTML table inside each placemark's description. Greek names there are cut at a byte limit; a cut name is completed when exactly one fuller name matches, otherwise it ends with "…". Points of one picnic site with the same name within 1.5 km become one site; route pieces with the same Greek name become one trail, its length the sum of the published lengths. Unnamed road pieces are left out and routes are simplified to about 6 m. A trail's map point is the first point of its first line, not always its trailhead. Opening periods, fees and fire bans are not in the source. The snapshot is stored in `parking.nature_sites` (JSONB, changeset `6-nature`); failures keep it.

## Maps and privacy

Leaflet with clustered markers uses standard OSM tiles, visible attribution and ordinary browser caching, without prefetch. Runtime `MAP_TILE_URL`, `MAP_ATTRIBUTION` and `MAP_ATTRIBUTION_URL` can change the provider without rebuilding. Use HTTPS and respect provider terms; this public client configuration must not contain secrets. The list and Google Maps directions remain usable if tiles fail.

Near me requests location only on click, calculates straight-line distance with Leaflet, and keeps coordinates in tab memory. Location is not sent to our API or stored. The tile provider sees the viewed area. Google directions links need no key; embedding Google Maps would require a separately configured API key and billing.

## Railway

Keep repository root as the build context. Service settings are managed directly in Railway; obsolete legacy TOML config paths are removed.

| Service | Dockerfile | PORT | Health check |
| --- | --- | --- | --- |
| Frontend | `frontend/Dockerfile` | 3000 | `/healthz` |
| Backend (statistics, places, pharmacies) | `backend/app/Dockerfile` | 8080 | `/actuator/health/readiness` |

The backend runs the Statistics, Places and Pharmacies modules in one process to keep hosting small. The modules stay separate in code (`backend/statistics-service`, `backend/places-service`, `backend/pharmacies-service`): own packages, API paths, refresh jobs, databases, roles, connection pools, Liquibase changelogs and transactions. The backend service takes all their variables (`STATISTICS_*`, `PARKING_*`, `PHARMACIES_*`, `PHARMACY_*`, `AIR_QUALITY_*`, `BEACHES_*`, `NATURE_*`), and the frontend's three upstream variables all point to the backend's private address. To split a module out again, give it a `@SpringBootApplication` class, a Dockerfile and its own Railway service.

Railway bills the memory each container holds, and traffic is low, so the backend is sized for a small steady footprint. The image sets `JAVA_TOOL_OPTIONS` (Serial GC, 384 MB heap cap, C1 compiler only); the backend keeps at most 16 request threads and each module one idle database connection. A 3.3 MB CYSTAT table (560,000 cells, near the 4 MB response limit) imports within the cap. To give the backend more memory, override `JAVA_TOOL_OPTIONS` in its Railway variables.

For the Nature page, set `NATURE_SYNC_ENABLED=true` on the backend; it imports the Department of Forests files on startup when they are missing and then monthly. Verify that `/api/v1/nature` returns sites.

Before deploying Statistics, configure `STATISTICS_JDBC_URL`, `STATISTICS_DB_USER`, `STATISTICS_DB_PASSWORD` and `STATISTICS_SYNC_ENABLED=true`; the restricted role and schema come from `infra/postgres/001-databases.sql`.

Before deploying the new Parking service, provision its restricted database role and schema and configure `PARKING_JDBC_URL`, `PARKING_DB_USER`, `PARKING_DB_PASSWORD` and optionally `PARKING_DB_SCHEMA` (default `parking`). Database secrets stay outside Git. Production database provisioning/deployment is not performed by this PR. Set `PARKING_DIRECTORY_SYNC_ENABLED=true` on the Railway Parking service for the release. On startup, Liquibase prepares the database and Parking imports the official directory if it is empty. Verify that `/api/v1/parking` returns items and a directory check timestamp. If the initial import fails, inspect the Parking logs before restarting the service to retry.

Deploy the updated Parking backend together with the frontend release after review. The frontend currently follows GitHub main; Parking currently uses a manual source. The new frontend handles the old placeholder API as a retryable error. Only the frontend needs a public domain; set its upstreams to the actual Railway private service origins with their ports.

Publish feature branches through PRs; the owner controls merging. README is published in English; other Markdown stays local. Interface translations are an owner-approved exception to English-only repository content.

The places module (formerly parking-service) keeps the `PARKING_` variable prefix and the `parking` database, so no secrets or data moved when it took over beaches and air quality.

## Shared map and parsing components

`frontend/src/components/map/MapView.tsx` accepts plain labelled coordinates, bounds, selection, marker symbol and labels. Tile loading, clustering, camera/resize handling and user-location display are shared. ParkingMap only adapts parking records and translations. Shared viewport presets, distance calculations and explicit-action geolocation live in `geo/` and `hooks/useBrowserLocation.ts`; map sizing can use `--map-height` and `--map-mobile-height`.

`backend/common-library` provides the framework-free `XmlDocumentReader`: bounded namespace-aware XML reading, disabled DTD/external resources and required-field extraction. Parking supplies it through Spring DI; DATEX parking fields and validation stay in DirectoryXmlParser. Other XML resources can reuse the reader; CSV/JSON resources need their own format-specific parsing. Parking builds the shared JAR through the root `-pl ... -am` command; no external artifact registry is needed.

## Pharmacy duty schedules

The pharmacies module stores the Pharmaceutical Services duty roster in its own PostgreSQL database (`pharmacies`, role `pharmacies_app`), created by Liquibase. The API and the Pharmacies page show entries for the selected date, defaulting to the current roster in Cyprus. The page links to address directions and the pharmacy's public telephone number; house numbers are never stored.

Set `PHARMACY_SYNC_ENABLED=true` to enable automatic checks. The service reads the five district CSV links from the official catalog metadata (`PHARMACY_CATALOG_METADATA_URL`), downloads only from that origin and replaces the stored roster when all five files are valid. It checks at startup if the last successful check is older than one day, then daily at 06:15 Cyprus time (see [Refresh schedule](#refresh-schedule)). A failed check keeps the stored roster. Each district's schedule is replaced from the first day its new file covers, so earlier days remain available by date. The current roster rolls over at 08:00 Cyprus time; late-evening and overnight entries are marked on call for urgent prescriptions.

Volumes created before the pharmacies database existed need it added once. Set `PHARMACIES_DB_PASSWORD` in `.env`, then run `infra/postgres/002-pharmacies.sql` as the admin user (the command is in the file).

For Railway, provision the restricted pharmacies role and schema the same way as Parking and configure `PHARMACIES_JDBC_URL`, `PHARMACIES_DB_USER`, `PHARMACIES_DB_PASSWORD` and optionally `PHARMACIES_DB_SCHEMA` (default `pharmacies`). The service no longer uses a snapshot file or volume.

Map points are stored in `pharmacies.pharmacy_location` by registration number with a `precision` (`exact`, `street` or `locality`) and a required `source`. Load or replace them from a CSV with `scripts/import-pharmacy-locations.sh` (header `registration_number,latitude,longitude,precision,source`; usage is in the script). Only use coordinates whose source permits storing and showing them on an OpenStreetMap map. The page shows coordinate coverage, marks approximate points and sorts mapped entries by straight-line distance on request.

# Cyprus Open

Public statistics and everyday tools for people living in Cyprus.

[Live preview](https://cyprus-open.up.railway.app) · [Source catalog](https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset)

React/TypeScript with EN/RU/EL interfaces, independent Spring Boot Statistics and Parking services, and a searchable map of 651 verified parking locations. Statistics charts use official CYSTAT tables; live parking occupancy is not connected yet.

## Local development

Use Docker Desktop. Preserve the existing `.env`; for a new checkout copy `.env.example` and set three distinct database passwords. Run:

```sh
docker compose up -d --build frontend statistics-service parking-service
```

Open http://localhost:3000. Parking now requires PostgreSQL; Compose starts it automatically and preserves the existing volume. The Parking role owns its database/schema, separately from Statistics. Liquibase owns schema migrations. Never remove the database volume to update the app.

Java 25, Spring Boot 4.1.2, Maven Wrapper, Node 24, React, TypeScript, Vite and pnpm 11.25.0. Build/test independently:

```sh
./mvnw -B -ntp -pl backend/parking-service -am verify
./mvnw -B -ntp -pl backend/statistics-service -am verify
cd frontend
pnpm install --frozen-lockfile
pnpm build
pnpm test:e2e
```

Parking integration tests use an isolated PostgreSQL Testcontainer and a local HTTP server. Browser CI uses `docker-compose.test.yml` with a private XML fixture server; the real refresh job fills the database from that server. These small English-only protocol fixtures are never a production data source. No government or tile-provider requests leave CI. Docker image packaging skips tests that require a Docker daemon; CI runs those integration tests separately before review.

For Vite development, run the backends locally and `pnpm start` alongside `pnpm dev` for runtime map configuration. The production frontend listens on `PORT` (3000 by default), proxies `STATISTICS_UPSTREAM` and `PARKING_UPSTREAM`, and exposes `/healthz`. Backend readiness is `/actuator/health/readiness`.

## Parking data and refresh

The [CYNAP / Public Works Department directory](https://www.traffic4cyprus.org.cy/en/dataset/parking_areas) links to the [DATEX II XML resource](https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/) under CC BY 4.0. It contains locations, not occupancy, prices, opening hours or verified entrances. Its declared publication frequency is less frequent than yearly. Some entries originate from OpenStreetMap. Missing English names and OSM-origin placeholders display as unnamed car parks; source identifiers and coordinates are preserved. Area filters are approximate viewports, not municipal boundaries.

PostgreSQL holds the latest normalized directory document (JSONB) and sync metadata in `parking.parking_directory`. One atomic document matches the upstream publication and current whole-directory API; no per-location joins or occupancy history are needed. `JdbcClient` serves persisted data. There is no bundled JSON or seed data. A new database stays empty until the shared HTTP refresh job loads its first successful publication. The same job handles every later update; no source-file edits or rebuild are needed.

The UI distinguishes the source publication timestamp, successful retrieval and last successful check. Displayed dates use Cyprus time (Asia/Nicosia). Publication time is the timestamp on the source's XML package, not a verified modification time for every parking location. On HTTP 304 only the successful-check time changes. Failed requests change attempt/status metadata and preserve the previous document and successful-check date.

Automatic checking is **disabled by default**. When enabled, the shared job also runs after startup if the directory is empty. Setting `PARKING_DIRECTORY_SYNC_ENABLED=true` enables one check at **03:00 UTC on the first day of each month**. This conservative internal schedule is for the slow-changing directory, not live occupancy. It is not a provider-issued quota. A manual one-shot check uses the same guarded code after PostgreSQL is running:

```sh
docker compose run --rm --no-deps parking-service --spring.profiles.active=directory-refresh
```

The job uses Spring `RestClient`, conditional ETag/Last-Modified headers, server-configured endpoint (the verified official HTTPS resource by default), no redirects, 5-second connection / 15-second read timeout, and a 2 MiB response limit. The job makes one request per run. If the source is unavailable or returns invalid data, the job logs the outcome and keeps the last successful snapshot. Only server configuration supplies the endpoint; user API input cannot change it. Tests replace that configuration with their HTTP server, without test flags or hostnames in production code. There is no public refresh endpoint. User API requests never call upstream. The browser polls our API every 10 seconds while the directory is empty and every 5 minutes after initialization.

A valid, non-empty publication replaces the previous directory atomically. Invalid XML/DOCTYPE, duplicate identifiers, invalid coordinates or older publication times preserve the previous directory. A 304 preserves publication/retrieval times. Database migrations never reset existing directory data.

The owner's [Nicosia availability dataset](https://www.data.gov.cy/el/dataset/trehoysa-diathesimotita-horon-stathmeysis-dimoy-leykosias) points to [RSS XML](https://www.nicosia.org.cy/el-GR/rss/parkingspaces/). On 4 November 2026 all four observations were dated 5 December 2022, with negative counts also present. Those are not live readings: the API keeps occupancy freshness unavailable and counts/timestamp null. Research into usable live sources is tracked in issue #13; implementation remains in #6.

## Statistics data and refresh

Charts read official [CYSTAT](https://www.cystat.gov.cy) tables, linked from data.gov.cy under CC BY 4.0: [tourist arrivals by country of usual residence, monthly](https://www.data.gov.cy/en/dataset/2285), [immigrants by citizenship, country of birth and previous residence, annual](https://www.data.gov.cy/en/dataset/2174) and [migration movements, annual](https://www.data.gov.cy/en/dataset/2172). The immigrant table publishes only Cyprus / EU / non-EU groups, not individual countries; the UI says so instead of substituting another measure.

`statistics-service` downloads each whole table as JSON-stat 2.0 from the PxWeb API (`STATISTICS_CYSTAT_API_URL`, default `https://cystatdb.cystat.gov.cy/api/v1/en/8.CYSTAT-DB/`). Each dataset class only maps table categories to series keys; fetching, parsing, storage and refresh are shared, so a new chart needs one small class. Observations live in `statistics.dataset` and `statistics.observation`; a table is replaced atomically and only when CYSTAT reports a newer update time. A failed or invalid table keeps its last stored values. Country series use ISO codes so the browser localizes names.

Automatic refresh is **disabled by default**. `STATISTICS_SYNC_ENABLED=true` imports missing tables on startup and checks every **Monday at 04:30 Cyprus time**. User requests never call CYSTAT. Integration tests and browser CI use local JSON-stat fixtures.

## Maps and privacy

Leaflet with clustered markers uses standard OSM tiles, visible attribution and ordinary browser caching, without prefetch. Runtime `MAP_TILE_URL`, `MAP_ATTRIBUTION` and `MAP_ATTRIBUTION_URL` can change the provider without rebuilding. Use HTTPS and respect provider terms; this public client configuration must not contain secrets. The list and Google Maps directions remain usable if tiles fail.

Near me requests location only on click, calculates straight-line distance with Leaflet, and keeps coordinates in tab memory. Location is not sent to our API or stored. The tile provider sees the viewed area. Google directions links need no key; embedding Google Maps would require a separately configured API key and billing.

## Railway

Keep repository root as the build context. Service settings are managed directly in Railway; obsolete legacy TOML config paths are removed.

| Service | Dockerfile | PORT | Health check |
| --- | --- | --- | --- |
| Frontend | `frontend/Dockerfile` | 3000 | `/healthz` |
| Statistics | `backend/statistics-service/Dockerfile` | 8081 | `/actuator/health/readiness` |
| Parking | `backend/parking-service/Dockerfile` | 8082 | `/actuator/health/readiness` |

Before deploying Statistics, configure `STATISTICS_JDBC_URL`, `STATISTICS_DB_USER`, `STATISTICS_DB_PASSWORD` and `STATISTICS_SYNC_ENABLED=true`; the restricted role and schema come from `infra/postgres/001-databases.sql`.

Before deploying the new Parking service, provision its restricted database role and schema and configure `PARKING_JDBC_URL`, `PARKING_DB_USER`, `PARKING_DB_PASSWORD` and optionally `PARKING_DB_SCHEMA` (default `parking`). Database secrets stay outside Git. Production database provisioning/deployment is not performed by this PR. Set `PARKING_DIRECTORY_SYNC_ENABLED=true` on the Railway Parking service for the release. On startup, Liquibase prepares the database and Parking imports the official directory if it is empty. Verify that `/api/v1/parking` returns items and a directory check timestamp. If the initial import fails, inspect the Parking logs before restarting the service to retry.

Deploy the updated Parking backend together with the frontend release after review. The frontend currently follows GitHub main; Parking currently uses a manual source. The new frontend handles the old placeholder API as a retryable error. Only the frontend needs a public domain; set its upstreams to the actual Railway private service origins with their ports.

Publish feature branches through PRs; the owner controls merging. README is published in English; other Markdown stays local. Interface translations are an owner-approved exception to English-only repository content.

## Shared map and parsing components

`frontend/src/components/map/MapView.tsx` accepts plain labelled coordinates, bounds, selection, marker symbol and labels. Tile loading, clustering, camera/resize handling and user-location display are shared. ParkingMap only adapts parking records and translations. Shared viewport presets, distance calculations and explicit-action geolocation live in `geo/` and `hooks/useBrowserLocation.ts`; map sizing can use `--map-height` and `--map-mobile-height`.

`backend/common-library` provides the framework-free `XmlDocumentReader`: bounded namespace-aware XML reading, disabled DTD/external resources and required-field extraction. Parking supplies it through Spring DI; DATEX parking fields and validation stay in DirectoryXmlParser. Other XML resources can reuse the reader; CSV/JSON resources need their own format-specific parsing. Parking builds the shared JAR through the root `-pl ... -am` command; no external artifact registry is needed.

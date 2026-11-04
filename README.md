# Cyprus Open

Public statistics and everyday tools for people living in Cyprus.

The first release includes a responsive English interface, Statistics and Parking APIs, source links, health checks and Railway-ready Docker builds. Dataset imports and live availability are not connected yet; the site displays explicit unavailable states rather than sample readings. Pharmacies are planned.

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

## Railway

Create three separate services from this repository after deployment approval. Keep the repository root as build context and set each service's config path to `/frontend/railway.toml`, `/backend/statistics-service/railway.toml`, or `/backend/parking-service/railway.toml`. These select their corresponding Dockerfiles.

Set backend `PORT` to 8081 for Statistics and 8082 for Parking. Set the frontend upstream variables to the corresponding Railway private HTTP origins including those ports; use the actual internal hostnames assigned to your services. Only the frontend needs a public domain. The frontend honors Railway's injected `PORT`. No scheduler or external data fetching is implemented or enabled in this release.

Submit related changes together through a PR. The owner reviews and merges; never push directly to main. README is published; other Markdown documentation stays local. The primary data catalog is [data.gov.cy](https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset).

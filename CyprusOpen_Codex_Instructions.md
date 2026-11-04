# Cyprus Open — Codex project instructions

## Mission
Build **Cyprus Open**, a free, public, responsive web application for people living in Cyprus. The website has a **Statistics** section with engaging interactive charts based on official public datasets, and separate mini-app sections. The first mini-app is **Parking** (live available spaces on a map); **On-duty Pharmacies** comes later.

This file is the initial project specification and ongoing instructions for Codex. Work incrementally, favor a simple working product, and document decisions. Do not invent government endpoints, dataset fields, coordinates, prices, or live availability.

## Non-negotiable principles
1. **Protect government infrastructure.** Never scrape aggressively, crawl entire catalogs, or generate load tests against government websites. Inspect sources manually/with minimal requests first. Check official terms, robots.txt where relevant, licenses, and any published API limits. A public dataset listing does not automatically grant unlimited API access.
2. **All public visitors use our backend**, never government APIs directly. No user registration or accounts in the MVP.
3. **PostgreSQL for official historical/statistical and relatively static reference data**, not for unnecessary tracking. Do not record page views or build our own parking occupancy history yet.
4. **In-memory Caffeine cache for live data** such as current parking availability. Display freshness timestamps and clearly flag stale information.
5. **Monorepo with multiple independent backend projects and a small shared library, not a monolith.** Keep frontend and each backend service as separate, independently buildable, runnable, testable and deployable projects in one Git repository. Start with distinct `statistics-service` and `parking-service`; add `pharmacies-service` when that feature is implemented. Avoid needless additional infrastructure or services. No Kafka, RabbitMQ, Redis, Kubernetes, or complex platform services in the MVP.
6. **All project code and documentation must be version-controlled in GitHub.** Commit coherent changes and push to the configured GitHub remote after each completed, validated milestone. Never force-push, overwrite unrelated changes, commit secrets, or silently create a public repository. If no GitHub remote/authentication exists, stop and ask the user to configure/authorize it; continue local work only if appropriate. Never claim a push succeeded without verifying it.

## Product and navigation
- `Statistics` (main landing section): attractive, interactive charts grouped by topics (the two mandatory launch charts: immigration by country of origin and year; tourist arrivals by country and season). Start with the **two mandatory charts** below, using verified datasets; do not build a generic ingestion platform.
- `Parking`: map and list of participating municipal parking lots, latest reported free-space counts, last updated time, and clear unavailable/stale states. Initial source candidate: Cyprus Open Data municipal parking dataset for Nicosia; confirm actual coverage and data fields before coding an integration.
- `Pharmacies` (phase 2): on-duty pharmacies, location, contact information and duty times if verified in an official source. Freshness and accuracy are critical; do not suggest a pharmacy is currently on duty based on outdated data.
- `About / Data sources`: link to each original source, licensing, update frequency, last successful sync and limitations.
- Full English, Russian, and Greek localization from the MVP, with English as the first-visit default. Mobile-first, accessible, fast-loading design.

## Preferred technology
- **Backend services:** Java 25 if supported by the selected stable Spring Boot version and build environment; otherwise Java 21 LTS. Spring Boot, Spring MVC, Spring `RestClient`, Spring Data JPA, PostgreSQL, Liquibase, Spring Scheduler, Caffeine, Jackson (including XML only if source requires it), JUnit 5 and Testcontainers for essential integration tests only.
- **Frontend:** React, TypeScript, Vite, Tailwind CSS, shadcn/ui, Apache ECharts, Leaflet, TanStack Query.
- **Infrastructure:** Docker Compose for local development (backend, frontend as needed, PostgreSQL), Railway for all production deployments, GitHub Actions for build/test checks. Avoid premature cloud/platform complexity.
- **Repository:** one monorepo containing a non-deployable `backend/common-library/` Maven module and independent `backend/statistics-service/`, `backend/parking-service/`, future `backend/pharmacies-service/`, and `frontend/` projects, plus `docs/`, `docker-compose.yml`, `.github/workflows/`, `README.md`, and this `AGENTS.md`. Each project owns its dependencies, build, configuration, and tests; do not combine their builds into a single application artifact.

## Code quality and engineering style
- Follow established Java, Spring Boot, TypeScript and React best practices. Write maintainable, idiomatic, production-quality code that looks like it was thoughtfully written and reviewed by an experienced human developer.
- Prefer clear names, straightforward control flow, cohesive components and small focused classes/functions. Use composition over unnecessary inheritance and abstractions. Follow SOLID principles pragmatically, without overengineering.
- Avoid boilerplate, speculative generalization, needless interfaces, excessive layers, duplicated logic, magic constants and giant utility classes. Introduce abstractions only when they solve a real problem.
- **Do not comment every line or narrate obvious code.** Prefer self-explanatory code and no comments by default. Add concise comments only to explain non-obvious business rules, subtle trade-offs, surprising behavior or safety-critical constraints. Avoid generated-looking commentary and decorative section comments.
- Use consistent formatting, clear error handling, validation and structured logging. Never silently swallow errors or log secrets.
- Keep pull-request-sized changes coherent; remove dead code, unused imports and temporary scaffolding before committing.

## Frontend engineering and design standards
- Apply the same high code-quality bar to frontend as backend: idiomatic, readable React and TypeScript, consistent naming, predictable data flow, and small cohesive components. Write code as an experienced human engineer would, not as a generated template.
- Build **reusable components where reuse is real**: shared page layouts, navigation, buttons, form controls, loading/empty/error states, data-source attribution, freshness indicators, chart containers and map controls. Prefer composition and simple props over inheritance, configuration-heavy component factories or premature design systems.
- Keep feature-specific components close to their features. Extract a shared component or hook when it demonstrably reduces duplication or improves consistency; do not abstract one-off UI. Avoid deep component nesting, excessive custom hooks, generic wrappers, redundant state layers and unnecessary dependencies.
- Use TypeScript types that model actual API responses and UI states; avoid `any` and speculative generic types. Keep API calls and server-state management in focused modules with TanStack Query. Do not duplicate server state in local stores or introduce Redux/Zustand without a concrete need.
- Handle loading, empty, error, stale and partial-data states deliberately. Use accessible semantic HTML, keyboard navigation, visible focus, meaningful labels, sufficient contrast and responsive layouts. Charts and maps need useful text equivalents or accessible summaries where practical.
- **Design must be beautiful, simple, warm and human.** Aim for a polished, trustworthy civic-data product rather than a corporate dashboard or generic AI-generated landing page. Favor clear visual hierarchy, generous but purposeful whitespace, restrained color, excellent typography, legible charts, intuitive navigation and subtle interactions.
- Establish a small coherent set of design tokens (spacing, typography, colors, radii) and use Tailwind/shadcn/ui pragmatically. Customize components for a consistent identity; do not pile on gradients, shadows, animations, cards, icons or decorative elements without purpose.
- Prioritize mobile usability and real user tasks: quickly understand a statistic, see when data was updated, or find parking. Keep key information above the fold where sensible, avoid visual clutter, and make official source attribution easy to find.
- **Do not add explanatory comments to obvious JSX, hooks or styles.** Prefer clear component names and straightforward implementation. Add comments only when necessary to explain a non-obvious constraint or decision.
- Keep frontend testing consistent with the project policy: a few high-value integration/end-to-end flows using real app components and a controlled local backend/test environment; no unit tests, object mocks, or calls to live government sites.

## Testing policy — integration tests only
- **Do not write unit tests.** Do not create tests merely to inflate coverage; prioritize a small number of high-value integration tests for critical behavior and failure modes.
- **Do not use mocks or mocking frameworks** (including Mockito, `@MockBean`, `@MockitoBean`, spies or hand-written fake service objects). Exercise real application components together.
- For backend integration tests, prefer `@SpringBootTest` with real PostgreSQL via Testcontainers and real migrations. For upstream HTTP boundaries, use a local HTTP stub server with deterministic fixture responses; this simulates the external protocol without mocking application objects.
- Test the most important flows: statistical import and idempotency, persisted data served through the API, parking refresh and stale/error behavior, and protection against uncontrolled upstream requests.
- Frontend: add only a few valuable end-to-end/integration tests for critical user journeys if warranted; avoid shallow component/unit tests.
- CI tests must be deterministic, independent of government service availability and must never make real requests to government websites. Make outbound government access disabled by default in test configuration.
- If a meaningful integration test is too expensive or brittle, explain the trade-off instead of adding low-value tests.

## Monorepo boundaries and service architecture
- **One GitHub repository, multiple independent applications, not a monolith or one multi-module deployable.** Each backend service is a separate Spring Boot application with its own `pom.xml`, configuration, Dockerfile, tests, and runtime process. Do not implement Statistics, Parking and Pharmacies as packages inside one Spring Boot application.
- Start with `backend/statistics-service/` and `backend/parking-service/`. Add `backend/pharmacies-service/` when implementing pharmacies. Keep `frontend/` as an independent React/Vite project.
- Each service owns its data and APIs. `statistics-service` owns official statistics importers, dataset sync metadata, and statistical PostgreSQL tables. `parking-service` owns parking source integration, refresh and Caffeine cache, plus optional static parking metadata. `pharmacies-service` will own pharmacy data and duty schedules.
- A **single PostgreSQL server/container** is fine for the MVP, but use **separate databases or separate schemas with distinct database users and migration histories per service**. No cross-service table access or shared JPA entities. Do not introduce separate database servers without a reason.
- Frontend can call each backend through one reverse proxy routing `/api/v1/statistics/**` and `/api/v1/parking/**` to the respective services (and later `/api/v1/pharmacies/**`). No API gateway product is required; use a simple reverse proxy and consistent routing.
- Include `backend/common-library/` as a **small, non-deployable Maven JAR module**. It is a library, not a Spring Boot application, microservice, or runtime process. Backend services may depend on it as a regular Maven dependency; they must remain independently runnable and deployable.
- A root `pom.xml` may serve as a Maven aggregator (and optionally a minimal parent for build conventions) so all backend modules can be built together. **Never** combine backend services into a single deployable artifact. Each service must also support its own build/test/deploy workflow, including resolution of the shared library from the monorepo build or a local Maven repository; do not require a remote artifact registry for the MVP.
- **Do not preemptively fill the shared library.** Extract code only when it is genuinely reused by at least two backend services or is an established cross-cutting contract. A little duplication is preferable to speculative abstractions.
- Good candidates *when genuinely shared*: outbound government HTTP client defaults (timeouts and bounded responses), upstream rate-limit/retry safeguards, consistent API error format, reusable logging configuration, and narrowly scoped utilities.
- **Never** put service-specific business rules, JPA entities, repositories, database migrations, dataset DTOs, or service-specific configuration in the shared library. Avoid a giant `utils` package, unnecessary interfaces, broad starter frameworks, or shared mutable state.
- Keep dependencies lightweight and avoid forcing every consumer to import JPA, web, or other frameworks it does not need. Shared components must not couple services to each other's data models or runtime availability. Do not require services to call one another for basic reads.
- Each service must build, test, start and deploy independently. CI should validate changed projects independently, with path filters where practical. Root Docker Compose orchestrates local development only. Production deployment targets Railway.

```text
cyprus-open/
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── pom.xml                    # Maven aggregator for backend modules
├── .github/workflows/
├── docs/
├── frontend/                  # Independent React + Vite app
│   ├── package.json
│   └── Dockerfile
└── backend/
    ├── common-library/        # Reusable non-deployable Maven JAR
    │   ├── pom.xml
    │   └── src/main/java/
    ├── statistics-service/    # Independent Spring Boot application
    │   ├── pom.xml
    │   ├── Dockerfile
    │   └── src/
    ├── parking-service/       # Independent Spring Boot application
    │   ├── pom.xml
    │   ├── Dockerfile
    │   └── src/
    └── pharmacies-service/    # Add in later milestone
        ├── pom.xml
        ├── Dockerfile
        └── src/
```

## Data lifecycle and storage
### Official statistics / slow-changing data
- Each verified dataset gets a small dedicated importer and domain model/table; avoid a universal key-value metric table or a generic ETL framework.
- Import into PostgreSQL; the frontend reads **only our API**, and the API serves stored data.
- Synchronize on a conservative schedule matched to the source's **actual** publication cadence. Proposed initial checks (NOT facts about the sources): annual datasets monthly, monthly datasets weekly, daily datasets daily. Do not implement schedules before verifying metadata.
- Store per-source sync metadata: `dataset_code`, source URL, `last_checked_at`, `last_successful_sync_at`, optional ETag / Last-Modified, sync status and concise error metadata.
- Prefer conditional GET (`If-None-Match`, `If-Modified-Since`) when supported; otherwise compare metadata or checksums where feasible. Avoid downloading unchanged large files. Make imports idempotent and transactional; do not delete valid existing data on failed imports.
- No custom analytics, visit tracking, or self-collected parking history in MVP.

### Live parking
- Read verified official endpoint using a scheduled refresh, **not one request per user**.
- Start with a conservative refresh interval (e.g. 5 minutes **only if permitted and appropriate**; otherwise longer). Keep only the latest successful response in Caffeine; use stale-while-error with a clearly visible freshness warning.
- Static parking metadata (name, coordinates, capacity) may be stored in PostgreSQL if useful.
- Never infer zero free spaces from missing data. Missing/stale != full.

### Pharmacies (later)
- Confirm official source, duty schedule and update frequency before implementation. Cache with a source-appropriate TTL. Show source and last update. Do not equate a registered pharmacy with one currently on duty.

## Strict external-request safeguards
- Centralize all outbound government HTTP access through one configurable client/policy.
- Default to **one in-flight request per upstream host** and at least **5 seconds between requests to the same host** as a conservative internal starting point; obey stricter published restrictions. These are *not* government-approved quotas.
- Apply connection/read timeouts, bounded response sizes, descriptive User-Agent with project/contact information once available, and low total request budgets per job.
- No unbounded retries. On 429/503 honor `Retry-After`, use limited exponential backoff with jitter, and pause further syncs after repeated failures. Never bypass throttling by rotating IPs or identities.
- Avoid concurrent import jobs to the same host. For a single-instance MVP, one scheduler is enough; if scaling instances, introduce coordination (e.g. ShedLock) before enabling multiple schedulers.
- Add a feature flag / configuration switch to **disable all external fetching** in local development and tests. Use deterministic local fixture files and a real local HTTP stub server (for example WireMock) in integration tests; **do not use Mockito or other object mocks, and never run automated tests against government endpoints**.
- Log upstream request count, last successful refresh, failures and staleness, without logging credentials or excessive response payloads.

## API outline (adjust to real data)
- `GET /api/v1/statistics/datasets`
- `GET /api/v1/statistics/{datasetCode}` (or explicit endpoints when clearer)
- `GET /api/v1/parking`
- `GET /api/v1/parking/{id}`
- Future: `GET /api/v1/pharmacies/on-duty`
- Every response that relies on externally refreshed data should expose `source`, `lastUpdatedAt` and a clear freshness/stale indicator where applicable.
- Public read-only API: sensible caching, pagination where needed, rate limiting at the public edge if necessary, input validation and CORS limited to intended frontend origins.

## Suggested backend package layout
Use separate base packages per service, e.g. `com.cyprusopen.statistics` inside `statistics-service` and `com.cyprusopen.parking` inside `parking-service`. When introduced, `pharmacies-service` uses `com.cyprusopen.pharmacies`. Within each service, separate API, application logic, source client/importer, DTOs, and persistence only as warranted. Use `backend/common-library` only for genuinely shared cross-cutting code; keep all domain-specific code inside its owning service.

## Initial implementation milestones
**Milestone 0 — Repository and source validation**
1. Inspect existing repository state and GitHub remote. Never overwrite existing work.
2. Create README, architecture notes, safe `.gitignore`, `.env.example`, root Maven aggregator, an initially minimal `backend/common-library` JAR module, and this specification.
3. Verify **one parking dataset and one statistical dataset** using the fewest possible official requests. Record actual URL, format, fields, update cadence, terms/license, and whether authentication is required in `docs/data-sources.md`. If access is uncertain, use fixtures and explicitly mark integration as pending; do not guess.
4. Commit and push to the configured GitHub remote, verifying success.

**Milestone 1 — Working skeleton**
1. Independently runnable `statistics-service` and `parking-service` Spring Boot projects, a small reusable `common-library` JAR + React frontend + one PostgreSQL instance with service-isolated schemas/databases and Liquibase changelogs + local Docker Compose.
2. Basic navigation: Statistics, Parking, Pharmacies (clearly marked Coming soon), About.
3. Health endpoint and configuration per backend service, essential integration tests, independent GitHub Actions CI jobs.
4. External-fetch-disabled mode and representative fixtures.
5. Commit, test and push.

**Milestone 2 — Statistics MVP**
1. Identify and validate official source datasets for **both mandatory charts** described below. Implement their importers and normalized PostgreSQL tables; if a required dataset cannot be verified, document the blocker and do not fabricate values.
2. Add sync metadata and safe manual/scheduled refresh (conservative cadence).
3. Serve data through `statistics-service`; implement **both mandatory charts** with source, period, units, coverage and last-update labels.
4. Add essential integration tests covering idempotent import, invalid data handling, persistence and zero external network calls in CI.
5. Commit, test and push.

**Milestone 3 — Parking MVP**
1. Implement source adapter in `parking-service` for verified parking data, rate-limited scheduled refresh, Caffeine cache and stale handling.
2. Display parking locations on Leaflet map and a mobile-friendly list, with last update.
3. Add integration tests against a local HTTP stub server with recorded/synthetic fixture responses; do not fabricate live readings in the actual product.
4. Commit, test and push.

**Milestone 4 — Pharmacies**
Only after a trustworthy, usable official source has been confirmed; create a separate `pharmacies-service` Spring Boot project, not a module within an existing service. Commit, test and push.

## Production hosting: Railway (mandatory)
- **Railway is the production hosting platform for the entire application.** Do not design production deployment around VPS, Kubernetes, Vercel, Render, Fly.io, or Docker Compose. Docker Compose remains for local development only.
- Deploy `frontend/`, `backend/statistics-service/`, `backend/parking-service/`, and later `backend/pharmacies-service/` as **separate Railway services** in one Railway project where practical. Provision PostgreSQL through Railway; use one PostgreSQL instance initially, with logical separation between backend services where needed.
- The `backend/common-library/` module is a build dependency, **not** a Railway service. Ensure each backend Docker build can access and compile this shared module from the monorepo using the correct build context and Dockerfile paths. Do not assume Railway builds run from `backend/<service>/` with access to parent files unless explicitly configured.
- Each service must have an independently reproducible build, start command, health check, and environment-variable configuration. Spring Boot applications must bind to Railway's injected `PORT` (with a local fallback) and listen on `0.0.0.0`; frontend must be served by a production-ready HTTP server configured for Railway's port.
- Configure Railway networking correctly: public frontend entry point, backend access via a deliberate proxy/API routing strategy, and private service-to-service connectivity where appropriate. Avoid accidentally exposing databases or internal services publicly. Handle CORS only if the chosen routing requires it.
- Use Railway-provided PostgreSQL connection variables and secrets via environment variables, never hardcoded credentials or committed `.env` files. Apply Liquibase migrations safely on service startup or via an explicitly controlled migration step; do not let multiple services race to modify the same schema.
- Account for Railway's ephemeral application filesystems: never rely on local files for persistent state. Store durable statistics and sync metadata in PostgreSQL; keep transient caching in memory.
- Keep services economical: conservative resource usage, no unnecessary always-on workers, no aggressive polling, and no infrastructure that is not needed for the MVP. Preserve government-source rate limits even when multiple instances or deployments exist; prevent duplicate scheduled imports (prefer a single designated scheduler instance or a PostgreSQL-backed distributed lock if scaling becomes necessary).
- Document exact Railway setup, service root/build context, Dockerfile path, start/health settings, environment variables, database wiring, domain/API routing, and deployment steps in `docs/railway.md` and README. Include a safe deployment checklist and how to roll back.
- GitHub remains the source of truth: commit and push each milestone. Prepare for Railway GitHub-based deployments, but **do not activate automatic production deployments, create paid resources, or deploy without explicit user authorization**. Once authorized, favor independent service deployments from the same monorepo and avoid rebuilding unrelated services when possible.

## Documentation, README and database diagrams (mandatory)
- Write documentation **briefly, precisely, and like a human engineer**. Prefer short practical explanations, examples and clear decisions over boilerplate, generated filler, exhaustive inventories, or repeating what the code already says. Keep it current whenever behavior, configuration, architecture or schema changes.
- The root `README.md` must be **very concise and welcoming**: one-sentence project purpose, what currently works, a compact stack summary, minimal local setup/run commands, links to deeper documentation, and a brief contribution/deployment pointer. No walls of text, excessive badges, generic marketing copy, or duplicated API reference.
- **`docs/` is the single source of truth for all technical documentation.** Maintain documentation in the main repository alongside the code; do not edit Wiki pages independently or maintain two divergent copies.
- **Publish selected `docs/` pages to GitHub Wiki automatically through GitHub Actions** after successful pushes to the default branch. GitHub Wiki is a separate Git repository (`<repo>.wiki.git`), so pushing the main repository alone does not update it. Set up a small, maintainable sync workflow that publishes only intended Markdown pages, preserves stable page names/links, and avoids accidentally deleting manually created Wiki content. Keep the workflow simple; do not introduce third-party actions unless justified.
- Before enabling publishing, **verify Wiki is enabled and initialized** (an initial page may need to be created manually), and that the workflow has suitable authenticated write access to the Wiki repository. Do not assume the default `GITHUB_TOKEN` can push to the Wiki; verify permissions and use an appropriately scoped secret/token if required. Never commit tokens, print credentials, or silently weaken repository security.
- If Wiki access or credentials are unavailable, keep documentation updated in `docs/`, prepare the sync workflow and a short setup guide, and explicitly report that Wiki publishing is pending. Do not block feature development or falsely claim the Wiki was published.
- Keep Wiki documentation short, human-written, and useful: architecture, service boundaries, verified data sources, local development, and Railway deployment. README stays exceptionally concise and links to the documentation. Update docs in the same commit as code; GitHub Actions should publish the committed version, not a separate hand-edited copy.
- Maintain `docs/architecture.md` with a small, accurate service overview and one Mermaid flow diagram; `docs/data-sources.md` for verified official sources and safe refresh policies; and `docs/railway.md` for practical deployment instructions. Create other docs only when genuinely needed.
- **Database schema must be visualizable from text.** Keep Liquibase changelogs as the executable source of truth. Also maintain `docs/database.dbml` in **DBML format**, compatible with dbdiagram.io, containing tables, columns, primary/foreign keys, indexes and relationships for the current PostgreSQL schema. Include schema names where relevant. Update DBML in the same change as every schema migration; never let the diagram drift from the actual database.
- Optionally embed a small Mermaid `erDiagram` in `docs/database.md` or the Wiki for quick GitHub previews, but **DBML is the primary portable visualization format**. Include a short instruction explaining that `docs/database.dbml` can be pasted into dbdiagram.io for visualization. Do not create fictional tables, columns, or relationships just to populate a diagram.
- Keep database ownership clear between independent backend services. A shared PostgreSQL instance does not justify cross-service JPA relationships or tight coupling. Document any intentional cross-service references without introducing database foreign keys across service boundaries by default.
- Documentation and DBML updates are part of the definition of done; commit and push them together with the corresponding code. If Wiki publishing is configured, verify the GitHub Actions documentation-sync workflow succeeds after pushing; if not, state what is pending.

## GitHub workflow and safety
- At the beginning, run `git status`, `git branch --show-current`, and `git remote -v` (do not expose credential-bearing remote URLs in user-facing output).
- If repository is absent, initialize locally; **ask for GitHub repository URL and authorization before creating or pushing to a remote**. Respect the user's preferred default branch and repo visibility.
- Use descriptive, scoped commits such as `chore: bootstrap project`, `feat(statistics): import reservoir data`, `feat(parking): show live availability`.
- After each milestone: run tests/builds, inspect `git diff`, `git status`, commit only intended files, `git push` to the configured remote/branch, and verify remote tracking state.
- Do not add GitHub credentials to files or logs. Use GitHub Actions for CI. Prepare Railway-compatible deployment configuration, but do not create Railway projects, connect accounts, provision paid resources, or deploy without explicit authorization.
- If a push is rejected, investigate safely and report the blocker; never force-push or rewrite shared history.

## Definition of done for each feature
- Works with real verified source data or clearly labeled local fixtures.
- Does not make excessive upstream requests; refresh interval and request limits are documented.
- Handles network failures and stale data transparently.
- Responsive UI with loading, empty and error states.
- Tests pass; concise README/docs and DBML (when schema changes) updated; changes committed and **successfully pushed** to GitHub (or explicitly reported as blocked).

## Start now
Begin with Milestone 0. First inspect the repository and available remote. Then validate the official data sources conservatively, document findings, and scaffold only what is justified. Ask the user for missing GitHub repo/auth details if needed. Do not silently invent APIs or deploy anything without authorization.

## Natural, human engineering style (mandatory everywhere)
- **Avoid AI-sounding language in every project artifact**, not just documentation: source code, identifiers, comments, commit messages, pull request descriptions, issue titles, UI copy, error messages, logs, tests, README, Wiki, and configuration examples.
- Write as a thoughtful human engineer would: direct, specific, concise, and natural. Prefer plain language over inflated, promotional, robotic, or formulaic phrasing.
- No generic filler such as "seamless experience", "robust and scalable solution", "leverage cutting-edge technology", "comprehensive implementation", or repetitive summaries and self-congratulatory statements.
- Name classes, methods, variables, files, and commits for what they actually do. Avoid grandiose names, artificial patterns, unnecessary abstractions, and verbose naming that adds no clarity.
- **Do not narrate the code.** Comments and Javadoc are optional, not a quota. Add them only to explain non-obvious reasoning, constraints, workarounds, or important trade-offs; never describe what an obvious line or method does. Do not insert decorative section separators or generated-looking headers.
- UI text should sound warm, clear, and useful, never corporate or synthetic. Use concise labels and helpful, actionable error messages; do not overdo emojis, exclamation marks, or marketing copy.
- Tests should have short, meaningful names and assertions that reflect actual behavior. Avoid ceremonial test scaffolding or explanatory comments that repeat the implementation.
- Do not mention that content was generated by AI or add AI signatures, attribution, boilerplate disclaimers, or "generated by" banners unless explicitly requested or legally required.
- Before committing, review newly written text and code for unnatural wording, redundant comments, and needless complexity. Edit it until it reads like a careful human contribution. This is a style and quality requirement, not a request to conceal facts or misrepresent authorship when disclosure is required.

## Internationalization (i18n)
- Support **English (`en`), Russian (`ru`), and Greek (`el`)** from the first frontend milestone. **English is the default on a first visit**, regardless of browser language.
- Use `i18next` and `react-i18next`; keep translations in `frontend/src/locales/en.json`, `ru.json`, and `el.json` (or a small, consistently organized namespace structure if the files grow). Do not build a custom translation framework.
- Provide a simple, accessible language switcher labeled **EN / RU / EL** (avoid country flags). Save explicit language choices in `localStorage` and restore them on subsequent visits. If there is no saved choice, start in English. Fall back to English for missing keys.
- Keep **all user-facing text** localized: navigation, headings, buttons, form labels, validation and error messages, empty/loading states, map controls and legends, chart titles, axes, tooltips, filters, data-source notices, update timestamps, and accessibility labels. Avoid hardcoded UI strings in components.
- Use `Intl` with the active locale for dates, numbers, percentages, and units where appropriate. Ensure layouts work with longer Russian and Greek strings, on mobile and desktop.
- Every new frontend feature must ship with all three translations. Write natural, concise, human-sounding copy in each language; do not use mechanical word-for-word translations or AI-style filler.
- Keep statistical values and API identifiers language-independent. Preserve original labels from imported government datasets. Translate dataset labels and descriptions only where needed, preferably in frontend locale files for small static sets; use PostgreSQL `JSONB` for modest multilingual content stored in the database when justified. Do not introduce translation tables or complex backend i18n infrastructure prematurely.
- For government-provided text without a reliable translation, show the original text clearly rather than inventing a translation; use English fallback for our own UI strings. Never fabricate official multilingual data.
- Cover the essential language-switching and persistence behavior in a small number of meaningful integration or end-to-end tests, using the project's existing no-mocks testing policy.


## Official data portal and mandatory first charts

- **Primary data source:** Cyprus Government Open Data Portal: https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset (dataset catalog). Use https://www.data.gov.cy/ as the starting point for discovering official datasets. Prefer original official downloads/APIs linked from dataset pages; do not treat the catalog HTML itself as a data API. Other official Cyprus statistical sources may be used when needed, but document the original publisher and exact dataset URL.
- Before writing an importer, verify the exact dataset, downloadable resource URL, schema, historical coverage, update cadence, license and access restrictions. Check whether the source distinguishes Republic of Cyprus government-controlled areas from the entire island, and display the scope accurately. If a dataset is missing, unclear or inaccessible, record the limitation and ask rather than guessing or scraping aggressively.
- **The Statistics landing page must initially feature these two real-data charts, in this order:**

### 1. Where do people moving to Cyprus come from?
- Topic: **immigration to Cyprus by country of origin and year** (Russian: «Откуда приезжают люди на Кипр?»).
- Use a country ranking for a selected year, with a year selector; if data supports it, provide an easy comparison across years. Show counts and units, not invented percentages.
- Distinguish immigration flows from resident foreign population, citizenship, country of birth and tourist arrivals. These are different measures; **do not silently substitute one for another**. Clearly label the precise definition the official source actually provides.

### 2. Who visits Cyprus most often?
- Topic: **tourist arrivals by country of origin and season** (Russian: «Кто чаще всего отдыхает на Кипре?»).
- Provide a country ranking and seasonal breakdown, with sensible year and season filters. Prefer official monthly arrivals and derive seasons only when month-level data is available. Document the season definition (e.g. winter Dec–Feb, spring Mar–May, summer Jun–Aug, autumn Sep–Nov) and handle cross-year winter carefully.
- Distinguish tourist arrivals from all passenger arrivals, overnight stays and unique visitors. Label the actual metric, units, geography and period; never present one as another.

### Shared chart requirements
- Both charts must be visible and useful on the first Statistics page, not hidden behind generic placeholders or a dataset explorer. Use Apache ECharts with readable labels, responsive layout, clear tooltips, filters and accessible supporting text.
- All titles, country display names where reliable, axes, filter labels, tooltips, empty/error states and source attribution must support **English, Russian and Greek**. The default language is **English** on a first visit, regardless of browser locale; restore an explicitly saved choice. Use i18next JSON resources as specified in the Internationalization section above.
- Persist verified official data in PostgreSQL and serve chart data from our own API. Synchronize infrequently according to official publication cadence; avoid per-user upstream requests. Never fake historical data to fill gaps.
- Update `docs/data-sources.md` with exact URLs, dataset definitions, refresh schedules and limitations. Update `docs/database.dbml` whenever tables or relationships change. Keep both concise.

## Mobile-first responsive experience (mandatory)
- Treat mobile support as a core product requirement, not a desktop layout squeezed onto a phone. Design and implement mobile-first, then adapt cleanly to tablets and desktops.
- All pages, navigation, filters, forms, tables, maps, and charts must work comfortably on small touchscreens. Avoid horizontal page scrolling, clipped content, overlapping controls, tiny text, and hover-only interactions.
- Use fluid layouts and sensible breakpoints rather than fixed desktop widths. Account for long Russian and Greek translations, browser text scaling, and portrait/landscape orientations.
- Provide accessible touch targets (aim for at least 44 × 44 CSS pixels), readable typography, visible focus states, keyboard support, and sufficient contrast. Never rely on color alone to communicate a result.
- On mobile, keep chart legends, axes, labels, and tooltips legible. Adapt chart presentation (including horizontal bars, simplified tick density, and stacked filters) rather than shrinking a desktop chart indiscriminately. Preserve the underlying data and allow users to inspect exact values.
- Make parking maps genuinely usable on phones: clear markers, easy-to-open details, practical map/list switching, and no gestures that trap page scrolling. Offer a useful list fallback when a map is inconvenient.
- Keep mobile performance strong: minimize JavaScript and layout shifts, lazy-load heavy map/chart code when appropriate, and avoid unnecessary animations or large assets.
- Validate at representative widths such as 320, 375, 390, 768, and 1280 CSS pixels, including mobile browser behavior. Test essential user journeys at mobile and desktop sizes with a small number of meaningful end-to-end/integration tests; follow the no-unit-tests/no-mocks policy.
- Consider responsive behavior and all three locales part of the acceptance criteria for every frontend feature. Do not mark a feature complete if it only looks or works well on desktop.

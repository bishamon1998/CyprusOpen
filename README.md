# Cyprus Open

Public statistics and civic tools for people living in Cyprus.

Local PostgreSQL runs with isolated Statistics and Parking databases. An implementation plan, architecture and interactive interface sketch are ready; application services and imports are next.

Planned stack: Spring Boot, PostgreSQL, Liquibase, React and TypeScript. Production hosting: Railway.

After installing Docker Desktop:

```powershell
Copy-Item .env.example .env
# Fill in three distinct passwords in .env.
docker compose up -d --wait postgres
```

Start with the [implementation plan](docs/plan.md), [architecture](docs/architecture.md) and [interface sketch](docs/prototype/index.html). Technical notes: [local setup](docs/local-development.md), [database](docs/database.md), [API draft](docs/api-design.md), [sources](docs/data-sources.md), [Railway](docs/railway.md) and [GitHub](docs/github.md). Project requirements: [CyprusOpen_Codex_Instructions.md](CyprusOpen_Codex_Instructions.md).

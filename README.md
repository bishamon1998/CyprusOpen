# Cyprus Open

Public statistics and civic tools for people living in Cyprus.

Initial setup: local PostgreSQL configuration with separate Statistics and Parking databases. Application services and dataset imports are not implemented yet.

Planned stack: Spring Boot, PostgreSQL, Liquibase, React and TypeScript. Production hosting: Railway.

After installing Docker Desktop:

```powershell
Copy-Item .env.example .env
# Fill in three distinct passwords in .env.
docker compose up -d --wait postgres
```

See [local setup](docs/local-development.md), [architecture](docs/architecture.md) and [database](docs/database.md). See [GitHub setup](docs/github.md) before the first push. Project requirements are in [CyprusOpen_Codex_Instructions.md](CyprusOpen_Codex_Instructions.md).

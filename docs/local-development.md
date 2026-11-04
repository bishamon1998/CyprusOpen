# Local development

Install Git for Windows and Docker Desktop with Compose v2. Git, Docker and Temurin JDK 25 are available on the development machine. Maven is not yet installed; add a Maven wrapper with the backend skeleton.

Copy `.env.example` to `.env` and supply three distinct, nonempty passwords. Never commit `.env`. Run `docker compose up -d --wait postgres` from the repository root. The database port is bound to localhost only; change `POSTGRES_PORT` if 5432 is occupied.

| Service | Database | Schema | User | JDBC URL from host |
| --- | --- | --- | --- | --- |
| Statistics | statistics | statistics | statistics_app | jdbc:postgresql://localhost:5432/statistics |
| Parking | parking | parking | parking_app | jdbc:postgresql://localhost:5432/parking |

Inside Compose, use `postgres:5432` instead of `localhost:5432`. Applications must use their own account, never the administrator account.

Check startup with `docker compose ps` and `docker compose logs postgres`. Run `./scripts/check-database.ps1` in PowerShell to verify writes in each service schema (rolled back) and denied cross-database connections. All four checks passed on the initial local instance. Connect interactively with `docker compose exec postgres psql -h 127.0.0.1 -U statistics_app -d statistics -W`; it prompts for the password.

Initialization runs only on a new empty volume. Changing `.env` does not change existing database passwords. If initialization fails, inspect the logs and repair the existing database explicitly; do not delete the volume to retry without checking for data. `docker compose down` preserves the volume. Do not add `-v` unless deliberately discarding all local data.

# Cyprus Open

Read `CyprusOpen_Codex_Instructions.md` for the project requirements. Keep the current user request separate from the full product specification.

- Independent Statistics and Parking applications, React frontend, small common JAR. Pharmacies later.
- Local PostgreSQL databases and roles already exist. Never remove volumes or overwrite `.env` without a deliberate reason and authorization for data loss.
- Work order and design: `docs/plan.md`, `docs/architecture.md`. Draft API and prototype are proposals, not implemented APIs or verified data.
- Verify official datasets and terms before integrations. External fetching is disabled by default; never guess source fields or live readings.
- Integration tests only; no object mocks or requests to government services from CI.
- Keep Liquibase and `docs/database.dbml` aligned. No speculative tables in the current-schema diagram.
- Review, validate, commit coherent milestones, push to the configured remote, verify the remote commit. Never commit secrets or force-push.
- Railway is the deployment target. Do not create paid resources or deploy without explicit authorization.

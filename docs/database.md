# Database

One local PostgreSQL 17 instance hosts two isolated databases. Each service owns a schema and a non-superuser login. PUBLIC has no database access to either service database, and neither service has privileges on the other database.

`infra/postgres/001-databases.sql` provisions databases and roles only. Domain tables and Liquibase histories will be added with the independently runnable backend services, after dataset verification. Parking occupancy history will not be stored.

`database.dbml` is the portable diagram source. Paste it into dbdiagram.io when tables exist; it is intentionally empty today. Update it together with every Liquibase schema change.

-- Adds the pharmacies database to a PostgreSQL volume initialised before it existed.
-- New volumes get it from 001-databases.sql. Run once as the admin user, for example:
--   docker compose exec -T -e PHARMACIES_DB_PASSWORD postgres psql -U postgres -f - < infra/postgres/002-pharmacies.sql
\set ON_ERROR_STOP on
\getenv pharmacies_password PHARMACIES_DB_PASSWORD

CREATE ROLE pharmacies_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'pharmacies_password';
CREATE DATABASE pharmacies OWNER pharmacies_app;
REVOKE ALL ON DATABASE pharmacies FROM PUBLIC;

\connect pharmacies
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA pharmacies AUTHORIZATION pharmacies_app;
ALTER ROLE pharmacies_app IN DATABASE pharmacies SET search_path TO pharmacies;

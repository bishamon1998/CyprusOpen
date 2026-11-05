\set ON_ERROR_STOP on
\getenv statistics_password STATISTICS_DB_PASSWORD
\getenv parking_password PARKING_DB_PASSWORD
\getenv pharmacies_password PHARMACIES_DB_PASSWORD

CREATE ROLE statistics_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'statistics_password';
CREATE ROLE parking_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'parking_password';
CREATE ROLE pharmacies_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'pharmacies_password';

CREATE DATABASE statistics OWNER statistics_app;
CREATE DATABASE parking OWNER parking_app;
CREATE DATABASE pharmacies OWNER pharmacies_app;
REVOKE ALL ON DATABASE statistics FROM PUBLIC;
REVOKE ALL ON DATABASE parking FROM PUBLIC;
REVOKE ALL ON DATABASE pharmacies FROM PUBLIC;
REVOKE CONNECT ON DATABASE postgres FROM PUBLIC;

\connect statistics
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA statistics AUTHORIZATION statistics_app;
ALTER ROLE statistics_app IN DATABASE statistics SET search_path TO statistics;

\connect parking
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA parking AUTHORIZATION parking_app;
ALTER ROLE parking_app IN DATABASE parking SET search_path TO parking;

\connect pharmacies
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA pharmacies AUTHORIZATION pharmacies_app;
ALTER ROLE pharmacies_app IN DATABASE pharmacies SET search_path TO pharmacies;

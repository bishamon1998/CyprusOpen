#!/bin/sh
# Loads or replaces pharmacy map points from a UTF-8 CSV.
#
# Usage: scripts/import-pharmacy-locations.sh locations.csv [psql command ...]
#   Local (default): docker compose exec -T postgres psql -U pharmacies_app -d pharmacies
#   Railway:         scripts/import-pharmacy-locations.sh locations.csv railway ssh --service Postgres -- psql -U postgres -d pharmacies
#
# CSV header: registration_number,latitude,longitude,precision,source
#   registration_number  digits, as "Reg. No." in the official roster
#   latitude, longitude  decimal degrees (WGS 84) within Cyprus
#   precision            exact | street | locality
#   source               origin of the point (URL or short description); required
#
# Run after the pharmacies service has created the table. Rows missing from the file are kept;
# the whole file is rejected if any row is invalid.
set -eu

csv=$1
shift
[ "$#" -gt 0 ] || set -- docker compose exec -T postgres psql -U pharmacies_app -d pharmacies

{
  cat <<'SQL'
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE location_import (
  registration_number TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  precision TEXT,
  source TEXT
) ON COMMIT DROP;
COPY location_import FROM STDIN WITH (FORMAT csv, HEADER true);
SQL
  sed -e '1s/^\xEF\xBB\xBF//' -e 's/\r$//' "$csv"
  printf '\\.\n'
  cat <<'SQL'
INSERT INTO pharmacies.pharmacy_location (registration_number, latitude, longitude, precision, source, updated_at)
SELECT trim(registration_number), latitude, longitude, lower(trim(precision)), trim(source), current_timestamp
FROM location_import
ON CONFLICT (registration_number) DO UPDATE
SET latitude = excluded.latitude, longitude = excluded.longitude, precision = excluded.precision,
    source = excluded.source, updated_at = excluded.updated_at;
SELECT count(*) AS imported FROM location_import;
COMMIT;
SQL
} | "$@"

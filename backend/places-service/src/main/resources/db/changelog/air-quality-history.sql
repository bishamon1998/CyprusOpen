-- Hourly readings per station, kept for a week so the site can show how the air changed; the source only
-- publishes the current hour.
CREATE TABLE air_quality_history (
  station_id TEXT NOT NULL,
  measured_at TIMESTAMPTZ NOT NULL,
  readings JSONB NOT NULL,
  PRIMARY KEY (station_id, measured_at)
);
CREATE INDEX air_quality_history_measured_at ON air_quality_history (measured_at);

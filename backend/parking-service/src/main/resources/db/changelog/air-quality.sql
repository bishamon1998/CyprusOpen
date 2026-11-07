-- Latest hourly air quality readings of all stations, as one document; empty until the first import.
CREATE TABLE air_quality (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  document JSONB NOT NULL,
  retrieved_at TIMESTAMPTZ NOT NULL
);

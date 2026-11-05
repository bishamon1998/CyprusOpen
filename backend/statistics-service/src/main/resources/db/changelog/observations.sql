CREATE TABLE dataset (
  code TEXT PRIMARY KEY CHECK (code ~ '^[a-z][a-z0-9-]*$'),
  source_updated_at TIMESTAMPTZ NOT NULL,
  checked_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE observation (
  dataset_code TEXT NOT NULL REFERENCES dataset (code) ON DELETE CASCADE,
  series TEXT NOT NULL CHECK (length(series) > 0),
  period TEXT NOT NULL CHECK (period ~ '^[0-9]{4}(-(0[1-9]|1[0-2]))?$'),
  value DOUBLE PRECISION NOT NULL,
  low_reliability BOOLEAN NOT NULL DEFAULT false,
  PRIMARY KEY (dataset_code, series, period)
);

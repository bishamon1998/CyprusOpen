CREATE TABLE pharmacy_location (
  registration_number TEXT PRIMARY KEY CHECK (registration_number ~ '^[0-9]+$'),
  latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN 34 AND 36),
  longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN 32 AND 35),
  precision TEXT NOT NULL DEFAULT 'exact' CHECK (precision IN ('exact', 'street', 'locality')),
  source TEXT NOT NULL CHECK (length(trim(source)) > 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);

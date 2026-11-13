-- Green Dot glass banks and recycling islands, as one document; empty until the first import.
CREATE TABLE recycling_points (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  document JSONB NOT NULL,
  retrieved_at TIMESTAMPTZ NOT NULL
);

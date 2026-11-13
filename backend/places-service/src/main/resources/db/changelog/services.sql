-- State hospitals and Citizen Service Centres, as one document; empty until the first import.
CREATE TABLE service_places (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  document JSONB NOT NULL,
  retrieved_at TIMESTAMPTZ NOT NULL
);

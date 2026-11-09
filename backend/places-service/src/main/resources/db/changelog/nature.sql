-- Department of Forests picnic sites, camping sites and nature trails, as one document; empty until the first import.
CREATE TABLE nature_sites (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  document JSONB NOT NULL,
  retrieved_at TIMESTAMPTZ NOT NULL
);

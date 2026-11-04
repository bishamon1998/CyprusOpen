CREATE TABLE parking_directory (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  document JSONB NOT NULL,
  last_checked_at TIMESTAMPTZ NOT NULL,
  last_attempt_at TIMESTAMPTZ,
  etag TEXT,
  last_modified TEXT,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT '-infinity',
  consecutive_failures INTEGER NOT NULL DEFAULT 0,
  sync_status TEXT NOT NULL DEFAULT 'seeded'
);

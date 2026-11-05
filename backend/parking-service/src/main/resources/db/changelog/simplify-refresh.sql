ALTER TABLE parking_directory
  DROP COLUMN last_attempt_at,
  DROP COLUMN next_attempt_at,
  DROP COLUMN consecutive_failures,
  DROP COLUMN sync_status;

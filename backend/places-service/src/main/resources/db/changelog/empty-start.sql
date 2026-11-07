ALTER TABLE parking_directory ALTER COLUMN document DROP NOT NULL;
ALTER TABLE parking_directory ALTER COLUMN last_checked_at DROP NOT NULL;
ALTER TABLE parking_directory ALTER COLUMN sync_status SET DEFAULT 'not_imported';
INSERT INTO parking_directory(id) VALUES (1) ON CONFLICT DO NOTHING;

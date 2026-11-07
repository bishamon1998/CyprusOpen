-- Quarterly tables (house price index) store periods as 2026-Q2.
ALTER TABLE observation DROP CONSTRAINT observation_period_check;
ALTER TABLE observation ADD CONSTRAINT observation_period_check
  CHECK (period ~ '^[0-9]{4}(-(0[1-9]|1[0-2]|Q[1-4]))?$');

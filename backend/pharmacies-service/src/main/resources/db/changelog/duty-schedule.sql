CREATE TABLE duty_entry (
  duty_date DATE NOT NULL,
  district TEXT NOT NULL,
  registration_number TEXT NOT NULL,
  name TEXT NOT NULL,
  address TEXT NOT NULL,
  municipality TEXT NOT NULL,
  phone TEXT NOT NULL,
  PRIMARY KEY (duty_date, district, registration_number)
);

CREATE INDEX duty_entry_district_date ON duty_entry (district, duty_date);

CREATE TABLE schedule_refresh (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  last_checked_at TIMESTAMPTZ
);

INSERT INTO schedule_refresh (id) VALUES (1);

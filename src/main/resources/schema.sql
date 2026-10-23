-- Dates use ISO-8601 text (YYYY-MM-DD)
-- Money uses integer minor units (cents)

-- lookup tables
CREATE TABLE IF NOT EXISTS event_types (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS skills (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS need_types (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

-- Donation module
CREATE TABLE IF NOT EXISTS donors (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  email TEXT,
  phone TEXT,
  active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE IF NOT EXISTS donations (
  id INTEGER PRIMARY KEY,
  donor_id INTEGER NOT NULL REFERENCES donors(id),
  donation_type TEXT NOT NULL,
  amount_minor INTEGER,
  item_description TEXT,
  quantity INTEGER,
  estimated_value_minor INTEGER,
  reference_number TEXT,
  donation_date TEXT NOT NULL,
  notes TEXT
);

-- Event module
CREATE TABLE IF NOT EXISTS events (
  id            INTEGER PRIMARY KEY,
  title         TEXT    NOT NULL,
  event_type_id INTEGER NOT NULL REFERENCES event_types(id),
  date          TEXT    NOT NULL,
  capacity      INTEGER NOT NULL CHECK (capacity > 0),
  status        TEXT    NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','OPEN','CLOSED')),
  description   TEXT
);

CREATE TABLE IF NOT EXISTS event_registrations (
  id             INTEGER PRIMARY KEY,
  event_id       INTEGER NOT NULL REFERENCES events(id),
  attendee_name  TEXT    NOT NULL,
  attendee_email TEXT,
  ticket_ref     TEXT    NOT NULL UNIQUE,
  checked_in     INTEGER NOT NULL DEFAULT 0 CHECK (checked_in IN (0,1))
);

-- admin deletions/edits of lookup rows survive restarts.
CREATE TABLE IF NOT EXISTS seed_history (
  name TEXT PRIMARY KEY,
  ran_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- initial lookup values, run once per fresh database.
-- NOTE: the first ADMIN account is NOT seeded here. It is created by the
-- app on first run because the password must be hashed in Java.
BEGIN TRANSACTION;

INSERT INTO event_types (name)
SELECT name FROM (
  SELECT 'Fundraiser' AS name
  UNION ALL SELECT 'Workshop'
  UNION ALL SELECT 'Volunteer Drive'
)
WHERE NOT EXISTS (SELECT 1 FROM seed_history WHERE name = 'lookup_init');

INSERT INTO skills (name)
SELECT name FROM (
  SELECT 'Packing' AS name
  UNION ALL SELECT 'Teaching'
  UNION ALL SELECT 'Medical Support'
)
WHERE NOT EXISTS (SELECT 1 FROM seed_history WHERE name = 'lookup_init');

INSERT INTO need_types (name)
SELECT name FROM (
  SELECT 'Food' AS name
  UNION ALL SELECT 'Clothing'
  UNION ALL SELECT 'Medical'
  UNION ALL SELECT 'Education'
)
WHERE NOT EXISTS (SELECT 1 FROM seed_history WHERE name = 'lookup_init');

-- mark the block as done
INSERT OR IGNORE INTO seed_history (name) VALUES ('lookup_init');

COMMIT;

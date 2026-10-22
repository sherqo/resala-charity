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

-- ============================================================
-- Domain tables
-- ============================================================

-- Beneficiary: a person or family receiving support.
-- need_type_id points at the need_types lookup seeded above.
CREATE TABLE IF NOT EXISTS beneficiaries (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  name           TEXT    NOT NULL,
  phone          TEXT,
  household_size INTEGER NOT NULL DEFAULT 1 CHECK (household_size >= 0),
  need_type_id   INTEGER NOT NULL REFERENCES need_types(id),
  status         TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CLOSED')),
  notes          TEXT
);

CREATE INDEX IF NOT EXISTS idx_beneficiaries_need_type ON beneficiaries(need_type_id);
CREATE INDEX IF NOT EXISTS idx_beneficiaries_status    ON beneficiaries(status);

-- Volunteer and Event tables live together: the foreign keys between them are
-- circular (event_registrations -> volunteers, volunteer_hours -> events), and
-- SQLite resolves a foreign key at insert time, so a child table cannot be
-- written to until every table it points at exists.

-- Volunteer: a person offering help. Availability is a flag (0/1).
CREATE TABLE IF NOT EXISTS volunteers (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  name      TEXT    NOT NULL,
  email     TEXT UNIQUE,
  phone     TEXT,
  available INTEGER NOT NULL DEFAULT 1 CHECK (available IN (0,1))
);

-- Which skills a volunteer has (V2). skills is the lookup table seeded above.
CREATE TABLE IF NOT EXISTS volunteer_skills (
  volunteer_id INTEGER NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
  skill_id     INTEGER NOT NULL REFERENCES skills(id),
  PRIMARY KEY (volunteer_id, skill_id)
);

-- Hours log: one row per logged assignment (V4). The volunteer's total is a
-- SUM over these rows, not a stored column.
CREATE TABLE IF NOT EXISTS volunteer_hours (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  volunteer_id INTEGER NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
  event_id     INTEGER REFERENCES events(id),
  hours        REAL NOT NULL CHECK (hours > 0),
  logged_on    TEXT NOT NULL DEFAULT (date('now'))
);

-- Event: an organized activity. date is an ISO-8601 date (YYYY-MM-DD).
CREATE TABLE IF NOT EXISTS events (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  title         TEXT    NOT NULL,
  event_type_id INTEGER NOT NULL REFERENCES event_types(id),
  date          TEXT    NOT NULL,
  capacity      INTEGER NOT NULL DEFAULT 0 CHECK (capacity >= 0),
  status        TEXT    NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT','OPEN','CLOSED')),
  description   TEXT
);

-- Registration of a volunteer for an event (E3-E5). attended_at stays NULL
-- until the volunteer is checked in.
CREATE TABLE IF NOT EXISTS event_registrations (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  event_id      INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  volunteer_id  INTEGER NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
  ticket_code   TEXT    NOT NULL UNIQUE,
  registered_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now')),
  attended_at   TEXT,
  UNIQUE (event_id, volunteer_id)
);

CREATE INDEX IF NOT EXISTS idx_volunteer_skills_skill     ON volunteer_skills(skill_id);
CREATE INDEX IF NOT EXISTS idx_volunteer_hours_volunteer ON volunteer_hours(volunteer_id);
CREATE INDEX IF NOT EXISTS idx_volunteer_hours_event     ON volunteer_hours(event_id);
CREATE INDEX IF NOT EXISTS idx_events_type                ON events(event_type_id);
CREATE INDEX IF NOT EXISTS idx_events_date                ON events(date);
CREATE INDEX IF NOT EXISTS idx_event_registrations_event ON event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_vol   ON event_registrations(volunteer_id);

-- ============================================================
-- Accounts (login + RBAC)
-- ============================================================

-- A login for ADMIN/STAFF (volunteer_id NULL) or a VOLUNTEER linked to their
-- volunteer profile. The CHECK enforces the link exactly for VOLUNTEER.
-- Usernames are case-insensitive; deleting a volunteer removes their login.
CREATE TABLE IF NOT EXISTS accounts (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT    NOT NULL COLLATE NOCASE UNIQUE,
  password_hash TEXT    NOT NULL,
  full_name     TEXT    NOT NULL,
  email         TEXT,
  role          TEXT    NOT NULL CHECK (role IN ('ADMIN','STAFF','VOLUNTEER')),
  volunteer_id  INTEGER UNIQUE REFERENCES volunteers(id) ON DELETE CASCADE,
  CHECK ((role = 'VOLUNTEER') = (volunteer_id IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_accounts_role ON accounts(role);


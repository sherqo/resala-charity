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

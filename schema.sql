-- Schema derived from class-diagram.mermaid (Resala non-profit system)
-- Target engine: SQLite

-- users (User + Role enum)
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('ADMIN','STAFF','VOLUNTEER'))
);

-- Donor
CREATE TABLE donors (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL,
    email TEXT UNIQUE,
    phone TEXT
);

-- Volunteer (Observer + hours tracking)
CREATE TABLE volunteers (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL,
    email TEXT UNIQUE,
    hours REAL NOT NULL DEFAULT 0
);

-- Beneficiary
CREATE TABLE beneficiaries (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT NOT NULL,
    address   TEXT,
    need_type TEXT NOT NULL
);

-- Event (Subject + EventState)
CREATE TABLE events (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    date  TEXT NOT NULL,                    -- ISO-8601 date/time
    state TEXT NOT NULL DEFAULT 'DRAFT'
          CHECK (state IN ('DRAFT','OPEN','CLOSED'))
);

-- Donation (Factory products; type discriminator for Online/Check/InKind)
CREATE TABLE donations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    donor_id        INTEGER NOT NULL REFERENCES donors(id),
    type            TEXT    NOT NULL CHECK (type IN ('ONLINE','CHECK','INKIND')),
    amount          REAL    NOT NULL,
    description     TEXT,
    has_tax_receipt INTEGER NOT NULL DEFAULT 0,   -- TaxReceiptDecorator flag
    thanked         INTEGER NOT NULL DEFAULT 0,   -- ThankYouDecorator flag
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- Volunteer *--* Event ("registers for")
CREATE TABLE event_volunteers (
    event_id     INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    volunteer_id INTEGER NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, volunteer_id)
);

-- DistributionService.distributeAid(Beneficiary, item)
CREATE TABLE distributions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    beneficiary_id INTEGER NOT NULL REFERENCES beneficiaries(id),
    item           TEXT    NOT NULL,
    event_id       INTEGER REFERENCES events(id),
    distributed_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_donations_donor  ON donations(donor_id);
CREATE INDEX idx_dist_beneficiary ON distributions(beneficiary_id);

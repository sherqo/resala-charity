-- Lookup data
-- for the first ADMIN account, it should be created from the app itself, not from here
-- since the password needs to be hashed before being added to the DB
INSERT OR IGNORE INTO event_types (name) VALUES ('Fundraiser');
INSERT OR IGNORE INTO event_types (name) VALUES ('Workshop');
INSERT OR IGNORE INTO event_types (name) VALUES ('Volunteer Drive');

INSERT OR IGNORE INTO skills (name) VALUES ('Packing');
INSERT OR IGNORE INTO skills (name) VALUES ('Teaching');
INSERT OR IGNORE INTO skills (name) VALUES ('Medical Support');

INSERT OR IGNORE INTO need_types (name) VALUES ('Food');
INSERT OR IGNORE INTO need_types (name) VALUES ('Clothing');
INSERT OR IGNORE INTO need_types (name) VALUES ('Medical');
INSERT OR IGNORE INTO need_types (name) VALUES ('Education');

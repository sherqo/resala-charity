-- Seed data for schema.sql (Resala non-profit system, SQLite)
-- Run after schema.sql. IDs are explicit so FK references are stable.

-- Users (password hashes are placeholders, not real bcrypt hashes)
INSERT INTO users (id, username, password_hash, role) VALUES
    (1, 'admin',    'HASH_placeholder_admin',    'ADMIN'),
    (2, 'staff1',   'HASH_placeholder_staff1',   'STAFF'),
    (3, 'vol1acc',  'HASH_placeholder_vol1acc',  'VOLUNTEER');

-- Donors
INSERT INTO donors (id, name, email, phone) VALUES
    (1, 'Ahmed Hassan',  'ahmed.hassan@example.com',  '+201001112233'),
    (2, 'Sara Ali',      'sara.ali@example.com',      '+201004445566'),
    (3, 'Fatma Khalil',  NULL,                        '+201007778899');

-- Volunteers
INSERT INTO volunteers (id, name, email, hours) VALUES
    (1, 'Mohamed Said',   'm.said@example.com',    24.5),
    (2, 'Nour Ibrahim',   'nour.i@example.com',    12.0),
    (3, 'Youssef Adel',   NULL,                     0.0);

-- Beneficiaries
INSERT INTO beneficiaries (id, name, address, need_type) VALUES
    (1, 'Zainab Omar',    '12 El-Malek St, Cairo',   'FOOD'),
    (2, 'Ibrahim Mostafa','45 Nasr St, Giza',        'MEDICAL'),
    (3, 'Amina Farouk',   NULL,                      'CLOTHING');

-- Events (one per state to cover DRAFT/OPEN/CLOSED)
INSERT INTO events (id, title, date, state) VALUES
    (1, 'Food Bank Drive',      '2026-11-01 09:00:00', 'OPEN'),
    (2, 'Winter Clothing Day',  '2026-12-15 10:00:00', 'DRAFT'),
    (3, 'Medical Checkup Day',  '2026-09-10 08:00:00', 'CLOSED');

-- Donations (one per type; donor 3 has an unthanked in-kind donation)
INSERT INTO donations (id, donor_id, type, amount, description, has_tax_receipt, thanked, created_at) VALUES
    (1, 1, 'ONLINE', 500.00, 'Online transfer',        1, 1, '2026-09-15 11:20:00'),
    (2, 2, 'CHECK',  1200.00,'Check #12345',           1, 1, '2026-09-20 14:00:00'),
    (3, 3, 'INKIND', 300.00, '10 boxes of food',       0, 0, '2026-10-01 09:30:00');

-- Volunteer *--* Event registrations
INSERT INTO event_volunteers (event_id, volunteer_id) VALUES
    (1, 1),
    (1, 2),
    (3, 1),
    (3, 3);

-- Aid distributions
INSERT INTO distributions (id, beneficiary_id, item, event_id, distributed_at) VALUES
    (1, 1, 'Food box',   1, '2026-09-12 12:00:00'),
    (2, 2, 'Medicine',   3, '2026-09-10 15:30:00'),
    (3, 1, 'Blanket',    NULL, '2026-10-05 17:45:00');

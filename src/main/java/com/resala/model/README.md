# Model

Shared nouns. Plain data + validation, no SQL, no printing.

## Done

- `Role` — enum ADMIN / STAFF / VOLUNTEER

## To create

- `User` — login account: id, username, password hash, full name, email, role
- `Donor` — individual/organization, contact, tax id
- `Volunteer` — contact, availability
- `Event` — title, type, date, capacity, status
- `Beneficiary` — contact, household size, needs, status
- `Permission` — enum used by `Role` and `Session`

## Rules

- No logic beyond validation (e.g. non-empty name, positive amount).
- Keep `User`, `Donor` and `Volunteer` separate; link a volunteer with a nullable id
  on `users` instead of forcing one class to be all three.
- Money is stored as integer minor units.

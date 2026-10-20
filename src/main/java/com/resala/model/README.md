# Model

Shared nouns. Plain data + validation, no SQL, no printing.

## Done

- `Role` — enum ADMIN / STAFF / VOLUNTEER
- `Account` — login: username, password hash, full name, email, role, optional `volunteerId`
- `Donor` — individual/organization, contact, tax id
- `Volunteer` — contact, availability
- `Event` — title, type, date, capacity, status
- `Beneficiary` — contact, household size, need type, status

## To create

- `Permission` — enum used by `Role` and `Session`

## Rules

- No logic beyond validation (e.g. non-empty name, positive amount).
- Keep `Account`, `Donor` and `Volunteer` separate; link a volunteer account with a
  nullable `volunteerId` instead of forcing one class to be all three.
- Money is stored as integer minor units.

## Notes for login

- The Donor and Beneficiary shouldn't have an account
- Volunteer can optionally have a read-only account
- Staff and Admins must have accounts

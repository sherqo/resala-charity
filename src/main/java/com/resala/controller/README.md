# Controller

Input handling only: turns view input into service calls.

## Rules

- Never print and never read `System.in` — only views do I/O.
- Never write SQL — repositories do.
- Never check roles with `if (role == ...)` — ask `Session.can(Permission.X)`.
- Catch `AccessDeniedException` / validation errors and hand a message back to the view.

## Files to create

One per module: `DonationController`, `VolunteerController`, `EventController`,
`BeneficiaryController`, `AuthController`, `AdminController`.

Each takes the owning service + a view interface in its constructor.

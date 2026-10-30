# Volunteer

Volunteers, skills, assignments, hours and certificates.

## Features (see `docs/design.md`)

- [x] V1 volunteer CRUD + availability
- [x] V2 skills per volunteer (skills lookup table)
- [x] V3 assign volunteer to an event (skill + date check)
- [x] V4 log hours per assignment
- [x] V5 summaries: total, per event, per month
- [x] V6 certificate at >= 20 hours
- [x] V7 search by skill / availability

## Patterns

- **MVC** — `VolunteerController` translates view input into service calls;
  `VolunteerService` holds the business rules; `VolunteerView` /
  `ConsoleVolunteerView` handle all input and output.
- **Concrete persistence class** — `SqliteVolunteerRepository` contains all SQL for
  volunteers, skills, and assignments and is used directly by `VolunteerService`.
  There is no repository interface.
- **Abstract Factory** — `UiFactory` creates the volunteer view, like the other modules.
- **Singleton** — the module reuses the existing `DatabaseManager` connection;
  no new singleton was added.

Assignment checks are kept directly in `VolunteerService.validateAssignment(...)`.
No Strategy is used. No Iterator is used.

## Tables

`volunteers`, `skills` (existing lookup), `volunteer_skills`, `volunteer_hours`.

`volunteer_hours` stores one row per assignment. `hours` starts at 0 when the
volunteer is assigned and is updated when hours are logged.

## Implemented Files

- `model/Skill` — skill lookup entity.
- `volunteer/VolunteerRequest` — input DTO (a normal class, not a record).
- `volunteer/assign/VolunteerAssignment` — one assignment and its logged hours.
- `volunteer/VolunteerSummary` — total, per-event, and per-month totals.
- `volunteer/VolunteerService` — volunteer, skill, assignment, summary, and certificate logic.
- `volunteer/CertificateGenerator` — certificate at 20 hours.
- `persistence/sqlite/SqliteVolunteerRepository` — concrete SQLite persistence class.
- `controller/VolunteerController` — MVC controller.
- `view/VolunteerView` + `view/console/ConsoleVolunteerView` — UI boundary and console menu.

# Event

Events, registration, attendance and reminders.

## Features (see `docs/design.md`)

- E1 event CRUD (title, type, date, capacity)
- E2 lifecycle DRAFT -> OPEN -> CLOSED
- E3 register attendee with capacity check
- E4 ticket reference code
- E5 attendance check-in (required)
- E6 reminder to attendees (delegated to Communication)
- E7 attendance report: registered / attended / no-show

## Patterns

- **State** (Phase 2) — `state/`: DraftState, OpenState, ClosedState own the rules per status
- **Observer** (reuse) — event updates notify registered attendees

## Tables

`events`, `event_types`, `event_registrations`

## Files to create

`EventRepository`, `EventService`, then `state/*` in Phase 2.

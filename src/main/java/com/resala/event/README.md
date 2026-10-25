# Event

Events, registration, attendance and reminders.

## Features (see `docs/design.md`)

- [x] E1 event CRUD (title, type, date, capacity, description)
- [x] E2 lifecycle DRAFT -> OPEN -> CLOSED
- [x] E3 register attendee with capacity check
- [x] E4 ticket reference code (unique 8-character code)
- [x] E5 attendance check-in (required)
- [x] E6 attendee notification on updates & closure (Observer pattern via `notification`)
- [x] E7 attendance report: registered / attended / no-show

## Patterns

- **MVC** — `EventController` handles requests without I/O; `EventView` / `ConsoleEventView` manage UI; `EventService` handles business logic.
- **Abstract Factory** — `UiFactory` creates `EventView`.
- **Repository** — `EventRepository` and `EventRegistrationRepository` abstract database operations with SQLite implementations.
- **Observer** — `EventService` acts as `EventSubject`. Registered attendees are automatically notified via `EventNotificationObserver` whenever an event is updated or closed.
- **State** (Phase 2) — `state/`: DraftState, OpenState, ClosedState own the rules per status.

## Tables

`events`, `event_types`, `event_registrations`

## Implemented Files

- `EventRequest` — DTO for creating and updating events.
- `EventRegistration` — Entity representing attendee registration and ticket details.
- `EventRepository` — Contract for event persistence.
- `EventRegistrationRepository` — Contract for registration and check-in persistence.
- `EventService` — Core business logic, implements `EventSubject`, handles lifecycle transitions, capacity verification, and attendance reports.
- `SqliteEventRepository` & `SqliteEventRegistrationRepository` — SQLite implementations.
- `EventController` — MVC controller.
- `EventView` & `ConsoleEventView` — UI boundary and console implementation (inherits `AbstractConsoleView`).

## To Create (Phase 2)

- `state/*` for State pattern event lifecycle.

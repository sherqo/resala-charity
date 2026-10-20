# View

The only layer allowed to print or read input.

## Shape

- One interface per screen: `DonationView`, `VolunteerView`, `EventView`,
  `BeneficiaryView`, `CommunicationView`, `ControlPanelView`.
- One console implementation per screen (`ConsoleDonationView`, ...).
- `ConsoleMenu` is a dispatcher: login, filter menu items by `Session.can(...)`,
  hand control to a view. Keep it under ~150 lines.
- `UiFactory` / `ConsoleUiFactory` pick the implementation family. A future
  JavaFX family (`FxUiFactory`) must not require changes in controllers.

## Rules

- No business rules, no SQL, no direct service-to-service calls.
- Views return plain data (forms/DTOs) to controllers, never entities half-filled.

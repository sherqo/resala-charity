# Notification

Message channels, recipients and the notification log.

## Features (see `docs/design.md`)

- [x] C1 send to one person
- [x] C2 channel chosen at runtime: email / SMS / social (all mocked)
- [ ] C3 templates: receipt, thank-you, reminder, certificate (Phase 2)
- [x] C4 send to a group: event attendees, etc.
- [ ] C5 notification log: who, what, when, channel (Phase 2)
- [x] C6 automatic triggers: new donation, event update

## Patterns

- **Strategy** — `NotificationStrategy` with `EmailStrategy`, `SmsStrategy`, `SocialStrategy`; `Notifier` holds the current one.
- **Observer** — `DonationSubject` & `DonationObserver` implemented by `DonationService` & `ThankYouNotificationObserver`.
- **Observer** — `EventSubject` & `EventObserver` implemented by `EventService` & `EventNotificationObserver`.

## Tables

`notifications` (log — Phase 2)

## Implemented Files

- `NotificationStrategy` — Interface for delivery channels.
- `EmailStrategy`, `SmsStrategy`, `SocialStrategy` — Concrete strategies.
- `Notifier` — Context class maintaining the active strategy and sending messages.
- `DonationSubject` & `DonationObserver` — Observer interfaces for donations.
- `ThankYouNotificationObserver` — Dispatches donor thank-you messages.
- `EventSubject` & `EventObserver` — Observer interfaces for event updates.
- `EventNotificationObserver` — Dispatches attendee update and closure notifications.

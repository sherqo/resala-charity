# Notification

Message channels, recipients and the notification log.

## Features (see `docs/design.md`)

- C1 send to one person
- C2 channel chosen at runtime: email / SMS / social (all mocked)
- C3 templates: receipt, thank-you, reminder, certificate
- C4 send to a group: event attendees, volunteers by skill, donors in a period
- C5 notification log: who, what, when, channel
- C6 automatic triggers: new donation, event update

## Patterns

- **Strategy** — `NotificationStrategy` with `EmailStrategy`, `SmsStrategy`,
  `SocialStrategy`; `Notifier` holds the current one
- **Observer** — home of `Subject` / `Observer`; donation and event services publish,
  a `NotificationObserver` subscribes and delegates to the current strategy

## Tables

`notifications` (log)

## Files to create

`NotificationStrategy`, `EmailStrategy`, `SmsStrategy`, `SocialStrategy`,
`Notifier`, `Subject`, `Observer`, `NotificationObserver`.

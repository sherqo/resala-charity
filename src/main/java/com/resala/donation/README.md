# Donation

Donors, donations, receipts, tax statements, payments.

## Features (see `docs/design.md`)

- D1 donor CRUD (individual/organization)
- D2 record donation: online / check / in-kind
- D3 list and filter by donor, date, type
- D4 donor history + yearly total
- D5 receipt per donation; D6 yearly tax statement per donor
- D7 online payment through a fake gateway (Phase 2)

## Patterns

- **Factory Method** — `DonationFactory` is the Creator; the three
  concrete factories create `OnlineDonation`, `CheckDonation`, or `InKindDonation`.
- **Decorator** — `BasicReceipt` is wrapped by `ThankYouReceiptDecorator`
  and, for money donations, `TaxReceiptDecorator`.
- **Observer** — `DonationService` is the Subject. The `notification` module
  provides `ThankYouNotificationObserver` after a save.
- **Adapter** — the `payment` module's `FakePaymentGatewayAdapter` adapts the fake provider to
  the app's `PaymentGateway` interface.
- **MVC + Abstract Factory** — `DonationController` has no console code;
  `UiFactory` creates a `DonationView`. `ConsoleUiFactory` is the current build.

## Tables

`donors`, `donations`; donation kinds are the fixed `DonationType` enum (no lookup table).
Donors are archived (`active=0`) rather than deleted, so donation history remains valid.

## Current console features

- add, search, edit, and archive donors
- record online, check, and in-kind donations
- list/filter donations by donor, type, and date range
- print a receipt and a yearly monetary statement

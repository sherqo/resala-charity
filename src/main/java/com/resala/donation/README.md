# Donation

Donors, donations, receipts, tax statements, payments.

## Features (see `docs/design.md`)

- D1 donor CRUD (individual/organization)
- D2 record donation: online / check / in-kind
- D3 list and filter by donor, date, type
- D4 donor history + yearly total
- D5 receipt per donation; D6 yearly tax statement per donor
- D7 online payment through a fake gateway (Phase 2)
- D8 donation-type lookup CRUD

## Patterns

- **Factory** — `DonationFactory` creates online / check / in-kind
- **Decorator** — `decorator/`: tax receipt, thank-you (stackable add-ons)
- **Observer** — `DonationService` notifies subscribers when a donation is added
- Phase 2: **Adapter** (`payment/`), **Template Method** (`report/`)

## Tables

`donors`, `donations`, `donation_types`

## Files to create

`Donation` (interface), `OnlineDonation`, `CheckDonation`, `InKindDonation`,
`DonationFactory`, `DonationRepository`, `DonationService`, then `decorator/*`,
`payment/*`, `report/*` as their phase starts.

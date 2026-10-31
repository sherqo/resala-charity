# Beneficiary

Beneficiaries, their needs, distributions and impact.

## Features (see `docs/design.md`)

- B1 beneficiary CRUD
- B2 need types (lookup table)
- B3 record distributions (items/services, quantity, date)
- B4 in-kind stock link: distribute items received as in-kind donations
- B5 history per beneficiary
- B6 impact metrics: people served, items/value distributed, per period

Every metric must be derived from stored rows; do not invent numbers.

## Patterns

- **Strategy Pattern**: `DistributionPolicy`
  - `DistributionPolicy` (interface)
  - `ByNeedDistributionPolicy` (proportional to household size)
  - `EqualDistributionPolicy` (equal allocation)
  - `FifoDistributionPolicy` (first-in first-out by registration order)
  - `BeneficiaryService` acts as the Context holding the active policy.

## Tables

`beneficiaries`, `need_types`, `distributions`

## Files to create

`BeneficiaryRepository`, `BeneficiaryService` (distribution logic lives here;
do not create a separate `DistributionService`).

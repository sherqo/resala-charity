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

None required. Optional second **Strategy**: `DistributionPolicy`
(by need / equal / FIFO) — only if the first Strategy is already solid.

## Tables

`beneficiaries`, `need_types`, `distributions`

## Done

- `beneficiaries` table (`src/main/resources/schema.sql`) — `name`, `phone`,
  `household_size` (>= 0), `need_type_id` FK to `need_types`, `status`
  (`ACTIVE`/`CLOSED`), `notes`
- `BeneficiaryRepository` — extends `JdbcRepository<Beneficiary>`, so it inherits
  the `CrudRepository` contract: `insert`, `update`, `deleteById`, `findById`,
  `findAll`. Adds the entity-specific filters `findByNeedType`, `findByStatus`,
  `searchByName` and `count`.

## Files to create

`BeneficiaryService` (distribution logic lives here;
do not create a separate `DistributionService`), plus the `distributions` table
and a `Distribution` model.

## Notes

- The repository only persists rows; validation belongs in the model.
- Failures come back as unchecked `IllegalStateException` carrying the SQLite
  message (FK violation, CHECK violation, NOT NULL violation).
- `deleteById` will start failing with a FK error once `distributions` rows
  reference a beneficiary.

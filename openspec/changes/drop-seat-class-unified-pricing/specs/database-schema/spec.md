# MODIFIED Requirements

## Requirement: Initial schema migration

The system SHALL provide Flyway migration files for core database tables. A migration `V8.0.0__drop_seat_class.sql` SHALL exist that drops the `seat_class` column from the `seats` table and the `seat_class_at_booking` column from the `booking_seats` table.

### Scenario: Drop migration file exists

- **WHEN** the database/migrations directory is examined
- **THEN** `database/migrations/V8.0.0__drop_seat_class.sql` SHALL exist

### Scenario: seats table has no seat_class column after migration

- **WHEN** the migration `V8.0.0__drop_seat_class.sql` is applied
- **THEN** the `seats` table SHALL contain columns `id`, `train_id`, `seat_number`, and `created_at` only — `seat_class` SHALL NOT exist

### Scenario: booking_seats table has no seat_class_at_booking column after migration

- **WHEN** the migration `V8.0.0__drop_seat_class.sql` is applied
- **THEN** the `booking_seats` table SHALL contain columns `booking_id`, `seat_id`, and `price_at_booking` only — `seat_class_at_booking` SHALL NOT exist

# REMOVED Requirements

## Requirement: seat_class column on seats table

**Reason**: Pricing is now unified — all seats on a route share `route.base_price`. Seat classification adds complexity with no business justification at this stage.

**Migration**: No data migration required (no production data). Column is dropped via `V8.0.0__drop_seat_class.sql`.

## Requirement: seat_class_at_booking snapshot on booking_seats table

**Reason**: The seat class snapshot is no longer meaningful after `seat_class` is removed from seats. The price snapshot (`price_at_booking`) alone is sufficient to reconstruct booking value.

**Migration**: Column dropped via `V8.0.0__drop_seat_class.sql`. Existing data is not preserved.

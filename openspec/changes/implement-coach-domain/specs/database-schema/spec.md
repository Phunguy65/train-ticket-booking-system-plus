# ADDED Requirements

## Requirement: Train-cars intermediate table

The system SHALL introduce a `train_cars` table in the database to represent the intermediate layer between `trains` and `seats`. The table SHALL have columns: `id` (UUID, PK, default `uuidv7()`), `train_id` (UUID, FK to `trains.id`, NOT NULL), `car_number` (INTEGER, NOT NULL, > 0), `total_seats` (INTEGER, NOT NULL, > 0), `created_at` (TIMESTAMPTZ, NOT NULL), and `deleted_at` (TIMESTAMPTZ, nullable for soft delete).

### Scenario: train_cars table exists after V12 migration

- **WHEN** migration `V12_0_0__add_coaches_table.sql` is applied
- **THEN** the `train_cars` table exists with all required columns and constraints

### Scenario: Unique car number per train (active only)

- **WHEN** two active coaches are inserted for the same train with the same `car_number`
- **THEN** the database rejects the second insert with a unique constraint violation

### Scenario: Soft-deleted coach does not conflict with unique index

- **WHEN** a coach with `(train_id, car_number)` is soft-deleted (deleted_at IS NOT NULL)
- **THEN** a new active coach with the same `(train_id, car_number)` can be inserted without constraint violation

## Requirement: Seats reference coaches instead of trains

The system SHALL update the `seats` table so that each seat references its parent `train_car` via a `train_car_id` (UUID, FK to `train_cars.id`, NOT NULL), replacing the previous `train_id` FK column. Seat number uniqueness SHALL be scoped to `(train_car_id, seat_number)` for active seats.

### Scenario: seats.train_car_id column exists after V12 migration

- **WHEN** migration `V12_0_0__add_coaches_table.sql` is applied
- **THEN** `seats.train_car_id` column exists as NOT NULL UUID with FK to `train_cars.id`

### Scenario: seats.train_id column is removed after V12 migration

- **WHEN** migration `V12_0_0__add_coaches_table.sql` is applied
- **THEN** `seats.train_id` column no longer exists

### Scenario: Seat number uniqueness is scoped to coach

- **WHEN** two active seats with the same `seat_number` belong to different coaches of the same train
- **THEN** both inserts succeed without unique constraint violation

# MODIFIED Requirements

## Requirement: Initial schema migration

The system SHALL provide an initial migration file for core database tables. The V1 schema defines `trains`, `routes`, `stations`, `seats`, `bookings`, and `users`. From V12 onward, the `seats` table no longer includes a `train_id` column; instead seats reference `train_cars` via `train_car_id`.

### Scenario: Initial migration file exists

- **WHEN** project initialization completes
- **THEN** `database/migrations/V1_0_0__initial_schema.sql` exists

### Scenario: Core tables defined in V1

- **WHEN** `V1_0_0__initial_schema.sql` is examined
- **THEN** it contains CREATE TABLE statements for users, trains, routes, stations, seats, and bookings

### Scenario: seats references train_cars after V12

- **WHEN** all migrations through V12 are applied
- **THEN** the `seats` table has a `train_car_id` column referencing `train_cars.id` and no `train_id` column

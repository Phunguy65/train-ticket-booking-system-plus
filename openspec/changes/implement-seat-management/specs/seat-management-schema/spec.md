## ADDED Requirements

### Requirement: V4.0.0 migration removes seats.status column and its index

Flyway migration `V4.0.0__implement_seat_management.sql` SHALL remove the `status` column from the `seats` table and drop the index `idx_seats_train_status` that was created over `(train_id, status)`.

#### Scenario: seats.status column is absent after migration

- **WHEN** V4.0.0 migration runs successfully
- **THEN** the `seats` table SHALL NOT contain a `status` column

#### Scenario: idx_seats_train_status index is absent after migration

- **WHEN** V4.0.0 migration runs successfully
- **THEN** no index named `idx_seats_train_status` SHALL exist on the `seats` table

### Requirement: V4.0.0 migration adds route_seat_availability table

The same migration SHALL create the `route_seat_availability` table with a composite primary key `(route_id, seat_id)`, a `status` CHECK constraint, and a `version` column for optimistic locking.

#### Scenario: route_seat_availability table exists after migration

- **WHEN** V4.0.0 migration runs successfully
- **THEN** the `route_seat_availability` table SHALL exist with columns: `route_id` (UUID FK → routes), `seat_id` (UUID FK → seats), `status` (VARCHAR(20), DEFAULT 'AVAILABLE'), `version` (INTEGER, DEFAULT 0)

#### Scenario: status column only allows valid enum values

- **WHEN** an INSERT is attempted with `status = 'INVALID_VALUE'`
- **THEN** the database SHALL reject the insert with a CHECK constraint violation

#### Scenario: Composite primary key prevents duplicate (route_id, seat_id) entries

- **WHEN** two INSERT statements with the same `(route_id, seat_id)` pair are executed
- **THEN** the second INSERT SHALL fail with a primary key violation

#### Scenario: Performance index on (route_id, status) exists

- **WHEN** V4.0.0 migration runs successfully
- **THEN** an index `idx_route_seat_status` SHALL exist on `route_seat_availability (route_id, status)`

## MODIFIED Requirements

### Requirement: Database indexes

The system SHALL define indexes for performance-critical queries.

#### Scenario: Indexes created

- **WHEN** all migrations through V4.0.0 have been applied
- **THEN** the following indexes SHALL exist:
  - `idx_bookings_user` on `bookings(user_id)`
  - `idx_routes_train` on `routes(train_id)`
  - `idx_routes_departure` on `routes(departure_time)`
  - `idx_route_seat_status` on `route_seat_availability(route_id, status)`
- **AND** `idx_seats_train_status` on `seats(train_id, status)` SHALL NOT exist (removed in V4.0.0)

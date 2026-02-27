# MODIFIED Requirements

## Requirement: Initial schema migration

The system SHALL provide migration files that evolve the booking and seat availability schema to support multi-seat holds with price snapshots.

### Scenario: V5 migration adds HELD to route_seat_availability status constraint

- **WHEN** migration `V5__add_held_seat_status.sql` is applied
- **THEN** the CHECK constraint on `route_seat_availability.status` SHALL be updated to `CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'CANCELLED'))`

### Scenario: V6 migration creates booking_seats table and migrates seat data

- **WHEN** migration `V6__add_booking_seats_table.sql` is applied
- **THEN** a `booking_seats` table SHALL exist with columns: `booking_id UUID NOT NULL REFERENCES bookings(id)`, `seat_id UUID NOT NULL REFERENCES seats(id)`, `price_at_booking DECIMAL(10,2) NOT NULL`, `seat_class_at_booking VARCHAR(20) NOT NULL`, and `PRIMARY KEY (booking_id, seat_id)`; existing rows in `bookings` with a non-null `seat_id` SHALL have corresponding rows inserted into `booking_seats` before the `seat_id` column is dropped from `bookings`

### Scenario: V6 migration drops bookings.seat_id after data migration

- **WHEN** migration `V6__add_booking_seats_table.sql` completes
- **THEN** the `bookings` table SHALL NOT contain a `seat_id` column

### Scenario: V7 migration adds partial unique index for active holds per user-route

- **WHEN** migration `V7__add_unique_active_hold_index.sql` is applied
- **THEN** a partial unique index `idx_one_active_hold_per_user_route` SHALL exist on `bookings(user_id, route_id) WHERE status = 'HELD'`

### Scenario: V8 migration updates bookings status constraint and migrates PENDING rows

- **WHEN** migration `V8__update_booking_status_constraint.sql` is applied
- **THEN** the CHECK constraint on `bookings.status` SHALL be updated to `CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED'))`; all existing rows with `status = 'PENDING'` SHALL be updated to `status = 'HELD'`

## Requirement: Database indexes

The system SHALL define indexes for performance-critical queries including hold expiry polling.

### Scenario: Index created for hold expiry polling

- **WHEN** migration `V6__add_booking_seats_table.sql` is applied
- **THEN** an index SHALL exist on `bookings(status, payment_deadline)` to support efficient polling of expired holds by the scheduled job

### Scenario: Existing indexes remain intact

- **WHEN** all new migrations are applied
- **THEN** all previously defined indexes (e.g., on `bookings(user_id)`) SHALL still exist and remain valid

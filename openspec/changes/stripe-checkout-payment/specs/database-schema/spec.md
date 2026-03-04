# ADDED Requirements

## Requirement: Flyway migration adds payments table and checkout_session_id to bookings

The system SHALL provide a new Flyway migration that creates a `payments` table for tracking Stripe payment records and adds a `checkout_session_id` column to the `bookings` table.

### Scenario: payments table created with correct schema

- **WHEN** the new Flyway migration runs
- **THEN** a `payments` table SHALL exist with columns: `id UUID PRIMARY KEY`, `booking_id UUID NOT NULL REFERENCES bookings(id)`, `checkout_session_id VARCHAR(255) NOT NULL UNIQUE`, `stripe_event_id VARCHAR(255) UNIQUE`, `amount DECIMAL(10,2) NOT NULL`, `currency VARCHAR(10) NOT NULL DEFAULT 'vnd'`, `status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PAID','CANCELLED'))`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

### Scenario: bookings table gains checkout_session_id column

- **WHEN** the new Flyway migration runs
- **THEN** the `bookings` table SHALL have a `checkout_session_id VARCHAR(255)` column (nullable for backward compatibility with existing rows)

### Scenario: Index on payments.booking_id exists

- **WHEN** the migration runs
- **THEN** an index SHALL exist on `payments(booking_id)` for efficient lookup by booking

### Scenario: Index on payments.checkout_session_id exists

- **WHEN** the migration runs
- **THEN** an index SHALL exist on `payments(checkout_session_id)` for webhook-to-payment lookup

### Scenario: Index on bookings.checkout_session_id exists

- **WHEN** the migration runs
- **THEN** an index SHALL exist on `bookings(checkout_session_id)` for reconciliation job queries

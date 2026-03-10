# ADDED Requirements

## Requirement: Checkout session created on booking

When a booking is created in HELD status, the system SHALL automatically create a Stripe Checkout Session and persist a Payment record in PENDING status.

### Scenario: Checkout session created successfully

- **WHEN** a `BookingCreated` domain event is published
- **THEN** the system creates a Stripe Checkout Session with `expires_at = now + 30 minutes`
- **THEN** the session metadata contains `bookingId` and `userId`
- **THEN** a `Payment` record is saved with `status = PENDING`, `checkoutSessionId`, and `bookingId`

### Scenario: Checkout URL returned to client

- **WHEN** a booking is created via `POST /api/v1/bookings`
- **THEN** the response includes a `checkoutUrl` field pointing to the Stripe-hosted checkout page

### Scenario: Duplicate checkout session prevented

- **WHEN** a `BookingCreated` event is received for a `bookingId` that already has a `Payment` record
- **THEN** the system skips session creation and returns without error

## Requirement: Payment status queryable by booking owner

The system SHALL expose an endpoint for authenticated users to query the payment status for their booking.

### Scenario: Owner queries payment status

- **WHEN** an authenticated user calls `GET /api/v1/payments/{bookingId}`
- **AND** the booking belongs to that user
- **THEN** the response returns `{ bookingId, status, checkoutUrl, amount, currency }`

### Scenario: Non-owner access denied

- **WHEN** an authenticated user calls `GET /api/v1/payments/{bookingId}`
- **AND** the booking does NOT belong to that user
- **THEN** the system returns HTTP 403

### Scenario: Payment not found

- **WHEN** `GET /api/v1/payments/{bookingId}` is called for a booking with no payment record
- **THEN** the system returns HTTP 404

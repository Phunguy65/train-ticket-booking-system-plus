# ADDED Requirements

## Requirement: Full refund issued on confirmed booking cancellation

The system SHALL automatically issue a full Stripe refund when a confirmed booking is cancelled.

### Scenario: Refund issued on cancellation of confirmed booking

- **WHEN** a `BookingCancelled` domain event is received with `requiresRefund = true`
- **AND** a Payment record exists for the booking with `status = PAID`
- **THEN** the system calls the Stripe Refunds API for the full amount
- **THEN** the Payment record status transitions to REFUNDED

### Scenario: No refund for unconfirmed booking cancellation

- **WHEN** a `BookingCancelled` domain event is received with `requiresRefund = false`
- **THEN** the system does NOT call the Stripe Refunds API
- **THEN** the Payment record status (if exists) transitions to CANCELLED

## Requirement: Refund operation is idempotent

The system SHALL use a deterministic idempotency key when calling the Stripe Refunds API to prevent duplicate refunds.

### Scenario: Idempotency key used for refund

- **WHEN** `RefundPaymentUseCase` calls `stripe.refunds.create()`
- **THEN** the idempotency key is set to `refund_{bookingId}`
- **THEN** if the same refund is attempted again, Stripe returns the cached result without issuing a second refund

## Requirement: Refund failure logged for manual intervention

The system SHALL log refund failures with sufficient context for manual resolution.

### Scenario: Stripe refund API call fails

- **WHEN** `stripe.refunds.create()` throws an exception
- **THEN** the system logs the error with `bookingId`, `paymentIntentId`, and exception details
- **THEN** the Payment record status is NOT changed to REFUNDED
- **THEN** the system does NOT throw an exception that would prevent the booking cancellation from completing

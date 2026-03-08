# ADDED Requirements

## Requirement: Webhook signature verified before processing

The system SHALL verify the Stripe webhook signature on every incoming request to `POST /api/v1/webhooks/stripe`.

### Scenario: Valid signature accepted

- **WHEN** a POST request arrives at `/api/v1/webhooks/stripe` with a valid `Stripe-Signature` header
- **THEN** the system processes the event

### Scenario: Invalid signature rejected

- **WHEN** a POST request arrives with an invalid or missing `Stripe-Signature` header
- **THEN** the system returns HTTP 400 and does not process the event

## Requirement: Payment success confirms booking and seats

The system SHALL confirm the booking and transition seats from HELD to BOOKED when a `checkout.session.completed` event is received.

### Scenario: Successful payment confirmation

- **WHEN** a `checkout.session.completed` webhook event is received
- **AND** the associated booking is in HELD status
- **THEN** the booking status transitions to CONFIRMED
- **THEN** all seats associated with the booking transition from HELD to BOOKED
- **THEN** the `Payment` record status transitions to PAID
- **THEN** `stripe_payment_intent_id` is stored on the Payment record

### Scenario: Payment arrives after hold expiry

- **WHEN** a `checkout.session.completed` webhook event is received
- **AND** the associated booking is in CANCELLED status (hold expired)
- **THEN** the system issues a full Stripe refund immediately
- **THEN** the `Payment` record status is set to REFUNDED
- **THEN** no seat transitions occur

## Requirement: Webhook events processed idempotently

The system SHALL not process the same Stripe event more than once.

### Scenario: Duplicate webhook ignored

- **WHEN** a webhook event with a `stripe_event_id` that already exists in the `payments` table is received
- **THEN** the system returns HTTP 200 without reprocessing the event

## Requirement: Checkout session expiry handled

The system SHALL update the Payment record when a Stripe session expires.

### Scenario: Session expired event received

- **WHEN** a `checkout.session.expired` webhook event is received
- **AND** the associated Payment record is in PENDING status
- **THEN** the Payment record status transitions to CANCELLED

## Requirement: Payment failure recorded

The system SHALL record payment failures for observability.

### Scenario: Payment failed event received

- **WHEN** a `payment_intent.payment_failed` webhook event is received
- **THEN** the Payment record status transitions to FAILED
- **THEN** the `error_message` field is populated with the failure reason

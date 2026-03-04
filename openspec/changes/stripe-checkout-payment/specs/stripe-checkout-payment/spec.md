# ADDED Requirements

## Requirement: CheckoutSessionPort abstracts Stripe session operations

The `payment` module SHALL define a `CheckoutSessionPort` interface in `payment/application/port/` that abstracts all Stripe Checkout Session operations. No Stripe SDK types SHALL appear in this interface's method signatures — only domain/primitive types.

### Scenario: Port interface uses only domain types

- **WHEN** examining `CheckoutSessionPort`
- **THEN** all method parameters and return types SHALL be domain types or primitives (`String`, `Long`, `BookingId`, `CheckoutSessionId`) — never `com.stripe.*` types

---

## Requirement: StripeCheckoutAdapter implements CheckoutSessionPort

The `StripeCheckoutAdapter` in `payment/infrastructure/stripe/` SHALL implement `CheckoutSessionPort` using the `stripe-java` SDK. It SHALL be annotated `@Component` and inject `StripeProperties` for API key and URL configuration.

### Scenario: Session created with correct parameters

- **WHEN** `createSession(bookingId, amountVnd, successUrl, cancelUrl, idempotencyKey)` is called
- **THEN** the adapter SHALL call `stripe.checkout.sessions.create()` with `mode = "payment"`, `currency = "vnd"`, `amount` as a zero-decimal integer, `expires_at = NOW + 30 minutes`, `metadata.booking_id = bookingId`, and `after_expiration.recovery.enabled = false`

### Scenario: VND amount is not multiplied by 100

- **WHEN** `createSession` is called with `amountVnd = 300000`
- **THEN** the Stripe API call SHALL use `unit_amount = 300000` — NOT `30000000`

### Scenario: Session expiry set to Stripe minimum

- **WHEN** `createSession` is called
- **THEN** `expires_at` SHALL be set to `Instant.now().plus(30, MINUTES)` as a Unix timestamp

### Scenario: Idempotency key passed to Stripe

- **WHEN** `createSession` is called with an `idempotencyKey`
- **THEN** the Stripe API call SHALL include the idempotency key via `RequestOptions.builder().setIdempotencyKey(idempotencyKey).build()`

### Scenario: Session expired via expire API

- **WHEN** `expireSession(checkoutSessionId)` is called
- **THEN** the adapter SHALL call `stripe.checkout.sessions.expire(sessionId)` and return without error

### Scenario: Session status retrieved for reconciliation

- **WHEN** `retrieveSession(checkoutSessionId)` is called
- **THEN** the adapter SHALL call `stripe.checkout.sessions.retrieve(sessionId)` and return the session `status` as a `CheckoutSessionStatus` enum (`OPEN`, `COMPLETE`, `EXPIRED`)

---

## Requirement: Payment aggregate tracks Checkout Session lifecycle

The `Payment` aggregate in `payment/domain/model/` SHALL extend `AggregateRoot<PaymentId>` and track the association between a booking and its Stripe Checkout Session.

### Scenario: Payment created with PENDING status

- **WHEN** `Payment.create(bookingId, checkoutSessionId, amountVnd)` is called
- **THEN** the resulting `Payment` SHALL have `status = PENDING` and a `PaymentCreated` domain event SHALL be registered

### Scenario: Payment confirmed transitions to PAID

- **WHEN** `payment.confirm(stripeEventId)` is called on a `PENDING` payment
- **THEN** `status` SHALL change to `PAID` and a `PaymentConfirmed` domain event SHALL be registered

### Scenario: Payment cancelled transitions to CANCELLED

- **WHEN** `payment.cancel(stripeEventId)` is called on a `PENDING` payment
- **THEN** `status` SHALL change to `CANCELLED` and a `PaymentCancelled` domain event SHALL be registered

### Scenario: Confirming a non-PENDING payment is a no-op (idempotent)

- **WHEN** `payment.confirm(stripeEventId)` is called on a payment already in `PAID` status
- **THEN** no state change SHALL occur and no domain event SHALL be registered

---

## Requirement: StripeWebhookController handles Checkout Session events

The `StripeWebhookController` in `payment/infrastructure/web/` SHALL expose `POST /api/v1/webhooks/stripe` and process `checkout.session.completed` and `checkout.session.expired` events. It SHALL verify the Stripe webhook signature before processing.

### Scenario: Webhook endpoint requires raw request body

- **WHEN** `POST /api/v1/webhooks/stripe` is configured
- **THEN** the endpoint SHALL consume `application/json` with the raw byte body (not parsed) to allow signature verification

### Scenario: Invalid signature returns 400

- **WHEN** a request arrives at the webhook endpoint with a missing or invalid `Stripe-Signature` header
- **THEN** the controller SHALL return `400 Bad Request` without processing the event

### Scenario: checkout.session.completed triggers booking confirmation

- **WHEN** a valid `checkout.session.completed` event is received with `payment_status = "paid"`
- **THEN** the controller SHALL extract `metadata.booking_id`, call `ConfirmBookingOnPaymentUseCase`, and return `200 OK`

### Scenario: checkout.session.expired triggers booking cancellation

- **WHEN** a valid `checkout.session.expired` event is received
- **THEN** the controller SHALL extract `metadata.booking_id`, call `CancelBookingOnExpiryUseCase`, and return `200 OK`

### Scenario: Duplicate webhook events are idempotent

- **WHEN** the same Stripe event ID is received twice
- **THEN** the second invocation SHALL detect the already-processed event (via `stripe_event_id` in `payments` table) and return `200 OK` without re-processing

---

## Requirement: ConfirmBookingOnPaymentUseCase confirms booking after successful payment

The `ConfirmBookingOnPaymentUseCase` in `payment/application/usecase/` SHALL confirm the payment record and delegate booking confirmation to the `booking` module via `ConfirmSeatHoldUseCase`.

### Scenario: Booking confirmed on payment success

- **WHEN** `execute(bookingId, stripeEventId)` is called
- **THEN** the `Payment` record SHALL transition to `PAID`, `ConfirmSeatHoldUseCase` SHALL be called with the `bookingId`, and seats SHALL transition to `BOOKED`

### Scenario: Already-confirmed booking is skipped

- **WHEN** `execute` is called for a booking already in `CONFIRMED` status
- **THEN** the use case SHALL return successfully without re-processing

---

## Requirement: CancelBookingOnExpiryUseCase cancels booking when session expires

The `CancelBookingOnExpiryUseCase` in `payment/application/usecase/` SHALL cancel the payment record and delegate booking cancellation to the `booking` module via `CancelBookingUseCase`.

### Scenario: Booking cancelled on session expiry

- **WHEN** `execute(bookingId, stripeEventId)` is called
- **THEN** the `Payment` record SHALL transition to `CANCELLED`, `CancelBookingUseCase` SHALL be called, and seats SHALL transition to `AVAILABLE`

### Scenario: Already-cancelled booking is skipped

- **WHEN** `execute` is called for a booking already in `CANCELLED` status
- **THEN** the use case SHALL return successfully without re-processing

---

## Requirement: PaymentReconciliationJob reconciles stale HELD bookings

The `PaymentReconciliationJob` in `payment/infrastructure/job/` SHALL run every 5 minutes and reconcile bookings in `HELD` status whose `created_at` is older than 35 minutes by retrieving the Checkout Session status from Stripe.

### Scenario: Expired session triggers cancellation

- **WHEN** the job finds a `HELD` booking older than 35 minutes whose Stripe session status is `EXPIRED`
- **THEN** `CancelBookingOnExpiryUseCase` SHALL be called for that booking

### Scenario: Completed session triggers confirmation

- **WHEN** the job finds a `HELD` booking older than 35 minutes whose Stripe session status is `COMPLETE`
- **THEN** `ConfirmBookingOnPaymentUseCase` SHALL be called for that booking

### Scenario: Open session is left unchanged

- **WHEN** the job finds a `HELD` booking older than 35 minutes whose Stripe session status is `OPEN`
- **THEN** no state change SHALL occur

---

## Requirement: ExpireCheckoutSessionOnCancelListener expires session when booking is cancelled

An `@ApplicationModuleListener` in the `payment` module SHALL listen for `BookingCancelled` domain events published by the `booking` module and call `CheckoutSessionPort.expireSession()`.

### Scenario: Session expired on booking cancellation

- **WHEN** a `BookingCancelled` event is published with a `checkoutSessionId`
- **THEN** the listener SHALL call `expireSession(checkoutSessionId)` on the Stripe adapter

### Scenario: Missing session ID is handled gracefully

- **WHEN** a `BookingCancelled` event has no `checkoutSessionId` (legacy booking without payment)
- **THEN** the listener SHALL log a warning and return without error

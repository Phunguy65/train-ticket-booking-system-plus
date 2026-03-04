# MODIFIED Requirements

## Requirement: Booking domain model enforces booking lifecycle invariants

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` from the shared kernel and enforce all booking business rules. Direct field mutation from outside the aggregate is PROHIBITED — all state changes SHALL go through domain methods.

Booking lifecycle states: `HELD` → `CONFIRMED` → `CANCELLED`.

### Scenario: New booking starts in HELD status

- **WHEN** `Booking.create(userId, routeId, seatIds, totalPrice, checkoutSessionId, idempotencyKey)` factory method is called
- **THEN** the resulting booking SHALL have `status = HELD`, a `paymentDeadline` of `null` (session expiry is owned by Stripe), a `checkoutSessionId` field populated, and a `BookingHeld` domain event SHALL be registered on the aggregate

### Scenario: Confirming a HELD booking transitions to CONFIRMED

- **WHEN** `booking.confirm(paymentReference)` is called on a booking with status `HELD`
- **THEN** the status SHALL change to `CONFIRMED`, `paymentReference` SHALL be set, and a `BookingConfirmed` domain event SHALL be registered

### Scenario: Confirming a non-HELD booking is rejected

- **WHEN** `booking.confirm()` is called on a booking with status `CONFIRMED` or `CANCELLED`
- **THEN** a `BookingError.CannotConfirm` SHALL be returned via `Result`

### Scenario: Cancelling a booking transitions to CANCELLED

- **WHEN** `booking.cancel()` is called on a booking with status `HELD` or `CONFIRMED`
- **THEN** the status SHALL change to `CANCELLED` and a `BookingCancelled` domain event SHALL be registered with the `checkoutSessionId`

### Scenario: Cancelling an already-cancelled booking is rejected

- **WHEN** `booking.cancel()` is called on a booking with status `CANCELLED`
- **THEN** a `BookingError.AlreadyCancelled` SHALL be returned via `Result`

---

## Requirement: CreateSeatHoldUseCase orchestrates hold creation and Checkout Session atomically

The `CreateSeatHoldUseCase` SHALL acquire pessimistic locks on requested seats, create the `Booking` aggregate in `HELD` status, call `CheckoutSessionPort.createSession()` within the same transaction, persist the booking with the returned `checkoutSessionId`, and return a `HoldDto` containing `checkoutUrl`.

### Scenario: Successful hold returns checkoutUrl

- **WHEN** `execute(CreateSeatHoldCommand)` is called with valid userId, routeId, seatIds
- **THEN** a `Booking` SHALL be created in `HELD` status, a Stripe Checkout Session SHALL be created, and the response `HoldDto` SHALL include `bookingId`, `checkoutUrl`, `checkoutSessionId`, and `expiresAt` (session `expires_at`)

### Scenario: Stripe failure rolls back the hold

- **WHEN** `CheckoutSessionPort.createSession()` throws an exception
- **THEN** the transaction SHALL roll back, no `Booking` record SHALL be persisted, and seats SHALL remain `AVAILABLE`

### Scenario: Idempotent hold returns existing booking

- **WHEN** `execute` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing `HoldDto` without creating a new booking or Checkout Session

---

## Requirement: CancelBookingUseCase expires the Checkout Session

The `CancelBookingUseCase` SHALL transition the booking to `CANCELLED`, publish `BookingCancelled` (which carries `checkoutSessionId`), and rely on `ExpireCheckoutSessionOnCancelListener` to call `CheckoutSessionPort.expireSession()`.

### Scenario: Cancellation publishes BookingCancelled with session ID

- **WHEN** `CancelBookingUseCase.execute(bookingId)` is called on a `HELD` booking
- **THEN** the booking SHALL transition to `CANCELLED` and a `BookingCancelled` event SHALL be published containing the `checkoutSessionId`

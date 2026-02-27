# ADDED Requirements

## Requirement: CreateSeatHoldUseCase orchestrates multi-seat hold within a transaction

The `CreateSeatHoldUseCase` class SHALL be annotated with `@Service` and its `execute()` method SHALL be annotated with `@Transactional`. It SHALL accept a `CreateSeatHoldCommand` containing `userId`, `routeId`, a non-empty `List<UUID> seatIds`, and an `idempotencyKey`. It SHALL perform the following operations in order within a single transaction: idempotency check, active-hold check, price calculation, pessimistic seat lock + hold, booking creation, and domain event publication.

### Scenario: Successful hold creation returns HoldDto with price breakdown

- **WHEN** `CreateSeatHoldUseCase.execute(command)` is called with valid `userId`, `routeId`, and a list of available `seatIds`
- **THEN** all specified seats SHALL be transitioned to `HELD`, a `Booking` aggregate SHALL be created with `status = HELD`, `paymentDeadline = Instant.now() + 15 minutes`, and per-seat price data stored in `booking_seats`; the use case SHALL return a `HoldDto` containing `bookingId`, `status = HELD`, `seats[]` (each with `seatId`, `seatClass`, `unitPrice`), `totalPrice`, `currency`, and `expiresAt`

### Scenario: Hold creation is idempotent with idempotency key

- **WHEN** `CreateSeatHoldUseCase.execute()` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing `HoldDto` without creating a new booking or re-locking any seats

### Scenario: Hold rejected when user already has active hold on same route

- **WHEN** `CreateSeatHoldUseCase.execute()` is called for `userId` X and `routeId` Y while a booking with `status = HELD` already exists for that same `(userId, routeId)` pair
- **THEN** the use case SHALL return `BookingError.ActiveHoldExists` and no seats SHALL be modified

### Scenario: Hold rejected when any seat is unavailable

- **WHEN** `CreateSeatHoldUseCase.execute()` is called and one or more `seatIds` have `status != AVAILABLE` in `route_seat_availability`
- **THEN** the use case SHALL return `BookingError.SeatsNotAvailable` containing the list of unavailable seat IDs, and NO seats SHALL be transitioned (all-or-nothing)

### Scenario: Hold rejected on lock timeout

- **WHEN** `CreateSeatHoldUseCase.execute()` attempts to acquire pessimistic locks on seats but cannot do so within 3 seconds (another transaction holds the locks)
- **THEN** the use case SHALL return `BookingError.SeatsLocked` and the caller receives `409 Conflict`

### Scenario: SeatHoldCreated domain event is registered

- **WHEN** hold creation succeeds and the transaction commits
- **THEN** a `SeatHoldCreated` domain event SHALL be published containing `bookingId`, `userId`, `routeId`, `seatIds`, and `expiresAt`

## Requirement: ConfirmSeatHoldUseCase transitions a held booking to confirmed

The `ConfirmSeatHoldUseCase` SHALL accept a `ConfirmSeatHoldCommand` containing `bookingId` and `paymentReference`. It SHALL validate that the booking exists, is in `HELD` status, and that `paymentDeadline` has not passed before transitioning.

### Scenario: Successful confirmation returns BookingDto with CONFIRMED status

- **WHEN** `ConfirmSeatHoldUseCase.execute(command)` is called on a booking with `status = HELD` and `paymentDeadline > NOW()`
- **THEN** all `booking_seats` rows SHALL remain unchanged, all associated `RouteSeatAvailability` rows SHALL be transitioned to `BOOKED`, the booking `status` SHALL become `CONFIRMED`, and a `BookingConfirmed` domain event SHALL be published

### Scenario: Confirmation rejected when hold is expired

- **WHEN** `ConfirmSeatHoldUseCase.execute()` is called on a booking with `status = HELD` but `paymentDeadline < NOW()`
- **THEN** the use case SHALL return `BookingError.HoldExpired` and SHALL trigger the hold expiry flow (seats released to `AVAILABLE`, booking set to `CANCELLED`)

### Scenario: Confirmation rejected when booking is not in HELD status

- **WHEN** `ConfirmSeatHoldUseCase.execute()` is called on a booking with `status = CONFIRMED` or `CANCELLED`
- **THEN** the use case SHALL return `BookingError.InvalidStatusTransition` with the current status

## Requirement: CancelBookingUseCase releases held or confirmed bookings

The `CancelBookingUseCase` SHALL accept a `CancelBookingCommand` containing `bookingId`. It SHALL release any `HELD` seats back to `AVAILABLE` and transition the booking to `CANCELLED`.

### Scenario: Cancelling a HELD booking releases all seats

- **WHEN** `CancelBookingUseCase.execute(command)` is called on a booking with `status = HELD`
- **THEN** all associated `RouteSeatAvailability` rows SHALL be transitioned from `HELD` to `AVAILABLE`, the booking `status` SHALL become `CANCELLED`, and a `BookingCancelled` event SHALL be published

### Scenario: Cancelling a CONFIRMED booking transitions seats to CANCELLED

- **WHEN** `CancelBookingUseCase.execute(command)` is called on a booking with `status = CONFIRMED`
- **THEN** all associated `RouteSeatAvailability` rows SHALL be transitioned to `CANCELLED`, the booking `status` SHALL become `CANCELLED`, and a `BookingCancelled` event SHALL be published

### Scenario: Cancelling an already-cancelled booking is idempotent

- **WHEN** `CancelBookingUseCase.execute(command)` is called on a booking with `status = CANCELLED`
- **THEN** the use case SHALL return success without modifying any state

## Requirement: ExpireHoldsJob releases stale holds via scheduled polling

A Spring `@Scheduled` job SHALL run with `fixedDelay = 60_000` ms. It SHALL query for all bookings with `status = HELD` and `payment_deadline < NOW()` (batch size 100) and for each expired booking: acquire a pessimistic write lock, verify the booking is still `HELD`, release all associated seats to `AVAILABLE`, and transition the booking to `CANCELLED`.

### Scenario: Expired hold releases seats and cancels booking

- **WHEN** a booking with `status = HELD` has `payment_deadline` that is more than 0 seconds in the past
- **THEN** within 60 seconds of expiry, the scheduled job SHALL transition all its `RouteSeatAvailability` rows to `AVAILABLE` and the booking to `CANCELLED`, and SHALL publish a `SeatHoldExpired` event

### Scenario: Concurrent confirmation and expiry job — confirmation wins

- **WHEN** the expiry job and `ConfirmSeatHoldUseCase` attempt to process the same booking simultaneously
- **THEN** only one transaction SHALL succeed; the other SHALL detect the changed status and abort without data corruption

## Requirement: RouteSeatAvailabilityPort extended to support batch hold operations

The `RouteSeatAvailabilityPort` interface SHALL add three new methods: `holdSeats(RouteId, List<SeatId>)`, `confirmHeldSeats(RouteId, List<SeatId>)`, and `releaseHeldSeats(RouteId, List<SeatId>)`. The adapter implementation SHALL sort seat IDs ascending before acquiring pessimistic locks.

### Scenario: holdSeats locks and transitions all seats atomically

- **WHEN** `holdSeats(routeId, seatIds)` is called inside a transaction
- **THEN** all specified `RouteSeatAvailability` rows SHALL be locked with `PESSIMISTIC_WRITE`, validated as `AVAILABLE`, and transitioned to `HELD` atomically; if any seat is not `AVAILABLE`, all return a failure result with no mutations

### Scenario: releaseHeldSeats restores seats to AVAILABLE

- **WHEN** `releaseHeldSeats(routeId, seatIds)` is called inside a transaction
- **THEN** all specified `RouteSeatAvailability` rows with `status = HELD` SHALL be transitioned to `AVAILABLE`

### Scenario: confirmHeldSeats transitions HELD seats to BOOKED

- **WHEN** `confirmHeldSeats(routeId, seatIds)` is called inside a transaction
- **THEN** all specified `RouteSeatAvailability` rows with `status = HELD` SHALL be transitioned to `BOOKED`

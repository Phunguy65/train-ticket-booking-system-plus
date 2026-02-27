# MODIFIED Requirements

## Requirement: Booking domain model enforces booking lifecycle invariants

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` from the shared kernel and enforce all booking business rules. Direct field mutation from outside the aggregate is PROHIBITED — all state changes SHALL go through domain methods.

The aggregate SHALL carry a `List<BookedSeat>` value objects (each containing `SeatId`, `unitPrice: BigDecimal`, `seatClass: SeatClass`) instead of a single `SeatId` field. A `paymentDeadline: Instant` field SHALL be present and non-null on all `HELD` bookings.

Booking lifecycle states: `HELD` → `CONFIRMED` → `CANCELLED`.

### Scenario: New booking hold starts in HELD status

- **WHEN** `Booking.createHold(userId, routeId, bookedSeats, totalPrice, currency, paymentDeadline, idempotencyKey)` factory method is called
- **THEN** the resulting booking SHALL have `status = HELD`, a non-empty `bookedSeats` list, a `paymentDeadline` set to `Instant.now() + 15 minutes`, and a `SeatHoldCreated` domain event SHALL be registered on the aggregate

### Scenario: Confirming a HELD booking transitions to CONFIRMED

- **WHEN** `booking.confirm(paymentReference)` is called on a booking with `status = HELD` and `paymentDeadline > Instant.now()`
- **THEN** the status SHALL change to `CONFIRMED` and a `BookingConfirmed` domain event SHALL be registered

### Scenario: Confirming an expired HELD booking is rejected

- **WHEN** `booking.confirm()` is called on a booking with `status = HELD` and `paymentDeadline <= Instant.now()`
- **THEN** a `BookingError.HoldExpired` SHALL be returned

### Scenario: Confirming a non-HELD booking returns InvalidStatusTransition error

- **WHEN** `booking.confirm()` is called on a booking with `status = CONFIRMED` or `CANCELLED`
- **THEN** `BookingError.InvalidStatusTransition` SHALL be returned with the current status

### Scenario: Expiring a HELD booking transitions to CANCELLED

- **WHEN** `booking.expire()` is called on a booking with `status = HELD`
- **THEN** the status SHALL change to `CANCELLED` and a `SeatHoldExpired` domain event SHALL be registered

### Scenario: Cancelling a booking that is already CANCELLED is idempotent

- **WHEN** `booking.cancel()` is called on a booking with `status = CANCELLED`
- **THEN** the method SHALL return success without modifying state or registering additional events

## Requirement: BookingId, UserId, RouteId are type-safe value objects

All ID types in the booking domain SHALL be distinct type-safe wrappers around `UUID`. Using a raw `UUID` where a typed ID is expected SHALL be a compile-time error. `SeatId` is owned by the train module and accessed via its named interface; it SHALL NOT be redefined in the booking module.

### Scenario: ID types prevent accidental mixing

- **WHEN** a method expects a `BookingId` parameter
- **THEN** passing a `UserId` or a raw `UUID` SHALL cause a compile-time type error

### Scenario: ID value objects have factory methods

- **WHEN** creating a new ID
- **THEN** the ID type SHALL provide a `generate()` static factory method that delegates to `IdGenerator.generateId()` from the shared infrastructure

## Requirement: BookingRepository interface defines domain-facing persistence contract

The `BookingRepository` interface in `booking/domain/repository/` SHALL define persistence operations using domain types only. No JPA, Spring, or persistence framework types SHALL appear in this interface's method signatures.

### Scenario: Repository methods use domain types only

- **WHEN** examining the `BookingRepository` interface
- **THEN** all method parameters and return types SHALL be domain types (`Booking`, `BookingId`, `Optional<Booking>`, `List<Booking>`) — never JPA entity types or framework-specific types

### Scenario: findActiveHoldByUserIdAndRouteId returns active hold if present

- **WHEN** `bookingRepository.findActiveHoldByUserIdAndRouteId(userId, routeId)` is called
- **THEN** it SHALL return `Optional<Booking>` containing the booking with `status = HELD` for that user-route pair, or `Optional.empty()` if none exists

### Scenario: findExpiredHolds returns all HELD bookings past their deadline

- **WHEN** `bookingRepository.findExpiredHolds(Instant now, int limit)` is called
- **THEN** it SHALL return up to `limit` bookings with `status = HELD` and `paymentDeadline < now`, ordered by `paymentDeadline` ascending

## Requirement: CreateSeatHoldUseCase orchestrates booking creation within a transaction

The `CreateSeatHoldUseCase` class in `booking/application/usecase/` SHALL be annotated with `@Service` and its `execute()` method SHALL be annotated with `@Transactional`. The use case SHALL validate input, check for existing active holds, calculate price, lock and hold seats, create the domain object, persist it, and return a result DTO.

### Scenario: Successful hold creation returns HoldDto

- **WHEN** `CreateSeatHoldUseCase.execute(CreateSeatHoldCommand)` is called with valid `userId`, `routeId`, `seatIds`, and `idempotencyKey`
- **THEN** a new `Booking` domain object SHALL be created with `status = HELD`, persisted with `booking_seats` rows, and a `HoldDto` SHALL be returned with booking ID, `status = HELD`, per-seat prices, `totalPrice`, and `expiresAt`

### Scenario: Domain events are published after transaction commits

- **WHEN** the `CreateSeatHoldUseCase` transaction commits successfully
- **THEN** Spring Modulith SHALL publish the `SeatHoldCreated` event to registered listeners

### Scenario: Use case is idempotent with idempotency key

- **WHEN** `CreateSeatHoldUseCase.execute()` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing `HoldDto` instead of creating a duplicate

## Requirement: BookingEntity maps to the bookings database table

The `BookingEntity` JPA class SHALL map to the `bookings` table. It SHALL NOT contain a `seatId` field. It SHALL have a `paymentDeadline` field mapped to `payment_deadline`. It SHALL use UUID primary key matching the `uuidv7()` default in the database.

### Scenario: BookingEntity fields match updated database schema

- **WHEN** the JPA schema validation runs at startup
- **THEN** `BookingEntity` SHALL validate successfully against the `bookings` table — all column names, types, and constraints SHALL match; there SHALL be no `seat_id` column

### Scenario: BookingEntity has protected default constructor

- **WHEN** Hibernate needs to instantiate a `BookingEntity` via reflection
- **THEN** the entity SHALL have a `protected BookingEntity() {}` no-args constructor available

## Requirement: BookingController exposes REST endpoints for hold and booking operations

The `BookingController` in `booking/infrastructure/web/` SHALL expose: `POST /api/bookings/hold` (create hold), `POST /api/bookings/{id}/confirm` (confirm), `DELETE /api/bookings/{id}` (cancel), and `GET /api/bookings/{id}` (retrieve). It SHALL NOT contain business logic.

### Scenario: POST /api/bookings/hold creates a new seat hold

- **WHEN** a `POST /api/bookings/hold` request is received with a valid JSON body containing `userId`, `routeId`, `seatIds` (non-empty list), and `idempotencyKey`
- **THEN** the controller SHALL map the request to a `CreateSeatHoldCommand`, call `CreateSeatHoldUseCase`, and return `201 Created` with the `HoldDto` including `expiresAt` and per-seat prices

### Scenario: POST /api/bookings/{id}/confirm confirms a held booking

- **WHEN** a `POST /api/bookings/{id}/confirm` request is received with `paymentReference`
- **THEN** the controller SHALL call `ConfirmSeatHoldUseCase` and return `200 OK` with the confirmed booking details

### Scenario: DELETE /api/bookings/{id} cancels a booking

- **WHEN** a `DELETE /api/bookings/{id}` request is received
- **THEN** the controller SHALL call `CancelBookingUseCase` and return `200 OK`

### Scenario: GET /api/bookings/{id} retrieves an existing booking

- **WHEN** a `GET /api/bookings/{id}` request is received with a valid booking UUID
- **THEN** the controller SHALL return `200 OK` with booking details including `seats[]` array and `expiresAt` (nullable for non-HELD bookings)

### Scenario: GET /api/bookings/{id} returns 404 for unknown booking

- **WHEN** a `GET /api/bookings/{id}` request is received with a UUID that does not exist
- **THEN** the controller SHALL return `404 Not Found`

# REMOVED Requirements

## Requirement: CreateBookingUseCase orchestrates booking creation within a transaction

**Reason**: Replaced by `CreateSeatHoldUseCase` (Phase 1) + `ConfirmSeatHoldUseCase` (Phase 2). The single-step create-and-confirm pattern is incompatible with the two-phase hold-then-pay flow.
**Migration**: Call `POST /api/bookings/hold` followed by `POST /api/bookings/{id}/confirm` instead of the former `POST /api/bookings`.

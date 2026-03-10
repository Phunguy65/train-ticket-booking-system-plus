# ADDED Requirements

## Requirement: Booking aggregate enforces HELD-first lifecycle with multi-seat support

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` and enforce all booking invariants. Initial status SHALL be `HELD`. The aggregate SHALL store `userId`, `routeId`, `passengerName`, `passengerEmail`, `passengerPhone`, `totalPrice` (as `Money`), `currency`, `status`, `idempotencyKey`, `paymentDeadline`, and `createdAt`. Direct field mutation from outside the aggregate is PROHIBITED.

`BookingStatus` SHALL be an enum with values: `HELD`, `CONFIRMED`, `CANCELLED`.

`Booking.create()` SHALL set `status = HELD`, compute `paymentDeadline = now + 15 minutes`, and register a `BookingCreated` domain event.

`Booking.reconstitute()` SHALL NOT register any domain events.

### Scenario: New booking starts in HELD status with a 15-minute payment deadline

- **WHEN** `Booking.create(bookingId, userId, routeId, passengerInfo, totalPrice, idempotencyKey, clock)` is called
- **THEN** the resulting booking SHALL have `status = HELD`, `paymentDeadline = now + 15 minutes`, and a `BookingCreated` domain event SHALL be registered on the aggregate

### Scenario: Confirming a HELD booking transitions to CONFIRMED

- **WHEN** `booking.confirm()` is called on a booking with `status = HELD`
- **THEN** `status` SHALL change to `CONFIRMED` and a `BookingConfirmed` domain event SHALL be registered

### Scenario: Confirming a non-HELD booking returns failure

- **WHEN** `booking.confirm()` is called on a booking with `status = CONFIRMED` or `CANCELLED`
- **THEN** `Result.failure(BookingError.InvalidStatusTransition)` SHALL be returned and no event SHALL be registered

### Scenario: Cancelling a HELD booking returns success with no-refund flag

- **WHEN** `booking.cancel()` is called on a booking with `status = HELD`
- **THEN** `status` SHALL change to `CANCELLED`, a `BookingCancelled` event with `requiresRefund = false` SHALL be registered, and `Result.success()` SHALL be returned

### Scenario: Cancelling a CONFIRMED booking returns success with refund flag

- **WHEN** `booking.cancel()` is called on a booking with `status = CONFIRMED`
- **THEN** `status` SHALL change to `CANCELLED`, a `BookingCancelled` event with `requiresRefund = true` SHALL be registered, and `Result.success()` SHALL be returned

### Scenario: Cancelling an already-CANCELLED booking returns failure

- **WHEN** `booking.cancel()` is called on a booking with `status = CANCELLED`
- **THEN** `Result.failure(BookingError.InvalidStatusTransition)` SHALL be returned

## Requirement: BookingId, UserId, RouteId, SeatId are type-safe value objects in the booking domain

All ID types in the booking domain SHALL be distinct type-safe wrappers around `UUID`. `BookingId`, `UserId`, and `RouteId` SHALL reside in `booking/domain/model/`. `SeatId` is imported from `train::model`.

### Scenario: ID types prevent accidental mixing at compile time

- **WHEN** a method expects a `BookingId` parameter
- **THEN** passing a `UserId` or a raw `UUID` SHALL cause a compile-time type error

### Scenario: BookingId provides a static factory method

- **WHEN** `BookingId.of(uuid)` is called with a non-null UUID
- **THEN** a `BookingId` wrapping that UUID SHALL be returned

### Scenario: BookingId rejects null values

- **WHEN** `BookingId.of(null)` is called
- **THEN** a `NullPointerException` or `IllegalArgumentException` SHALL be thrown

## Requirement: CreateBookingUseCase atomically holds multiple seats and creates a booking

`CreateBookingUseCase` SHALL be annotated `@Service` with `@Transactional` on `execute()`. It SHALL:
1. Check for an existing booking with the same `idempotencyKey` — return existing `BookingDto` if found
2. Check for an existing `HELD` booking for the same `(userId, routeId)` — return `BookingError.ActiveHoldExists` if found
3. Call `RouteSeatAvailabilityPort.holdSeats(routeId, seatIds)` — return `BookingError.SeatNotAvailable` on failure
4. Create `Booking` aggregate via `Booking.create()`
5. Persist via `BookingRepository.save()`
6. Publish domain events

### Scenario: Successful multi-seat booking returns BookingDto with HELD status

- **WHEN** `createBookingUseCase.execute(CreateBookingCommand)` is called with valid `userId`, `routeId`, `seatIds` (1–N seats), and `idempotencyKey`
- **THEN** all requested seats SHALL transition to `HELD` in `route_seat_availability`, a `Booking` with `status = HELD` and `paymentDeadline = now + 15 min` SHALL be persisted, and a `BookingDto` SHALL be returned

### Scenario: Duplicate idempotency key returns existing booking without side effects

- **WHEN** `createBookingUseCase.execute()` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing `BookingDto` without creating a new booking or modifying any seat status

### Scenario: Seat already HELD or BOOKED returns SeatNotAvailable error

- **WHEN** `createBookingUseCase.execute()` is called and one or more requested seats are not `AVAILABLE`
- **THEN** `Result.failure(BookingError.SeatNotAvailable)` SHALL be returned, no booking SHALL be persisted, and no seat status SHALL change

### Scenario: Concurrent booking of the same seat — exactly one succeeds

- **WHEN** two concurrent requests attempt to hold the same seat simultaneously
- **THEN** exactly one SHALL succeed with `200/201`; the other SHALL receive `409 Conflict` due to `OptimisticLockException` on `route_seat_availability.version`

### Scenario: Active hold already exists for user on same route returns error

- **WHEN** `createBookingUseCase.execute()` is called for a `(userId, routeId)` pair that already has a `HELD` booking
- **THEN** `Result.failure(BookingError.ActiveHoldExists)` SHALL be returned

## Requirement: BookingRepository interface defines domain-facing persistence contract

`BookingRepository` in `booking/domain/repository/` SHALL define persistence operations using domain types only. No JPA, Spring, or persistence framework types SHALL appear in method signatures.

### Scenario: Repository methods use domain types only

- **WHEN** examining the `BookingRepository` interface
- **THEN** all method parameters and return types SHALL be domain types (`Booking`, `BookingId`, `Optional<Booking>`) — never JPA entity types

### Scenario: findByIdempotencyKey returns existing booking

- **WHEN** `bookingRepository.findByIdempotencyKey(key)` is called with a key that matches an existing booking
- **THEN** `Optional.of(booking)` SHALL be returned

### Scenario: findActiveHoldByUserAndRoute returns HELD booking if exists

- **WHEN** `bookingRepository.findActiveHoldByUserAndRoute(userId, routeId)` is called
- **THEN** `Optional.of(booking)` SHALL be returned if a `HELD` booking exists for that pair; `Optional.empty()` otherwise

## Requirement: BookingEntity maps to the bookings table

`BookingEntity` SHALL map to the `bookings` table. It SHALL use UUID primary key. It SHALL NOT use `@GeneratedValue` — the ID is assigned by the domain layer using `uuidv7()` semantics (UUID passed in from the use case).

### Scenario: BookingEntity fields match database schema

- **WHEN** JPA schema validation runs at startup
- **THEN** `BookingEntity` SHALL validate successfully against the `bookings` table — all column names, types, and constraints SHALL match

### Scenario: BookingEntity has protected default constructor

- **WHEN** Hibernate needs to instantiate a `BookingEntity` via reflection
- **THEN** the entity SHALL have a `protected BookingEntity() {}` no-args constructor

## Requirement: BookingController exposes POST /api/v1/bookings endpoint

`BookingController` SHALL expose `POST /api/v1/bookings` for seat hold creation. It SHALL NOT contain business logic. Authentication is required (`@PreAuthorize("isAuthenticated()")`).

### Scenario: POST /api/v1/bookings creates a booking and returns 201

- **WHEN** an authenticated user sends `POST /api/v1/bookings` with valid `routeId`, `seatIds`, `passengerName`, `passengerEmail`, `idempotencyKey`
- **THEN** the controller SHALL return `201 Created` with `Location` header and a JSend success body containing `BookingHttpResponse`

### Scenario: POST /api/v1/bookings with unavailable seat returns 409

- **WHEN** an authenticated user sends `POST /api/v1/bookings` and one or more seats are not available
- **THEN** the controller SHALL return `409 Conflict` with a JSend fail body

### Scenario: POST /api/v1/bookings with invalid body returns 400

- **WHEN** a request is sent with missing required fields (e.g., no `routeId`)
- **THEN** the controller SHALL return `400 Bad Request` with Bean Validation error details

## Requirement: RouteSeatAvailabilityPort gains findSeatIdsByBookingId method

`RouteSeatAvailabilityPort` in `train/application/port/` SHALL add `List<SeatId> findSeatIdsByBookingId(BookingId bookingId)` to support cancellation flows in the booking module.

### Scenario: findSeatIdsByBookingId returns all seat IDs linked to a booking

- **WHEN** `routeSeatAvailabilityPort.findSeatIdsByBookingId(bookingId)` is called for a booking that holds 3 seats
- **THEN** a list of exactly 3 `SeatId` values SHALL be returned

### Scenario: findSeatIdsByBookingId returns empty list for unknown booking

- **WHEN** `routeSeatAvailabilityPort.findSeatIdsByBookingId(unknownBookingId)` is called
- **THEN** an empty list SHALL be returned

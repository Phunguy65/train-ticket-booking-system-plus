# MODIFIED Requirements

## Requirement: Booking domain model enforces booking lifecycle invariants

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` from the shared kernel and enforce all booking business rules. Direct field mutation from outside the aggregate is PROHIBITED — all state changes SHALL go through domain methods.

Booking lifecycle states: `HELD` → `CONFIRMED` → `CANCELLED` (also `HELD` → `CANCELLED`).

Domain methods SHALL return `Result<T, BookingError>` — they SHALL NOT throw exceptions.

### Scenario: New booking starts in HELD status

- **WHEN** `Booking.create(bookingId, userId, routeId, passengerInfo, totalPrice, idempotencyKey, clock)` factory method is called
- **THEN** the resulting booking SHALL have `status = HELD`, `paymentDeadline = now + 15 minutes`, and a `BookingCreated` domain event SHALL be registered on the aggregate

### Scenario: Confirming a HELD booking transitions to CONFIRMED

- **WHEN** `booking.confirm()` is called on a booking with `status = HELD`
- **THEN** `status` SHALL change to `CONFIRMED`, a `BookingConfirmed` domain event SHALL be registered, and `Result.success()` SHALL be returned

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

## Requirement: BookingId, UserId, RouteId are type-safe value objects

All ID types in the booking domain SHALL be distinct type-safe wrappers around `UUID`. Using a raw `UUID` where a typed ID is expected SHALL be a compile-time error. `SeatId` is imported from `train::model` and SHALL NOT be redefined in the booking module.

### Scenario: ID types prevent accidental mixing

- **WHEN** a method expects a `BookingId` parameter
- **THEN** passing a `UserId` or a raw `UUID` SHALL cause a compile-time type error

### Scenario: ID value objects have static factory methods

- **WHEN** creating a new ID via `BookingId.of(uuid)`
- **THEN** a `BookingId` wrapping that UUID SHALL be returned; passing `null` SHALL throw `NullPointerException` or `IllegalArgumentException`

## Requirement: BookingRepository interface defines domain-facing persistence contract

The `BookingRepository` interface in `booking/domain/repository/` SHALL define persistence operations using domain types only. No JPA, Spring, or persistence framework types SHALL appear in this interface's method signatures.

### Scenario: Repository methods use domain types only

- **WHEN** examining the `BookingRepository` interface
- **THEN** all method parameters and return types SHALL be domain types (`Booking`, `BookingId`, `Optional<Booking>`) — never JPA entity types or framework-specific types

### Scenario: Save method returns the saved domain model

- **WHEN** `bookingRepository.save(booking)` is called
- **THEN** it SHALL return a `Booking` domain model reflecting the persisted state

## Requirement: CreateBookingUseCase orchestrates multi-seat booking creation within a transaction

The `CreateBookingUseCase` class in `booking/application/usecase/` SHALL be annotated with `@Service` and its `execute()` method SHALL be annotated with `@Transactional`. The use case SHALL check idempotency, hold seats via `RouteSeatAvailabilityPort`, create the domain object, persist it, and return a result DTO.

### Scenario: Successful booking creation returns BookingDto with HELD status

- **WHEN** `createBookingUseCase.execute(CreateBookingCommand)` is called with valid `userId`, `routeId`, `seatIds`, and `idempotencyKey`
- **THEN** a new `Booking` with `status = HELD` SHALL be persisted and a `BookingDto` SHALL be returned

### Scenario: Domain events are published after transaction commits

- **WHEN** the `CreateBookingUseCase` transaction commits successfully
- **THEN** Spring Modulith SHALL publish the `BookingCreated` event collected by the aggregate

### Scenario: Use case is idempotent with idempotency key

- **WHEN** `CreateBookingUseCase.execute()` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing `BookingDto` without creating a duplicate

## Requirement: BookingEntity maps to the existing bookings database table

The `BookingEntity` JPA class SHALL map to the `bookings` table as defined in the Flyway migration schema. It SHALL use UUID primary key matching the `uuidv7()` default in the database.

### Scenario: BookingEntity fields match database schema

- **WHEN** the JPA schema validation runs at startup
- **THEN** `BookingEntity` SHALL validate successfully against the `bookings` table — all column names, types, and constraints SHALL match

### Scenario: BookingEntity has protected default constructor

- **WHEN** Hibernate needs to instantiate a `BookingEntity` via reflection
- **THEN** the entity SHALL have a `protected BookingEntity() {}` no-args constructor available

## Requirement: BookingController exposes REST endpoints for booking operations

The `BookingController` in `booking/infrastructure/web/` SHALL expose: `POST /api/v1/bookings` (hold seats) and `POST /api/v1/bookings/{id}/cancel` (cancel). It SHALL NOT contain business logic.

### Scenario: POST /api/v1/bookings creates a new booking

- **WHEN** an authenticated user sends `POST /api/v1/bookings` with valid JSON body
- **THEN** the controller SHALL return `201 Created` with booking details in JSend format

### Scenario: POST /api/v1/bookings/{id}/cancel cancels a booking

- **WHEN** an authenticated user sends `POST /api/v1/bookings/{id}/cancel` for their own booking
- **THEN** the controller SHALL return `200 OK` with updated booking details

## Requirement: BookingEntityMapper converts between JPA entity and domain model

The `BookingEntityMapper` class in `booking/infrastructure/persistence/` SHALL provide bidirectional mapping between `BookingEntity` and `Booking` domain model.

### Scenario: toDomain() uses reconstitute to avoid spurious domain events

- **WHEN** `bookingEntityMapper.toDomain(bookingEntity)` is called to load an existing booking from DB
- **THEN** it SHALL call `Booking.reconstitute(...)` and the resulting domain object SHALL have an empty domain events list

### Scenario: toEntity() produces a complete and valid JPA entity

- **WHEN** `bookingEntityMapper.toEntity(booking)` is called
- **THEN** the resulting `BookingEntity` SHALL have all fields populated from the domain model with correct type conversions

# REMOVED Requirements

## Requirement: Confirming a non-PENDING booking throws DomainException

**Reason**: The booking module uses the `Result` monad pattern — domain methods return `Result.failure()` instead of throwing exceptions. `DomainException` is not used in this codebase. The `PENDING` status has been replaced by `HELD`.

**Migration**: Use `booking.confirm()` which returns `Result.failure(BookingError.InvalidStatusTransition)` on invalid transitions. Map this to `409 Conflict` in the controller.

## Requirement: Cancelling a booking that is not CONFIRMED throws DomainException

**Reason**: Same as above — `DomainException` replaced by `Result` monad. Status lifecycle updated to `HELD/CONFIRMED/CANCELLED`.

**Migration**: Use `booking.cancel()` which returns `Result.failure(BookingError.InvalidStatusTransition)` when called on a `CANCELLED` booking.

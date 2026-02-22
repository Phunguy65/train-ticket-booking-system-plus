# ADDED Requirements

## Requirement: Booking domain model enforces booking lifecycle invariants

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` from the shared kernel and enforce all booking business rules. Direct field mutation from outside the aggregate is PROHIBITED — all state changes SHALL go through domain methods.

Booking lifecycle states: `PENDING` → `CONFIRMED` → `CANCELLED`.

#### Scenario: New booking starts in PENDING status

- **WHEN** `Booking.create(userId, routeId, seatId, price)` factory method is called
- **THEN** the resulting booking SHALL have `status = PENDING` and a `BookingCreated` domain event SHALL be registered on the aggregate

#### Scenario: Confirming a PENDING booking transitions to CONFIRMED

- **WHEN** `booking.confirm()` is called on a booking with status `PENDING`
- **THEN** the status SHALL change to `CONFIRMED` and a `BookingConfirmed` domain event SHALL be registered

#### Scenario: Confirming a non-PENDING booking throws DomainException

- **WHEN** `booking.confirm()` is called on a booking with status `CONFIRMED` or `CANCELLED`
- **THEN** a `DomainException` SHALL be thrown with a descriptive message

#### Scenario: Cancelling a booking that is not CONFIRMED throws DomainException

- **WHEN** `booking.cancel()` is called on a booking with status `CANCELLED`
- **THEN** a `DomainException` SHALL be thrown

## Requirement: BookingId, UserId, RouteId, SeatId are type-safe value objects

All ID types in the booking domain SHALL be distinct Kotlin `@JvmInline value class` wrappers around `UUID`. Using a raw `UUID` where a typed ID is expected SHALL be a compile-time error.

#### Scenario: ID types prevent accidental mixing

- **WHEN** a method expects a `BookingId` parameter
- **THEN** passing a `UserId` or a raw `UUID` SHALL cause a compile-time type error

#### Scenario: ID value objects have factory methods

- **WHEN** creating a new ID
- **THEN** the ID value class SHALL provide a `generate()` static factory method that delegates to `IdGenerator.generateId()` from the shared infrastructure

## Requirement: BookingRepository interface defines domain-facing persistence contract

The `BookingRepository` interface in `booking/domain/repository/` SHALL define persistence operations using domain types only. No JPA, Spring, or persistence framework types SHALL appear in this interface's method signatures.

#### Scenario: Repository methods use domain types only

- **WHEN** examining the `BookingRepository` interface
- **THEN** all method parameters and return types SHALL be domain types (`Booking`, `BookingId`, `Optional<Booking>`) — never JPA entity types or framework-specific types

#### Scenario: Save method returns the saved domain model

- **WHEN** `bookingRepository.save(booking)` is called
- **THEN** it SHALL return a `Booking` domain model reflecting the persisted state (including any generated fields)

## Requirement: CreateBookingUseCase orchestrates booking creation within a transaction

The `CreateBookingUseCase` class in `booking/application/usecase/` SHALL be annotated with `@Service` and its `execute()` method SHALL be annotated with `@Transactional`. The use case SHALL validate input, create the domain object, persist it, and return a result DTO.

#### Scenario: Successful booking creation returns BookingDto

- **WHEN** `createBookingUseCase.execute(CreateBookingCommand)` is called with valid userId, routeId, seatId
- **THEN** a new `Booking` domain object SHALL be created, persisted, and a `BookingDto` SHALL be returned with the booking ID and PENDING status

#### Scenario: Domain events are published after transaction commits

- **WHEN** the `CreateBookingUseCase` transaction commits successfully
- **THEN** Spring Modulith SHALL publish the `BookingCreated` event collected by the aggregate to registered listeners (e.g., seat inventory reservation)

#### Scenario: Use case is idempotent with idempotency key

- **WHEN** `CreateBookingUseCase.execute()` is called twice with the same `idempotencyKey`
- **THEN** the second call SHALL return the existing booking DTO instead of creating a duplicate

## Requirement: BookingEntity maps to the existing bookings database table

The `BookingEntity` JPA class SHALL map to the `bookings` table as defined in the Flyway migration schema. It SHALL use UUID primary key matching the `uuidv7()` default in the database.

#### Scenario: BookingEntity fields match database schema

- **WHEN** the JPA schema validation runs at startup
- **THEN** `BookingEntity` SHALL validate successfully against the `bookings` table — all column names, types, and constraints SHALL match

#### Scenario: BookingEntity has protected default constructor

- **WHEN** Hibernate needs to instantiate a `BookingEntity` via reflection
- **THEN** the entity SHALL have a `protected BookingEntity() {}` no-args constructor available

## Requirement: BookingController exposes REST endpoints for booking operations

The `BookingController` in `booking/infrastructure/web/` SHALL expose HTTP REST endpoints: `POST /api/bookings` (create) and `GET /api/bookings/{id}` (retrieve). It SHALL NOT contain business logic — all logic is delegated to use cases.

#### Scenario: POST /api/bookings creates a new booking

- **WHEN** a `POST /api/bookings` request is received with a valid JSON body containing userId, routeId, seatId
- **THEN** the controller SHALL map the request to a `CreateBookingCommand`, call `CreateBookingUseCase`, and return `201 Created` with the booking details

#### Scenario: GET /api/bookings/{id} retrieves an existing booking

- **WHEN** a `GET /api/bookings/{id}` request is received with a valid booking UUID
- **THEN** the controller SHALL call `GetBookingUseCase` and return `200 OK` with the booking details

#### Scenario: GET /api/bookings/{id} returns 404 for unknown booking

- **WHEN** a `GET /api/bookings/{id}` request is received with a UUID that does not exist in the database
- **THEN** the controller SHALL return `404 Not Found`

## Requirement: BookingEntityMapper converts between JPA entity and domain model

The `BookingEntityMapper` class in `booking/infrastructure/persistence/` SHALL provide bidirectional mapping between `BookingEntity` and `Booking` domain model. It SHALL use `Booking.create()` for new objects and `Booking.reconstitute()` when loading from persistence.

#### Scenario: toDomain() uses reconstitute to avoid spurious domain events

- **WHEN** `bookingEntityMapper.toDomain(bookingEntity)` is called to load an existing booking from DB
- **THEN** it SHALL call `Booking.reconstitute(...)` and the resulting domain object SHALL have an empty domain events list

#### Scenario: toEntity() produces a complete and valid JPA entity

- **WHEN** `bookingEntityMapper.toEntity(booking)` is called
- **THEN** the resulting `BookingEntity` SHALL have all fields populated from the domain model with correct type conversions (e.g., `BookingId.value` → `UUID`, `BookingStatus` → `String`)

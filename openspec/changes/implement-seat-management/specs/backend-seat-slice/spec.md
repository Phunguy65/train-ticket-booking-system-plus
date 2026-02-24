## ADDED Requirements

### Requirement: Seat domain model enforces seat invariants

The `Seat` aggregate SHALL extend `AggregateRoot<SeatId>` from the shared kernel and reside in `train/domain/model/`. It SHALL hold `SeatId`, `TrainId`, `seatNumber`, and `seatClass` as immutable fields set at creation. Direct field mutation from outside the aggregate is PROHIBITED.

`SeatClass` SHALL be an enum with values: `ECONOMY`, `BUSINESS`, `FIRST_CLASS`.

#### Scenario: New seat is created with valid fields

- **WHEN** `Seat.create(trainId, seatNumber, seatClass)` factory method is called
- **THEN** the resulting `Seat` SHALL have all fields populated and a generated `SeatId`

#### Scenario: Reconstituted seat does not emit domain events

- **WHEN** `Seat.reconstitute(id, trainId, seatNumber, seatClass, createdAt)` is called to load from persistence
- **THEN** the resulting `Seat` SHALL have an empty domain events list

### Requirement: SeatId is a type-safe value object

`SeatId` SHALL be a distinct type-safe wrapper around `UUID` in `train/domain/model/`. It SHALL already exist as a value object in the `booking` module — the new `SeatId` in the `train` module's named interface SHALL replace that usage.

#### Scenario: SeatId prevents accidental mixing with other ID types

- **WHEN** a method expects a `SeatId` parameter
- **THEN** passing a `TrainId` or raw `UUID` SHALL cause a compile-time type error

### Requirement: SeatRepository interface defines domain-facing persistence contract

The `SeatRepository` interface in `train/domain/repository/` SHALL define persistence operations using domain types only. No JPA or Spring framework types SHALL appear in this interface.

#### Scenario: Repository saves a seat and returns the domain model

- **WHEN** `seatRepository.save(seat)` is called
- **THEN** it SHALL return a `Seat` domain model reflecting the persisted state

#### Scenario: Repository finds all seats by train ID

- **WHEN** `seatRepository.findByTrainId(trainId)` is called
- **THEN** it SHALL return a list of all `Seat` domain models belonging to that train

#### Scenario: Repository prevents duplicate seat number per train

- **WHEN** a seat with the same `(trainId, seatNumber)` combination already exists
- **THEN** saving a second seat with identical combination SHALL result in a `SeatError.SeatNumberAlreadyExists` failure

### Requirement: CreateSeatUseCase orchestrates seat creation within a transaction

The `CreateSeatUseCase` class in `train/application/usecase/` SHALL be annotated with `@Service` and its `execute()` method SHALL be annotated with `@Transactional`. It SHALL validate that the train exists, check for duplicate seat numbers, create the domain object, and return a result DTO.

#### Scenario: Successful seat creation returns SeatDto

- **WHEN** `createSeatUseCase.execute(CreateSeatCommand)` is called with a valid `trainId`, `seatNumber`, and `seatClass`
- **THEN** a new `Seat` domain object SHALL be created, persisted, and a `SeatDto` SHALL be returned with the seat ID and all fields

#### Scenario: Creating a seat for a non-existent train returns failure

- **WHEN** `createSeatUseCase.execute(CreateSeatCommand)` is called with a `trainId` that does not exist
- **THEN** the use case SHALL return `Result.failure(SeatError.TrainNotFound)`

#### Scenario: Creating a duplicate seat number returns failure

- **WHEN** `createSeatUseCase.execute(CreateSeatCommand)` is called with a `seatNumber` already assigned to another seat on the same train
- **THEN** the use case SHALL return `Result.failure(SeatError.SeatNumberAlreadyExists)`

### Requirement: GetSeatsByTrainUseCase retrieves all seats for a train

The `GetSeatsByTrainUseCase` class in `train/application/usecase/` SHALL return all seats belonging to a given train as a list of `SeatDto`.

#### Scenario: Returns all seats for an existing train

- **WHEN** `getSeatsByTrainUseCase.execute(trainId)` is called with a valid `trainId`
- **THEN** a list of `SeatDto` objects for all seats on that train SHALL be returned

#### Scenario: Returns empty list for a train with no seats

- **WHEN** `getSeatsByTrainUseCase.execute(trainId)` is called with a `trainId` that has no seats
- **THEN** an empty list SHALL be returned (not an error)

### Requirement: SeatEntity maps to the existing seats database table

The `SeatEntity` JPA class in `train/infrastructure/persistence/` SHALL map to the `seats` table. It SHALL use UUID primary key with `uuidv7()` default. It SHALL NOT include a `status` column (removed in V4.0.0 migration).

#### Scenario: SeatEntity fields match database schema after V4.0.0 migration

- **WHEN** the JPA schema validation runs at startup
- **THEN** `SeatEntity` SHALL validate successfully against the `seats` table with columns: `id`, `train_id`, `seat_number`, `seat_class`, `created_at`

### Requirement: SeatController exposes REST endpoints for seat management

The `SeatController` in `train/infrastructure/web/` SHALL expose:
- `POST /api/v1/trains/{trainId}/seats` — create a seat (admin only, `@PreAuthorize("hasRole('ADMIN')")`)
- `GET /api/v1/trains/{trainId}/seats` — list all seats for a train

It SHALL NOT contain business logic — all logic is delegated to use cases.

#### Scenario: POST /api/v1/trains/{trainId}/seats creates a new seat

- **WHEN** a `POST /api/v1/trains/{trainId}/seats` request is received with valid JSON body containing `seatNumber` and `seatClass`
- **THEN** the controller SHALL return `201 Created` with the `SeatHttpResponse` containing the created seat's details

#### Scenario: GET /api/v1/trains/{trainId}/seats lists all seats

- **WHEN** a `GET /api/v1/trains/{trainId}/seats` request is received with a valid `trainId`
- **THEN** the controller SHALL return `200 OK` with an array of seat objects

#### Scenario: POST /api/v1/trains/{trainId}/seats returns 409 for duplicate seat number

- **WHEN** a `POST /api/v1/trains/{trainId}/seats` request is received with a `seatNumber` already assigned on that train
- **THEN** the controller SHALL return `409 Conflict`

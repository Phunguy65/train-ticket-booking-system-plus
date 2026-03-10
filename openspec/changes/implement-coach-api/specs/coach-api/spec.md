# ADDED Requirements

## Requirement: CoachError typed business errors

The system SHALL provide a `CoachError` sealed interface in `train/domain/errors/` with three typed error records: `CoachNotFound`, `CarNumberAlreadyExists(int carNumber)`, and `TrainNotFound`. Each record SHALL implement a `message()` method returning a human-readable string. No JPA or Spring framework types SHALL appear in this interface.

### Scenario: Coach not found error carries correct message

- **WHEN** `new CoachError.CoachNotFound()` is constructed
- **THEN** `message()` returns `"Coach not found"`

### Scenario: Car number already exists error carries car number

- **WHEN** `new CoachError.CarNumberAlreadyExists(3)` is constructed
- **THEN** `message()` returns a string containing `"3"` and indicating the car number is already in use on the train

### Scenario: Train not found error carries correct message

- **WHEN** `new CoachError.TrainNotFound()` is constructed
- **THEN** `message()` returns `"Train not found"`

## Requirement: Coach error codes in shared ErrorCode enum

The system SHALL add three constants to the shared `ErrorCode` enum in `shared/infrastructure/web/`: `COACH_NOT_FOUND`, `COACH_CAR_NUMBER_ALREADY_EXISTS`, and `COACH_TRAIN_NOT_FOUND`.

### Scenario: Coach error codes exist in ErrorCode enum

- **WHEN** `ErrorCode.COACH_NOT_FOUND` is referenced
- **THEN** it compiles and is distinct from all other enum constants

## Requirement: Create Coach use case

The system SHALL provide a `CreateCoachUseCase` in `train/application/usecase/` annotated with `@Service @Transactional`. It SHALL accept a `CreateCoachCommand(UUID trainId, int carNumber, int totalSeats)` and return `Result<CoachDto, CoachError>`. The use case SHALL:

1. Verify the parent train exists (non-deleted) via `trainRepository.findById(TrainId.of(command.trainId()))` — return `CoachError.TrainNotFound` if empty.
2. Verify uniqueness of `carNumber` within the train via `coachRepository.existsByTrainIdAndCarNumber(trainId, command.carNumber())` — return `CoachError.CarNumberAlreadyExists(command.carNumber())` if true.
3. Create the coach via `Coach.create(CoachId.of(UUID.randomUUID()), trainId, command.carNumber(), command.totalSeats())`.
4. Persist via `coachRepository.save(coach)`.
5. Return `Result.success(toDto(saved))`. No domain events are published on creation (consistent with the domain model design).

### Scenario: Successfully create a coach

- **WHEN** `CreateCoachUseCase.execute(command)` is called with a valid `trainId`, unique `carNumber`, and positive `totalSeats`
- **THEN** a `Coach` is persisted and the use case returns `Result.success` containing a `CoachDto` with matching `id`, `trainId`, `carNumber`, `totalSeats`, and non-null `createdAt`

### Scenario: Fail when parent train does not exist

- **WHEN** `CreateCoachUseCase.execute(command)` is called with a `trainId` that does not match any active train
- **THEN** the use case returns `Result.failure(CoachError.TrainNotFound)`

### Scenario: Fail when car number already exists in the train

- **WHEN** `CreateCoachUseCase.execute(command)` is called with a `carNumber` that is already in use by an active coach on the same train
- **THEN** the use case returns `Result.failure(CoachError.CarNumberAlreadyExists(carNumber))`

## Requirement: Get Coach by ID use case

The system SHALL provide a `GetCoachByIdUseCase` in `train/application/usecase/` annotated with `@Service` with `@Transactional(readOnly = true)`. It SHALL accept both a `CoachId` and a `TrainId`, fetch the coach via `coachRepository.findById(coachId)`, and return `Result<CoachDto, CoachError>`. If the coach is not found, or if the found coach's `trainId` does not equal the provided `trainId`, the use case SHALL return `Result.failure(new CoachError.CoachNotFound())`.

### Scenario: Successfully retrieve a coach by ID

- **WHEN** `GetCoachByIdUseCase.execute(trainId, coachId)` is called with a `coachId` that exists and belongs to `trainId`
- **THEN** the use case returns `Result.success` containing a `CoachDto` with the coach's data

### Scenario: Return not found when coach does not exist

- **WHEN** `GetCoachByIdUseCase.execute(trainId, coachId)` is called with a `coachId` that does not exist
- **THEN** the use case returns `Result.failure(CoachError.CoachNotFound)`

### Scenario: Return not found when coach belongs to a different train

- **WHEN** `GetCoachByIdUseCase.execute(trainId, coachId)` is called and the coach with `coachId` exists but its `trainId` does not match the provided `trainId`
- **THEN** the use case returns `Result.failure(CoachError.CoachNotFound)` (same as not found — no information leakage)

## Requirement: Get Coaches by Train use case

The system SHALL provide a `GetCoachesByTrainUseCase` in `train/application/usecase/` annotated with `@Service` with `@Transactional(readOnly = true)`. It SHALL accept a `TrainId` and return `List<CoachDto>` by calling `coachRepository.findByTrainId(trainId)`. The list SHALL be ordered by `carNumber` ascending (as guaranteed by the JPA query). If no coaches exist for the given train, an empty list SHALL be returned.

### Scenario: Return coaches ordered by carNumber

- **WHEN** `GetCoachesByTrainUseCase.execute(trainId)` is called for a train with coaches having carNumbers 3, 1, 2
- **THEN** the returned list contains all three coaches ordered as 1, 2, 3

### Scenario: Return empty list when train has no coaches

- **WHEN** `GetCoachesByTrainUseCase.execute(trainId)` is called for a train with no active coaches
- **THEN** an empty list is returned

## Requirement: Coach REST endpoints

The system SHALL provide a `CoachController` in `train/infrastructure/web/` as a package-private `@RestController`. Endpoints SHALL be declared with full paths on each method (following `SeatController` pattern, NOT class-level `@RequestMapping`). All responses SHALL use `JsendResponse<?>`. The controller SHALL expose three endpoints:

**POST `/{version}/trains/{trainId}/coaches`** (version `"1.0"`, requires `ADMIN` role):
- Accepts `@Valid @RequestBody CreateCoachHttpRequest` containing `@Positive int carNumber` and `@Positive int totalSeats`.
- On success: returns `201 Created` with `Location` header pointing to the new resource and `JsendResponse.success(CoachHttpResponse)`.
- On `CoachError.TrainNotFound`: returns `404 Not Found` with `JsendResponse.fail` using `ErrorCode.COACH_TRAIN_NOT_FOUND`.
- On `CoachError.CarNumberAlreadyExists`: returns `409 Conflict` with `JsendResponse.fail` using `ErrorCode.COACH_CAR_NUMBER_ALREADY_EXISTS`.

**GET `/{version}/trains/{trainId}/coaches`** (version `"1.0"`, public):
- Returns `200 OK` with `JsendResponse.success(List<CoachHttpResponse>)` ordered by `carNumber` ascending.

**GET `/{version}/trains/{trainId}/coaches/{id}`** (version `"1.0"`, public):
- On success: returns `200 OK` with `JsendResponse.success(CoachHttpResponse)`.
- On `CoachError.CoachNotFound`: returns `404 Not Found` with `JsendResponse.fail` using `ErrorCode.COACH_NOT_FOUND`.

### Scenario: Create coach returns 201 with Location header

- **WHEN** `POST /api/1.0/trains/{trainId}/coaches` is called by an ADMIN with valid `carNumber` and `totalSeats`
- **THEN** the response status is `201 Created`, the `Location` header ends with `/coaches/{newCoachId}`, and the body contains `{ "status": "success", "data": { "id": "...", "trainId": "...", "carNumber": ..., "totalSeats": ..., "createdAt": "..." } }`

### Scenario: Create coach returns 404 when train does not exist

- **WHEN** `POST /api/1.0/trains/{nonExistentTrainId}/coaches` is called by an ADMIN
- **THEN** the response status is `404 Not Found` and the body contains `{ "status": "fail", "data": { "code": "COACH_TRAIN_NOT_FOUND", ... } }`

### Scenario: Create coach returns 409 when carNumber is duplicate

- **WHEN** `POST /api/1.0/trains/{trainId}/coaches` is called by an ADMIN with a `carNumber` already used by an active coach on that train
- **THEN** the response status is `409 Conflict` and the body contains `{ "status": "fail", "data": { "code": "COACH_CAR_NUMBER_ALREADY_EXISTS", ... } }`

### Scenario: Create coach returns 400 when carNumber is zero or negative

- **WHEN** `POST /api/1.0/trains/{trainId}/coaches` is called with `carNumber: 0` or a negative value
- **THEN** the response status is `400 Bad Request` and the body contains `{ "status": "fail", "data": { "code": "VALIDATION_ERROR", ... } }`

### Scenario: Create coach returns 403 for non-ADMIN user

- **WHEN** `POST /api/1.0/trains/{trainId}/coaches` is called without ADMIN role
- **THEN** the response status is `403 Forbidden`

### Scenario: Get all coaches returns ordered list

- **WHEN** `GET /api/1.0/trains/{trainId}/coaches` is called
- **THEN** the response status is `200 OK` and the body contains `{ "status": "success", "data": [ ... ] }` with coaches ordered by `carNumber` ascending

### Scenario: Get coach by ID returns coach data

- **WHEN** `GET /api/1.0/trains/{trainId}/coaches/{id}` is called with a valid `id` belonging to `trainId`
- **THEN** the response status is `200 OK` and the body contains `{ "status": "success", "data": { "id": "...", ... } }`

### Scenario: Get coach by ID returns 404 when not found

- **WHEN** `GET /api/1.0/trains/{trainId}/coaches/{id}` is called with a non-existent or mismatched `id`
- **THEN** the response status is `404 Not Found` and the body contains `{ "status": "fail", "data": { "code": "COACH_NOT_FOUND", ... } }`

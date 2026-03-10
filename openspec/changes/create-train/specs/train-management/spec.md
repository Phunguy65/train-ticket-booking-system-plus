## ADDED Requirements

### Requirement: Create a new train

The system SHALL allow an ADMIN user to create a new train record by providing a unique train number, a display name, and total seat count. Upon successful creation the system SHALL return the persisted train data including its generated UUID and SHALL publish a `TrainCreated` domain event.

#### Scenario: Successful train creation

- **WHEN** an authenticated ADMIN sends `POST /api/v1.0/trains` with valid `trainNumber`, `name`, and `totalSeats`
- **THEN** the system persists the train, returns HTTP 201 Created with a `Location` header pointing to `/api/v1.0/trains/{id}`, and wraps the new train in a JSend `success` response body

#### Scenario: Duplicate train number rejected

- **WHEN** an authenticated ADMIN sends `POST /api/v1.0/trains` with a `trainNumber` that already exists in the system
- **THEN** the system returns HTTP 409 Conflict with a JSend `fail` response and error code `TRAIN_NUMBER_ALREADY_EXISTS`

#### Scenario: Invalid request body rejected

- **WHEN** an authenticated ADMIN sends `POST /api/v1.0/trains` with a missing or blank `trainNumber`, blank `name`, or non-positive `totalSeats`
- **THEN** the system returns HTTP 400 Bad Request with a JSend `fail` response containing per-field validation errors and error code `VALIDATION_ERROR`

#### Scenario: Non-ADMIN user forbidden

- **WHEN** an authenticated non-ADMIN user sends `POST /api/v1.0/trains`
- **THEN** the system returns HTTP 403 Forbidden

#### Scenario: Unauthenticated request rejected

- **WHEN** an unauthenticated client sends `POST /api/v1.0/trains`
- **THEN** the system returns HTTP 401 Unauthorized

### Requirement: Retrieve a paginated list of trains

The system SHALL allow any authenticated user to retrieve a paginated, sortable list of all trains. The response SHALL use the shared `PageResult` envelope.

#### Scenario: Default pagination returns first page

- **WHEN** an authenticated user sends `GET /api/v1.0/trains` with no query parameters
- **THEN** the system returns HTTP 200 OK with a JSend `success` response containing the first page of trains (default page size) sorted by `trainNumber` ascending

#### Scenario: Custom pagination and sort applied

- **WHEN** an authenticated user sends `GET /api/v1.0/trains?page=1&size=5&sort=name&direction=DESC`
- **THEN** the system returns HTTP 200 OK with the second page of 5 trains sorted by `name` descending

#### Scenario: Empty list when no trains exist

- **WHEN** an authenticated user sends `GET /api/v1.0/trains` and no trains have been created
- **THEN** the system returns HTTP 200 OK with a JSend `success` response containing an empty `items` array and `totalElements` of 0

#### Scenario: Unauthenticated request rejected

- **WHEN** an unauthenticated client sends `GET /api/v1.0/trains`
- **THEN** the system returns HTTP 401 Unauthorized

### Requirement: Retrieve a single train by ID

The system SHALL allow any authenticated user to fetch a single train by its UUID. The response SHALL include all train fields.

#### Scenario: Existing train returned

- **WHEN** an authenticated user sends `GET /api/v1.0/trains/{id}` with a UUID that corresponds to an existing train
- **THEN** the system returns HTTP 200 OK with a JSend `success` response containing `id`, `trainNumber`, `name`, `totalSeats`, and `createdAt`

#### Scenario: Non-existent train returns 404

- **WHEN** an authenticated user sends `GET /api/v1.0/trains/{id}` with a UUID that does not match any train
- **THEN** the system returns HTTP 404 Not Found with a JSend `fail` response and error code `TRAIN_NOT_FOUND`

#### Scenario: Unauthenticated request rejected

- **WHEN** an unauthenticated client sends `GET /api/v1.0/trains/{id}`
- **THEN** the system returns HTTP 401 Unauthorized

### Requirement: TrainCreated domain event published

The system SHALL publish a `TrainCreated` domain event after a train is successfully persisted. The event SHALL be published within the same transaction boundary as the save operation.

#### Scenario: Event published on successful create

- **WHEN** `CreateTrainUseCase` successfully creates and saves a train
- **THEN** a `TrainCreated` event containing the new `TrainId` and `trainNumber` is registered on the aggregate and published via `ApplicationEventPublisher`

#### Scenario: No event published on failure

- **WHEN** `CreateTrainUseCase` returns a failure (e.g., duplicate train number)
- **THEN** no `TrainCreated` event is published

### Requirement: TrainId exposed as named interface

The system SHALL expose the `TrainId` value object from the `train` module via a `@NamedInterface("model")` so that other modules can safely import it without coupling to internal train module implementation.

#### Scenario: Other modules can declare dependency on train model

- **WHEN** a module declares `allowedDependencies = {"train::model"}` in its `package-info.java`
- **THEN** it can import and use `TrainId` without ArchUnit or Spring Modulith boundary violations

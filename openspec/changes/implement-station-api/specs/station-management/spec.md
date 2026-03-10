## ADDED Requirements

### Requirement: Create station
An authenticated ADMIN user SHALL be able to create a new railway station by providing a unique code, a name, and a city. The system SHALL persist the station and return its generated UUID along with all fields and creation timestamp.

#### Scenario: Successful station creation
- **WHEN** an ADMIN sends `POST /api/v1/stations` with a valid `code`, `name`, and `city`
- **THEN** the system returns HTTP 201 Created with a `Location` header pointing to the new station and a JSend success body containing `id`, `code`, `name`, `city`, and `createdAt`

#### Scenario: Duplicate station code rejected
- **WHEN** an ADMIN sends `POST /api/v1/stations` with a `code` that already exists in the system
- **THEN** the system returns HTTP 409 Conflict with a JSend fail body containing error code `STATION_CODE_ALREADY_EXISTS`

#### Scenario: Missing required fields rejected
- **WHEN** an ADMIN sends `POST /api/v1/stations` with one or more required fields (`code`, `name`, `city`) missing or blank
- **THEN** the system returns HTTP 400 Bad Request with a JSend fail body listing the violated fields and error code `VALIDATION_ERROR`

#### Scenario: Non-admin user cannot create station
- **WHEN** an authenticated non-ADMIN user sends `POST /api/v1/stations`
- **THEN** the system returns HTTP 403 Forbidden

#### Scenario: Unauthenticated user cannot create station
- **WHEN** an unauthenticated request is sent to `POST /api/v1/stations`
- **THEN** the system returns HTTP 401 Unauthorized

---

### Requirement: Get station by ID
Any authenticated user SHALL be able to retrieve a station's full details by its UUID.

#### Scenario: Existing station retrieved successfully
- **WHEN** an authenticated user sends `GET /api/v1/stations/{id}` with a valid UUID that corresponds to an existing station
- **THEN** the system returns HTTP 200 OK with a JSend success body containing `id`, `code`, `name`, `city`, and `createdAt`

#### Scenario: Non-existent station returns 404
- **WHEN** an authenticated user sends `GET /api/v1/stations/{id}` with a UUID that does not match any station
- **THEN** the system returns HTTP 404 Not Found with a JSend fail body containing error code `STATION_NOT_FOUND`

#### Scenario: Unauthenticated user cannot retrieve station
- **WHEN** an unauthenticated request is sent to `GET /api/v1/stations/{id}`
- **THEN** the system returns HTTP 401 Unauthorized

---

### Requirement: List stations with pagination
Any authenticated user SHALL be able to retrieve a paginated, sorted list of all stations.

#### Scenario: Default paginated list returned
- **WHEN** an authenticated user sends `GET /api/v1/stations` with no query parameters
- **THEN** the system returns HTTP 200 OK with a JSend success body containing a slice of stations (default page 0, size 20, sorted by `createdAt` descending), with `hasNext` and `hasPrevious` flags

#### Scenario: Custom pagination parameters applied
- **WHEN** an authenticated user sends `GET /api/v1/stations?page=1&size=10&sort=name,asc`
- **THEN** the system returns HTTP 200 OK with the second page of 10 stations sorted alphabetically by name ascending

#### Scenario: Invalid page parameter rejected
- **WHEN** an authenticated user sends `GET /api/v1/stations?page=-1`
- **THEN** the system returns HTTP 400 Bad Request with a JSend fail body and error code `VALIDATION_ERROR`

#### Scenario: Size out of range rejected
- **WHEN** an authenticated user sends `GET /api/v1/stations?size=0` or `GET /api/v1/stations?size=101`
- **THEN** the system returns HTTP 400 Bad Request with a JSend fail body and error code `VALIDATION_ERROR`

#### Scenario: Invalid sort field rejected
- **WHEN** an authenticated user sends `GET /api/v1/stations?sort=unknown,asc`
- **THEN** the system returns HTTP 400 Bad Request with a JSend fail body and error code `VALIDATION_ERROR`

#### Scenario: Unauthenticated user cannot list stations
- **WHEN** an unauthenticated request is sent to `GET /api/v1/stations`
- **THEN** the system returns HTTP 401 Unauthorized

---

### Requirement: Station domain event on creation
The system SHALL publish a `StationCreated` domain event after a station is successfully persisted.

#### Scenario: Domain event published after creation
- **WHEN** a station is successfully created via `POST /api/v1/stations`
- **THEN** a `StationCreated` domain event is published containing the new station's `id`, `code`, `name`, and `city`

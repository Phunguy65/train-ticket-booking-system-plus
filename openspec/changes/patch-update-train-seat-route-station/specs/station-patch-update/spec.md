## ADDED Requirements

### Requirement: Admin can partially update a Station

The system SHALL expose a `PATCH /api/v1.0/stations/{id}` endpoint that allows an authenticated ADMIN user to update one or more of the following fields on an existing Station: `code`, `name`, `city`. Fields not present in the request body SHALL be left unchanged. The endpoint SHALL return the full updated Station representation on success.

#### Scenario: Successful partial update of station name only

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with body `{"name": "Hanoi Central"}`
- **THEN** the system SHALL update only `name`, leave `code` and `city` unchanged, and return `200 OK` with the updated Station in JSend success format

#### Scenario: Successful update of multiple fields

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with body `{"code": "HAN", "city": "Hanoi"}`
- **THEN** the system SHALL update both `code` and `city` and return `200 OK`

#### Scenario: Station not found

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with a non-existent `id`
- **THEN** the system SHALL return `404 Not Found` with JSend fail format and error code `STATION_NOT_FOUND`

#### Scenario: Station code conflict on update

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with a `code` already used by a different Station
- **THEN** the system SHALL return `409 Conflict` with JSend fail format and error code `STATION_CODE_ALREADY_EXISTS`

#### Scenario: Same code as current value

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with the same `code` the Station already has
- **THEN** the system SHALL NOT treat it as a conflict and SHALL successfully update the Station

#### Scenario: Non-admin user is rejected

- **WHEN** a non-ADMIN authenticated user sends `PATCH /api/v1.0/stations/{id}`
- **THEN** the system SHALL return `403 Forbidden`

#### Scenario: Blank code rejected

- **WHEN** an ADMIN sends `PATCH /api/v1.0/stations/{id}` with `code` set to a blank string
- **THEN** the system SHALL return `400 Bad Request` with JSend fail format and error code `VALIDATION_ERROR`

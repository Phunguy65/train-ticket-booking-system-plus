## ADDED Requirements

### Requirement: Admin can partially update a Train

The system SHALL expose a `PATCH /api/v1.0/trains/{id}` endpoint that allows an authenticated ADMIN user to update one or more of the following fields on an existing Train: `trainNumber`, `name`, `totalSeats`. Fields not present in the request body SHALL be left unchanged. The endpoint SHALL return the full updated Train representation on success.

#### Scenario: Successful partial update of train name only

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with body `{"name": "New Name"}`
- **THEN** the system SHALL update only the `name` field and return `200 OK` with the updated Train in JSend success format

#### Scenario: Successful update of multiple fields

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with body `{"name": "Express", "totalSeats": 200}`
- **THEN** the system SHALL update both `name` and `totalSeats`, leave `trainNumber` unchanged, and return `200 OK`

#### Scenario: Train not found

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with a non-existent `id`
- **THEN** the system SHALL return `404 Not Found` with JSend fail format and error code `TRAIN_NOT_FOUND`

#### Scenario: trainNumber conflict on update

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with a `trainNumber` that is already used by a different Train
- **THEN** the system SHALL return `409 Conflict` with JSend fail format and error code `TRAIN_NUMBER_ALREADY_EXISTS`

#### Scenario: Same trainNumber as current value

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with the same `trainNumber` the Train already has
- **THEN** the system SHALL NOT treat it as a conflict and SHALL successfully update the Train

#### Scenario: Non-admin user is rejected

- **WHEN** a non-ADMIN authenticated user sends `PATCH /api/v1.0/trains/{id}`
- **THEN** the system SHALL return `403 Forbidden`

#### Scenario: Validation failure on invalid field

- **WHEN** an ADMIN sends `PATCH /api/v1.0/trains/{id}` with `trainNumber` set to a blank string
- **THEN** the system SHALL return `400 Bad Request` with JSend fail format and error code `VALIDATION_ERROR`

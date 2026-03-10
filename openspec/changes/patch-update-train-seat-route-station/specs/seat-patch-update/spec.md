## ADDED Requirements

### Requirement: Admin can partially update a Seat

The system SHALL expose a `PATCH /api/v1.0/seats/{id}` endpoint that allows an authenticated ADMIN user to update one or more of the following fields on an existing Seat: `seatNumber`, `seatClass`. Fields not present in the request body SHALL be left unchanged. The `trainId` field is immutable and SHALL NOT be updatable. The endpoint SHALL return the full updated Seat representation on success.

#### Scenario: Successful partial update of seat class only

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with body `{"seatClass": "BUSINESS"}`
- **THEN** the system SHALL update only `seatClass`, leave `seatNumber` unchanged, and return `200 OK` with the updated Seat in JSend success format

#### Scenario: Successful update of both seatNumber and seatClass

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with body `{"seatNumber": "B12", "seatClass": "FIRST_CLASS"}`
- **THEN** the system SHALL update both fields and return `200 OK`

#### Scenario: Seat not found

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with a non-existent `id`
- **THEN** the system SHALL return `404 Not Found` with JSend fail format and error code `SEAT_NOT_FOUND`

#### Scenario: seatNumber conflict within same train on update

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with a `seatNumber` already used by another Seat belonging to the same Train
- **THEN** the system SHALL return `409 Conflict` with JSend fail format and error code `SEAT_NUMBER_ALREADY_EXISTS`

#### Scenario: Same seatNumber as current value

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with the same `seatNumber` the Seat already has
- **THEN** the system SHALL NOT treat it as a conflict and SHALL successfully update the Seat

#### Scenario: Non-admin user is rejected

- **WHEN** a non-ADMIN authenticated user sends `PATCH /api/v1.0/seats/{id}`
- **THEN** the system SHALL return `403 Forbidden`

#### Scenario: Invalid seatClass value

- **WHEN** an ADMIN sends `PATCH /api/v1.0/seats/{id}` with `seatClass` set to an unrecognized enum value
- **THEN** the system SHALL return `400 Bad Request` with JSend fail format and error code `VALIDATION_ERROR`

## ADDED Requirements

### Requirement: Admin can partially update a Route

The system SHALL expose a `PATCH /api/v1.0/routes/{id}` endpoint that allows an authenticated ADMIN user to update one or more of the following fields on an existing Route: `departureTime`, `arrivalTime`, `basePrice`, `status`. Fields not present in the request body SHALL be left unchanged. The following fields are immutable and SHALL NOT be updatable via PATCH: `trainId`, `originStationId`, `destinationStationId`. The endpoint SHALL return the full updated Route representation on success.

#### Scenario: Successful partial update of route status

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with body `{"status": "CANCELLED"}`
- **THEN** the system SHALL update only `status`, leave all other fields unchanged, and return `200 OK` with the updated Route in JSend success format

#### Scenario: Successful update of departure and arrival times

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with body `{"departureTime": "2026-03-01T08:00:00Z", "arrivalTime": "2026-03-01T12:00:00Z"}`
- **THEN** the system SHALL update both time fields and return `200 OK`

#### Scenario: Successful update of base price

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with body `{"basePrice": 150000}`
- **THEN** the system SHALL update `basePrice` and return `200 OK`

#### Scenario: Route not found

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with a non-existent `id`
- **THEN** the system SHALL return `404 Not Found` with JSend fail format and error code `ROUTE_NOT_FOUND`

#### Scenario: Non-admin user is rejected

- **WHEN** a non-ADMIN authenticated user sends `PATCH /api/v1.0/routes/{id}`
- **THEN** the system SHALL return `403 Forbidden`

#### Scenario: Invalid status value

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with `status` set to an unrecognized value
- **THEN** the system SHALL return `400 Bad Request` with JSend fail format and error code `VALIDATION_ERROR`

#### Scenario: Negative base price rejected

- **WHEN** an ADMIN sends `PATCH /api/v1.0/routes/{id}` with `basePrice` set to a negative number
- **THEN** the system SHALL return `400 Bad Request` with JSend fail format and error code `VALIDATION_ERROR`

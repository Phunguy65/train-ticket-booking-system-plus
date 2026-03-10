# MODIFIED Requirements

## Requirement: Train API endpoints

The Train REST API SHALL expose the following endpoints:
- `POST /api/{version}/trains` — create a Train (existing)
- `GET /api/{version}/trains` — list Trains with pagination (existing)
- `GET /api/{version}/trains/{id}` — get a single Train (existing)
- `PUT /api/{version}/trains/{id}` — update a Train (existing)
- `DELETE /api/{version}/trains/{id}` — soft-delete a single Train (NEW)
- `DELETE /api/{version}/trains` — bulk soft-delete Trains (NEW)

All mutating endpoints (POST, PUT, DELETE) MUST require `ROLE_ADMIN`.
All responses MUST use the JSend envelope format (`{ "status", "data" }`).

### Scenario: Delete endpoints accessible only to admin

- **WHEN** a `ROLE_CUSTOMER` user calls any Train DELETE endpoint
- **THEN** the system returns `403 Forbidden`

## Requirement: Seat API endpoints

The Seat REST API SHALL expose the following endpoints:
- `POST /api/{version}/seats` — create a Seat (existing)
- `GET /api/{version}/trains/{trainId}/seats` — list Seats for a Train (existing)
- `GET /api/{version}/seats/{id}` — get a single Seat (existing)
- `PUT /api/{version}/seats/{id}` — update a Seat (existing)
- `DELETE /api/{version}/seats/{id}` — soft-delete a single Seat (NEW)
- `DELETE /api/{version}/seats` — bulk soft-delete Seats (NEW)

All mutating endpoints MUST require `ROLE_ADMIN`.

### Scenario: Delete endpoints accessible only to admin

- **WHEN** a `ROLE_CUSTOMER` user calls any Seat DELETE endpoint
- **THEN** the system returns `403 Forbidden`

## Requirement: Station API endpoints

The Station REST API SHALL expose the following endpoints:
- `POST /api/{version}/stations` — create a Station (existing)
- `GET /api/{version}/stations` — list Stations with pagination (existing)
- `GET /api/{version}/stations/{id}` — get a single Station (existing)
- `PUT /api/{version}/stations/{id}` — update a Station (existing)
- `DELETE /api/{version}/stations/{id}` — soft-delete a single Station (NEW)
- `DELETE /api/{version}/stations` — bulk soft-delete Stations (NEW)

All mutating endpoints MUST require `ROLE_ADMIN`.

### Scenario: Delete endpoints accessible only to admin

- **WHEN** a `ROLE_CUSTOMER` user calls any Station DELETE endpoint
- **THEN** the system returns `403 Forbidden`

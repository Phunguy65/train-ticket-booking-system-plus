# ADDED Requirements

## Requirement: Admin can soft-delete a single Station

An authenticated admin user SHALL be able to soft-delete a Station by its UUID.
The system MUST set `deleted_at` to the current timestamp on the `stations` row.
The Station record MUST remain in the database.
The operation MUST be idempotent: deleting an already-deleted Station SHALL return success without error.
The system MUST block deletion if any active (non-deleted) Route references the Station as `origin_station_id` or `destination_station_id`.

### Scenario: Successful single Station delete

- **WHEN** an admin sends `DELETE /api/1.0/stations/{id}` for a Station that has no active Routes
- **THEN** the system sets `stations.deleted_at = NOW()` and returns `200 OK` with `{ "status": "success", "data": null }`

### Scenario: Station not found

- **WHEN** an admin sends `DELETE /api/1.0/stations/{id}` for a UUID that does not exist
- **THEN** the system returns `404 Not Found` with `{ "status": "fail", "data": { "code": "STATION_NOT_FOUND" } }`

### Scenario: Station already soft-deleted (idempotent)

- **WHEN** an admin sends `DELETE /api/1.0/stations/{id}` for a Station whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with `{ "status": "success", "data": null }` without modifying the record

### Scenario: Station is origin of active Route — deletion blocked

- **WHEN** an admin sends `DELETE /api/1.0/stations/{id}` for a Station that is the `origin_station_id` of at least one Route with `deleted_at IS NULL`
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "STATION_IN_USE" } }`

### Scenario: Station is destination of active Route — deletion blocked

- **WHEN** an admin sends `DELETE /api/1.0/stations/{id}` for a Station that is the `destination_station_id` of at least one Route with `deleted_at IS NULL`
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "STATION_IN_USE" } }`

### Scenario: Unauthenticated request

- **WHEN** a request without a valid JWT token is sent to `DELETE /api/1.0/stations/{id}`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request

- **WHEN** a user with `ROLE_CUSTOMER` sends `DELETE /api/1.0/stations/{id}`
- **THEN** the system returns `403 Forbidden`

## Requirement: Admin can bulk soft-delete multiple Stations atomically

An authenticated admin user SHALL be able to soft-delete up to 100 Stations in a single request.
The operation MUST be atomic: either all Stations in the request are deleted or none are.
The system MUST validate all IDs against the active-route guard BEFORE performing any deletion.
If any Station in the list has active Routes (as origin or destination), the ENTIRE request MUST be rejected.
Already-deleted Stations in the list SHALL be skipped silently.
The request body MUST contain a non-empty array of UUIDs with a maximum of 100 elements.

### Scenario: Successful bulk Station delete

- **WHEN** an admin sends `DELETE /api/1.0/stations` with `{ "stationIds": ["uuid1", "uuid2"] }` where no Station has active Routes
- **THEN** the system sets `deleted_at` on all matching Stations and returns `200 OK` with `{ "status": "success", "data": { "deletedCount": N } }`

### Scenario: One Station in batch has active Routes — entire batch rejected

- **WHEN** an admin sends `DELETE /api/1.0/stations` with multiple IDs and one of them is referenced by an active Route
- **THEN** the system performs no deletions and returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "STATION_IN_USE", "conflictingIds": ["uuid-of-blocked-station"] } }`

### Scenario: Empty ID array rejected

- **WHEN** an admin sends `DELETE /api/1.0/stations` with `{ "stationIds": [] }`
- **THEN** the system returns `400 Bad Request` with validation error

### Scenario: More than 100 IDs rejected

- **WHEN** an admin sends `DELETE /api/1.0/stations` with more than 100 IDs
- **THEN** the system returns `400 Bad Request` with validation error

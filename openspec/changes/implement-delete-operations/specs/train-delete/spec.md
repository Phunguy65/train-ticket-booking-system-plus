# ADDED Requirements

## Requirement: Admin can soft-delete a single Train

An authenticated admin user SHALL be able to soft-delete a Train by its UUID.
The system MUST set `deleted_at` to the current timestamp on the `trains` row.
The Train record MUST remain in the database.
The operation MUST be idempotent: deleting an already-deleted Train SHALL return success without error.
The system MUST block deletion if any active (non-deleted) Route references the Train.

### Scenario: Successful single Train delete

- **WHEN** an admin sends `DELETE /api/1.0/trains/{id}` for a Train that has no active Routes
- **THEN** the system sets `trains.deleted_at = NOW()` and returns `200 OK` with `{ "status": "success", "data": null }`

### Scenario: Train not found

- **WHEN** an admin sends `DELETE /api/1.0/trains/{id}` for a UUID that does not exist
- **THEN** the system returns `404 Not Found` with `{ "status": "fail", "data": { "code": "TRAIN_NOT_FOUND" } }`

### Scenario: Train already soft-deleted (idempotent)

- **WHEN** an admin sends `DELETE /api/1.0/trains/{id}` for a Train whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with `{ "status": "success", "data": null }` without modifying the record

### Scenario: Train has active Routes — deletion blocked

- **WHEN** an admin sends `DELETE /api/1.0/trains/{id}` for a Train that has one or more Routes with `deleted_at IS NULL`
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "TRAIN_IN_USE" } }`

### Scenario: Unauthenticated request

- **WHEN** a request without a valid JWT token is sent to `DELETE /api/1.0/trains/{id}`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request

- **WHEN** a user with `ROLE_CUSTOMER` sends `DELETE /api/1.0/trains/{id}`
- **THEN** the system returns `403 Forbidden`

## Requirement: Admin can bulk soft-delete multiple Trains atomically

An authenticated admin user SHALL be able to soft-delete up to 100 Trains in a single request.
The operation MUST be atomic: either all Trains in the request are deleted or none are.
The system MUST validate all IDs against the active-route guard BEFORE performing any deletion.
If any Train in the list has active Routes, the ENTIRE request MUST be rejected.
Already-deleted Trains in the list SHALL be skipped silently (not treated as a failure).
The request body MUST contain a non-empty array of UUIDs with a maximum of 100 elements.

### Scenario: Successful bulk Train delete

- **WHEN** an admin sends `DELETE /api/1.0/trains` with `{ "trainIds": ["uuid1", "uuid2"] }` where no Train has active Routes
- **THEN** the system sets `deleted_at` on all matching Trains and returns `200 OK` with `{ "status": "success", "data": { "deletedCount": N } }`

### Scenario: One Train in batch has active Routes — entire batch rejected

- **WHEN** an admin sends `DELETE /api/1.0/trains` with three IDs and one of them has active Routes
- **THEN** the system performs no deletions and returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "TRAIN_IN_USE", "conflictingIds": ["uuid-of-blocked-train"] } }`

### Scenario: Empty ID array rejected

- **WHEN** an admin sends `DELETE /api/1.0/trains` with `{ "trainIds": [] }`
- **THEN** the system returns `400 Bad Request` with validation error

### Scenario: More than 100 IDs rejected

- **WHEN** an admin sends `DELETE /api/1.0/trains` with more than 100 IDs
- **THEN** the system returns `400 Bad Request` with validation error

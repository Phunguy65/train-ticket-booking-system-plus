# ADDED Requirements

## Requirement: Admin can soft-delete a single Seat

An authenticated admin user SHALL be able to soft-delete a Seat by its UUID.
The system MUST set `deleted_at` to the current timestamp on the `seats` row.
The Seat record MUST remain in the database.
The operation MUST be idempotent: deleting an already-deleted Seat SHALL return success without error.
The system MUST block deletion if any `route_seat_availability` record for that Seat has status `HELD` or `BOOKED`.
The system MUST NOT cascade the deletion to the owning Train.

### Scenario: Successful single Seat delete

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a Seat with no active availability records
- **THEN** the system sets `seats.deleted_at = NOW()` and returns `200 OK` with `{ "status": "success", "data": null }`

### Scenario: Seat not found

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a UUID that does not exist
- **THEN** the system returns `404 Not Found` with `{ "status": "fail", "data": { "code": "SEAT_NOT_FOUND" } }`

### Scenario: Seat already soft-deleted (idempotent)

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a Seat whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with `{ "status": "success", "data": null }` without modifying the record

### Scenario: Seat is actively HELD — deletion blocked

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a Seat that has at least one `route_seat_availability` record with `status = 'HELD'`
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "SEAT_IN_USE" } }`

### Scenario: Seat is actively BOOKED — deletion blocked

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a Seat that has at least one `route_seat_availability` record with `status = 'BOOKED'`
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "SEAT_IN_USE" } }`

### Scenario: Seat has only AVAILABLE or CANCELLED availability records — deletion allowed

- **WHEN** an admin sends `DELETE /api/1.0/seats/{id}` for a Seat whose all `route_seat_availability` records have status `AVAILABLE` or `CANCELLED`
- **THEN** the system sets `seats.deleted_at = NOW()` and returns `200 OK`

### Scenario: Unauthenticated request

- **WHEN** a request without a valid JWT token is sent to `DELETE /api/1.0/seats/{id}`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request

- **WHEN** a user with `ROLE_CUSTOMER` sends `DELETE /api/1.0/seats/{id}`
- **THEN** the system returns `403 Forbidden`

## Requirement: Admin can bulk soft-delete multiple Seats atomically

An authenticated admin user SHALL be able to soft-delete up to 100 Seats in a single request.
The operation MUST be atomic: either all Seats in the request are deleted or none are.
The system MUST validate all IDs against the active-availability guard BEFORE performing any deletion.
If any Seat in the list has a `route_seat_availability` record with status `HELD` or `BOOKED`, the ENTIRE request MUST be rejected.
Already-deleted Seats in the list SHALL be skipped silently.
The request body MUST contain a non-empty array of UUIDs with a maximum of 100 elements.

### Scenario: Successful bulk Seat delete

- **WHEN** an admin sends `DELETE /api/1.0/seats` with `{ "seatIds": ["uuid1", "uuid2"] }` where no Seat has active availability
- **THEN** the system sets `deleted_at` on all matching Seats and returns `200 OK` with `{ "status": "success", "data": { "deletedCount": N } }`

### Scenario: One Seat in batch is in use — entire batch rejected

- **WHEN** an admin sends `DELETE /api/1.0/seats` with multiple IDs and one of them has a `HELD` or `BOOKED` availability record
- **THEN** the system performs no deletions and returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "SEAT_IN_USE", "conflictingIds": ["uuid-of-blocked-seat"] } }`

### Scenario: Empty ID array rejected

- **WHEN** an admin sends `DELETE /api/1.0/seats` with `{ "seatIds": [] }`
- **THEN** the system returns `400 Bad Request` with validation error

### Scenario: More than 100 IDs rejected

- **WHEN** an admin sends `DELETE /api/1.0/seats` with more than 100 IDs
- **THEN** the system returns `400 Bad Request` with validation error

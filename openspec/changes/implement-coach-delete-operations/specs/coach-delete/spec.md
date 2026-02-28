# ADDED Requirements

## Requirement: Admin can soft-delete a single coach

The system SHALL allow an authenticated admin to soft-delete a coach by ID via `DELETE /api/1.0/trains/{trainId}/coaches/{id}`. The coach's `deletedAt` field SHALL be set to the current timestamp and a `CoachDeleted` domain event SHALL be published. The operation SHALL be idempotent: deleting an already-deleted coach returns success without side effects.

### Scenario: Successfully delete a coach with no active seats

- **WHEN** an admin sends `DELETE /api/1.0/trains/{trainId}/coaches/{id}` for a coach that exists, belongs to the given train, and has no active (non-deleted) seats
- **THEN** the system sets `deleted_at` on the coach row to the current timestamp
- **AND** returns `200 OK` with `{ "status": "success", "data": null }`
- **AND** publishes one `CoachDeleted` domain event carrying the coach's ID

### Scenario: Delete an already-deleted coach (idempotent)

- **WHEN** an admin sends `DELETE /api/1.0/trains/{trainId}/coaches/{id}` for a coach whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with `{ "status": "success", "data": null }` without modifying the record or publishing any event

### Scenario: Delete a coach that does not exist

- **WHEN** an admin sends `DELETE /api/1.0/trains/{trainId}/coaches/{id}` where `{id}` does not match any coach (active or deleted)
- **THEN** the system returns `404 Not Found` with `{ "status": "fail", "data": { "code": "COACH_NOT_FOUND", "message": "Coach not found" } }`

### Scenario: Delete a coach whose trainId does not match the path

- **WHEN** an admin sends `DELETE /api/1.0/trains/{trainId}/coaches/{id}` where the coach exists but belongs to a different train
- **THEN** the system returns `404 Not Found` (treated as not found under the given train path)

### Scenario: Delete a coach that has active seats

- **WHEN** an admin sends `DELETE /api/1.0/trains/{trainId}/coaches/{id}` for a coach that has one or more active (non-deleted) seat records
- **THEN** the system returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "COACH_IN_USE", "message": "One or more coaches have active seats and cannot be deleted", "conflictingIds": ["{id}"] } }`
- **AND** the coach's `deleted_at` remains null

### Scenario: Unauthenticated request is rejected

- **WHEN** a request without a valid JWT is sent to `DELETE /api/1.0/trains/{trainId}/coaches/{id}`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request is rejected

- **WHEN** a request with a valid JWT for a non-admin user is sent to `DELETE /api/1.0/trains/{trainId}/coaches/{id}`
- **THEN** the system returns `403 Forbidden`

---

## Requirement: Admin can bulk soft-delete coaches via query parameters

The system SHALL allow an authenticated admin to soft-delete multiple coaches in a single atomic request via `DELETE /api/1.0/coaches?ids={uuid}&ids={uuid}`. The operation SHALL be **fail-all**: if _any_ supplied coach ID has active seats, the entire request SHALL be rejected and no coaches SHALL be deleted. On success, `deletedAt` SHALL be set on all matching (non-deleted) coaches and a `CoachDeleted` event SHALL be published for each ID in the request.

### Scenario: Successfully bulk-delete coaches with no active seats

- **WHEN** an admin sends `DELETE /api/1.0/coaches?ids=uuid1&ids=uuid2` and none of the coaches have active seats
- **THEN** the system sets `deleted_at` on all matching non-deleted coach rows
- **AND** returns `200 OK` with `{ "status": "success", "data": { "deletedCount": <n> } }` where `n` is the number of rows actually updated
- **AND** publishes one `CoachDeleted` event per ID supplied in the request

### Scenario: Bulk delete is rejected when any coach has active seats (fail-all)

- **WHEN** an admin sends `DELETE /api/1.0/coaches?ids=uuid1&ids=uuid2&ids=uuid3` and `uuid2` has active seats
- **THEN** the system does NOT soft-delete any of the supplied coaches
- **AND** returns `422 Unprocessable Entity` with `{ "status": "fail", "data": { "code": "COACH_IN_USE", "message": "One or more coaches have active seats and cannot be deleted", "conflictingIds": ["uuid2"] } }`

### Scenario: Bulk delete with missing `ids` parameter

- **WHEN** an admin sends `DELETE /api/1.0/coaches` with no `ids` query parameter (or an empty list)
- **THEN** the system returns `400 Bad Request` with a JSend fail body indicating that at least one ID is required

### Scenario: Bulk delete exceeds maximum batch size

- **WHEN** an admin sends `DELETE /api/1.0/coaches` with more than 100 `ids` values
- **THEN** the system returns `400 Bad Request` with a JSend fail body indicating the batch size limit

### Scenario: Unauthenticated bulk-delete request is rejected

- **WHEN** a request without a valid JWT is sent to `DELETE /api/1.0/coaches?ids=...`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin bulk-delete request is rejected

- **WHEN** a request with a valid JWT for a non-admin user is sent to `DELETE /api/1.0/coaches?ids=...`
- **THEN** the system returns `403 Forbidden`

# ADDED Requirements

## Requirement: Admin can soft-delete a single Route

An authenticated admin user SHALL be able to soft-delete a Route by its UUID.
The system MUST set `deleted_at` to the current UTC timestamp on the `routes` row.
The Route record MUST remain in the database.
The operation MUST be idempotent: soft-deleting an already-deleted Route SHALL return success without error.
The system MUST cascade soft-delete to all associated Booking records (status HELD or CONFIRMED) within the same database transaction.

### Scenario: Successful single Route soft-delete

- **WHEN** an authenticated admin sends `DELETE /api/1.0/routes/{id}` for a Route that exists and is not yet deleted
- **THEN** the system sets `routes.deleted_at = NOW()`, cascades soft-delete to all associated Booking rows, and returns `200 OK` with body `{ "status": "success", "data": null }`

### Scenario: Route not found

- **WHEN** an authenticated admin sends `DELETE /api/1.0/routes/{id}` with a UUID that does not exist in the database
- **THEN** the system returns `404 Not Found` with body `{ "status": "fail", "data": { "message": "Route not found", "code": "ROUTE_NOT_FOUND" } }`

### Scenario: Route already soft-deleted (idempotent)

- **WHEN** an authenticated admin sends `DELETE /api/1.0/routes/{id}` for a Route whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with body `{ "status": "success", "data": null }` and performs no further database writes

### Scenario: Unauthenticated request

- **WHEN** a request without a valid auth token is sent to `DELETE /api/1.0/routes/{id}`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request

- **WHEN** a request with a valid auth token for a non-admin user is sent to `DELETE /api/1.0/routes/{id}`
- **THEN** the system returns `403 Forbidden`

---

## Requirement: Admin can bulk soft-delete multiple Routes

An authenticated admin user SHALL be able to soft-delete multiple Routes in a single request by supplying a list of UUIDs.
The request MUST contain at least 1 and at most 100 route IDs.
If ANY supplied ID does not exist (or is already deleted and being re-validated via a pre-check), the system MUST fail the entire operation atomically; no routes or bookings SHALL be modified.
The system MUST cascade soft-delete to all Booking records associated with the deleted routes within the same database transaction.
The operation MUST be executed within a single database transaction (all-or-nothing).

### Scenario: Successful bulk soft-delete

- **WHEN** an authenticated admin sends `POST /api/1.0/routes:bulkDelete` with a JSON body `{ "routeIds": ["<uuid1>", "<uuid2>"] }` where all IDs exist and are active
- **THEN** the system sets `deleted_at = NOW()` on all matched route rows, cascades soft-delete to their associated bookings, and returns `200 OK` with body `{ "status": "success", "data": { "deletedCount": 2 } }`

### Scenario: One or more IDs not found — entire operation fails

- **WHEN** an authenticated admin sends `POST /api/1.0/routes:bulkDelete` and at least one UUID does not exist in the database
- **THEN** the system returns `422 Unprocessable Entity` with body `{ "status": "fail", "data": { "message": "One or more routes not found", "code": "ROUTE_NOT_FOUND", "invalidIds": ["<uuid>"] } }` and NO routes or bookings are modified

### Scenario: Empty routeIds array

- **WHEN** an authenticated admin sends `POST /api/1.0/routes:bulkDelete` with `{ "routeIds": [] }`
- **THEN** the system returns `400 Bad Request` with a validation error indicating the array must not be empty

### Scenario: routeIds array exceeds maximum size

- **WHEN** an authenticated admin sends `POST /api/1.0/routes:bulkDelete` with more than 100 UUIDs
- **THEN** the system returns `400 Bad Request` with a validation error indicating the maximum allowed is 100

### Scenario: Unauthenticated request

- **WHEN** a request without a valid auth token is sent to `POST /api/1.0/routes:bulkDelete`
- **THEN** the system returns `401 Unauthorized`

### Scenario: Non-admin authenticated request

- **WHEN** a request with a valid auth token for a non-admin user is sent to `POST /api/1.0/routes:bulkDelete`
- **THEN** the system returns `403 Forbidden`

---

## Requirement: Booking cascade on Route soft-delete

When one or more Routes are soft-deleted, all associated Booking records (regardless of status) for those routes SHALL be soft-deleted within the same database transaction.
The cascade MUST be triggered by the `RouteDeleted` domain event published by the `SoftDeleteRouteUseCase`, consumed inside the Booking module via a `@TransactionalEventListener(phase = BEFORE_COMMIT)`.
The Booking module MUST NOT be called directly from the Route use case (Spring Modulith boundary must be respected).

### Scenario: Single route delete cascades to bookings

- **WHEN** a Route is successfully soft-deleted
- **THEN** all Booking rows with `route_id = <deleted_route_id>` have their `deleted_at` set to the same timestamp in the same transaction

### Scenario: Bulk route delete cascades to bookings

- **WHEN** multiple Routes are successfully soft-deleted via the bulk endpoint
- **THEN** all Booking rows whose `route_id` is in the deleted set have their `deleted_at` set to the same timestamp in the same transaction

### Scenario: Booking cascade failure rolls back route deletion

- **WHEN** the booking cascade listener throws an exception (e.g., database error)
- **THEN** the entire transaction is rolled back; neither the route nor any bookings are modified

---

## Requirement: Deleted Routes are excluded from listings and lookups

Active read operations (list routes, get route by ID) SHALL exclude soft-deleted routes from their results.
All JPQL queries in `RouteJpaRepository` that return route data MUST include a `WHERE deletedAt IS NULL` (or equivalent `AND deletedAt IS NULL`) condition.

### Scenario: Deleted route excluded from list

- **WHEN** a client requests `GET /api/1.0/routes`
- **THEN** routes with a non-null `deleted_at` are NOT included in the response

### Scenario: Deleted route not found by ID

- **WHEN** a client requests `GET /api/1.0/routes/{id}` for a soft-deleted route
- **THEN** the system returns `404 Not Found`

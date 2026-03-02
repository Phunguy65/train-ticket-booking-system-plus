# Why

The Route module currently exposes full CRUD via its REST controller but is missing delete endpoints. The database schema already has a `deleted_at` column on the `routes` table, yet the application layer (use cases, repository port, JPA queries, and controller endpoints) has never been wired up. Without soft delete, operators cannot retire routes that are no longer in service, and deleted routes cannot be recovered if removed by mistake.

## What Changes

- Add a `DELETE /{version}/routes/{id}` endpoint that soft-deletes a single route.
- Add a `POST /{version}/routes:bulkDelete` endpoint that soft-deletes multiple routes in one request; if **any** supplied ID is invalid or not found, the entire operation fails atomically.
- Cascade soft-delete to associated `Booking` records: when a route is soft-deleted, all active bookings on that route are also soft-deleted within the same transaction.
- Audit all existing `RouteJpaRepository` find-queries to ensure they filter `deleted_at IS NULL` (closing a potential data-leakage gap).
- Publish `RouteDeleted` and `RouteBulkDeleted` domain events after a successful soft-delete so downstream modules (e.g., notifications) can react.

## Capabilities

### New Capabilities

- `route-soft-delete`: Soft-delete one or many routes (with booking cascade) through dedicated REST endpoints, following the existing DDD / Vertical Slice pattern used by Train, Station, Seat, and Coach modules.

### Modified Capabilities

<!-- No existing spec-level behaviour changes — this is additive. -->

## Impact

- **Backend – Route module** (`backend/src/main/java/.../route/`): new domain methods, domain events, repository port additions, two new use cases, JPA query additions, new HTTP request DTO, two new controller endpoints.
- **Backend – Booking module**: soft-delete cascade triggered via a domain event listener (no direct cross-module method call — respects Spring Modulith boundaries).
- **Database**: `routes.deleted_at` column is already present; `bookings.deleted_at` must be confirmed present for cascade to work.
- **API contracts**: two new endpoints added; no existing endpoints removed or modified.
- **Tests**: new unit tests for domain aggregate, use cases, repository adapter, and controller.

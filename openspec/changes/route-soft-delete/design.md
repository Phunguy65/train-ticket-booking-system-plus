# Context

The Route module was fully scaffolded as part of `implement-route-crud` but delete endpoints were deliberately deferred. The `routes` table already has a `deleted_at TIMESTAMPTZ` column and `RouteEntity` maps it — but `Route.java` (domain aggregate), `RouteRepository` (port), and every layer above are missing soft-delete support.

A confirmed design decision (from the exploration phase) requires that when a route is soft-deleted, all associated bookings on that route must also be soft-deleted **within the same transaction**, to keep the data model consistent. However, the `bookings` table currently has **no `deleted_at` column** and `BookingEntity` has no such mapping — this must be added as part of this change.

The booking cascade will be implemented via a **`@TransactionalEventListener`** in the Booking module listening for `RouteDeleted` / `RouteBulkDeleted` events. This preserves Spring Modulith module isolation (no direct cross-module method call) while ensuring the cascade fires within the same transaction (using `TransactionPhase.BEFORE_COMMIT`).

## Goals / Non-Goals

**Goals:**

- Add `Route.softDelete()` domain method (idempotent, registers `RouteDeleted` event).
- Add `softDeleteById` / `softDeleteByIds` to `RouteRepository` port and `RouteJpaRepository`.
- Implement `SoftDeleteRouteUseCase` and `BulkSoftDeleteRoutesUseCase`.
- Expose `DELETE /{version}/routes/{id}` and `POST /{version}/routes:bulkDelete` on `RouteController`.
- Cascade soft-delete to Booking rows via a domain event listener inside the Booking module.
- Audit existing `RouteJpaRepository` queries to add `deleted_at IS NULL` guards.
- Add `deleted_at` column + mapping to `BookingEntity`.

**Non-Goals:**

- Restore (un-delete) routes or bookings.
- Hard (physical) deletion of routes.
- Pagination or chunking of bulk delete beyond the `@Size(max = 100)` guard.
- Notifying end-users (email, push) about booking cancellations — that is a separate change.

## Decisions

### Decision 1 — Booking cascade via `@TransactionalEventListener(BEFORE_COMMIT)`

**Chosen**: publish `RouteDeleted` / `RouteBulkDeleted` domain events from the use case; the Booking module consumes them with a `@TransactionalEventListener(phase = BEFORE_COMMIT)` that issues a bulk `UPDATE bookings SET deleted_at = :now WHERE route_id IN (...)`.

**Alternatives considered**:

- *Direct call from use case* — violates Spring Modulith boundaries; ArchUnit would reject it.
- *Async event (AFTER_COMMIT)* — creates a window where the route is gone but bookings are still active; inconsistent state visible to other transactions.
- *Database-level ON DELETE CASCADE* — requires DDL change to bookings FK; does not work with soft delete anyway.

`BEFORE_COMMIT` is the right choice: it runs inside the same transaction, so a failure in the booking cascade rolls back the route deletion atomically.

### Decision 2 — Bulk delete: fail-all if any ID not found

**Chosen**: validate all IDs before touching the database; return `RouteError.RouteNotFound` (with the list of invalid IDs) if any are missing.

**Alternatives considered**:

- *Partial success / ignore missing* — caller cannot trust the result; requires a more complex response schema.
- *Silent ignore* — caller gets a success response even though their intent was not fully satisfied.

Fail-all is consistent with the existing `BulkSoftDeleteTrainsUseCase` pattern (which fails on `TrainInUse` rather than skipping).

### Decision 3 — Single delete: load aggregate, call `softDelete()`, then save

For the single-ID endpoint the use case loads the `Route` aggregate, calls `route.softDelete()` (which sets `deletedAt` and registers the `RouteDeleted` event), then calls `routeRepository.save(route)`. This is the same pattern used by `SoftDeleteTrainUseCase` and `SoftDeleteCoachUseCase` and keeps the domain aggregate as the source of truth.

For bulk, a single `@Modifying @Query` `UPDATE` is used (matching `BulkSoftDeleteTrainsUseCase`) to avoid N round-trips. Domain events are published manually per ID after the bulk update.

### Decision 4 — `RouteJpaRepository.findById` must use active-only query

The existing `RouteRepositoryAdapter.findById` delegates to Spring Data's default `findById`, which does **not** filter `deleted_at IS NULL`. This must be replaced with a named JPQL query `findActiveById` (same pattern as `TrainJpaRepository`, `StationJpaRepository`, `SeatJpaRepository`).

### Decision 5 — `bookings.deleted_at` added via Flyway migration

A new Flyway migration script adds `ALTER TABLE bookings ADD COLUMN deleted_at TIMESTAMPTZ`. `BookingEntity` gets the corresponding `@Column(name = "deleted_at") private Instant deletedAt` field. No new Flyway migration is needed for `routes` (column already present).

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| `BookingEntity` missing `deleted_at` means listener cannot set it | Add DB column + entity mapping in this change (migration required). |
| `RouteJpaRepository.findById` leaks deleted routes | Replace with `findActiveById` in this change (audit task). |
| `BEFORE_COMMIT` listener failure rolls back the whole route delete | Desired behaviour — keeps consistency. Document this in tests. |
| Bulk event publishing loops over all IDs after a single UPDATE | Acceptable for `max = 100` items. If limit grows, switch to a single `RouteBulkDeleted` event carrying the list of IDs. |
| `RouteJpaRepository.findAllWithFilter` currently does not filter deleted routes | Add `AND r.deletedAt IS NULL` condition in the audit task. |

## Migration Plan

1. Create Flyway migration: `ALTER TABLE bookings ADD COLUMN deleted_at TIMESTAMPTZ`.
2. Deploy backend — new column is nullable, no data migration needed.
3. Implement Route soft-delete code (domain → application → infrastructure → web).
4. Implement Booking cascade listener.
5. Smoke-test both endpoints in development before merging.
6. **Rollback**: remove the two new endpoints from the controller (feature-flag equivalent); the DB column addition is backward-compatible and can remain.

## Open Questions

- Should `RouteBulkDeleted` be a single event carrying `List<RouteId>` instead of N × `RouteDeleted` events? Currently N individual events are emitted (consistent with `BulkSoftDeleteTrainsUseCase`). Revisit if bulk size limit is raised above 100.
- Does the Booking module need to publish its own `BookingCancelledDueToRouteDeletion` event for downstream notification? Deferred to a future notification change.

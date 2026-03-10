# Context

The backend uses Spring Boot with Spring Modulith (JPA outbox), Clean Architecture, and DDD. Entities are organized into `station` and `train` modules. All five entities — Station, Route, Train, Coach, Seat — already have `deletedAt: Instant` and a `softDelete()` method on their aggregate roots. Bulk delete use cases exist for all entities and perform a single `UPDATE ... WHERE id IN (...)` query, but then emit one `*Deleted` event per record in a for-loop, writing N rows to the `event_publication` outbox table.

Currently, all bulk delete use cases guard against child dependencies (e.g., Station blocks if active routes exist). There is no cascade path — a Station with routes cannot be deleted at all. `RouteSeatAvailability` (RSA) is a join table with status; it has no soft delete and no hard delete methods. It is created by a `SeatAvailabilitySeeder` listener on `RouteCreated`, but nothing cleans it up on delete.

## Goals / Non-Goals

**Goals:**

- Enable top-down cascade soft delete: deleting a Station cascades to Routes → Trains (if orphaned) → Coaches → Seats
- Hard delete orphaned `RouteSeatAvailability` rows as part of the cascade
- Replace N-event for-loops in all `BulkSoftDelete*UseCase` files with a single bulk domain event
- Keep all changes within existing module boundaries (no new cross-module dependencies)
- Preserve single-entity delete flows unchanged

**Non-Goals:**

- Hard delete of any entity (soft delete only, except RSA)
- Cancelling active bookings when a cascade delete occurs (separate concern)
- Frontend or API contract changes
- Restoring (un-deleting) cascaded entities

## Decisions

### Decision 1: Event-driven cascade via `@ApplicationModuleListener` (Option A) over orchestrated use case (Option B)

Cascade listeners react to bulk delete events in separate transactions, each scoped to one level of the hierarchy. This fits naturally with Spring Modulith's outbox: each listener runs in its own transaction, failures are retried automatically, and no single transaction spans the entire hierarchy.

Option B (one large `@Transactional` use case) would work but creates a large transaction scope, couples all cascade logic into one class, and goes against the event-driven grain of the existing architecture (the only existing listener, `SeatAvailabilitySeeder`, already demonstrates this pattern).

**Accepted trade-off**: eventual consistency — there is a window between Station soft delete and Route soft delete. This is acceptable for a train booking system where admin-initiated deletes are not time-critical.

### Decision 2: Cascade listeners live in the `train` module, not `station`

The `train` module already declares `allowedDependencies = {"station::model", ...}` and `StationDeleted` lives in `station::model`. Placing cascade listeners in `train` means they can directly access `RouteRepository`, `TrainRepository`, `CoachRepository`, `SeatRepository`, and `RouteSeatAvailabilityRepository` without any new cross-module ports. No changes to `package-info.java` module boundaries are needed.

Alternative (listeners in `station` module) would require a new `RouteCascadePort` interface and adapter, adding indirection with no benefit.

### Decision 3: Bulk domain events replace per-record event loops

Each `BulkSoftDelete*UseCase` currently emits N individual events. Replace with a single bulk event record per entity type: `StationsDeleted`, `RoutesDeleted`, `TrainsDeleted`, `CoachesDeleted`, `SeatsDeleted`. Single-entity delete use cases continue to emit the existing single events (`StationDeleted`, etc.) — no change there.

Cascade listeners emit bulk events at each level, so the entire cascade chain produces at most 5 outbox rows regardless of how many records are affected.

### Decision 4: RSA hard delete triggered at two points in the cascade

- **Primary**: in `CascadeOnRoutesDeletedListener` — `hardDeleteByRouteIds(routeIds)` before soft-deleting trains. This covers the main path (Route deleted → RSA for that route is meaningless).
- **Safety net**: in `CascadeOnCoachesDeletedListener` — `hardDeleteBySeatIds(seatIds)` before soft-deleting seats. This covers the case where seats are bulk-deleted directly (not via cascade).

RSA has no soft delete and no audit requirement — hard delete is correct.

### Decision 5: Train orphan check before cascading Train delete

A Train may be referenced by multiple Routes. When Routes are cascade-deleted, only Trains with zero remaining active routes should be cascade-deleted. The check uses a new `countActiveByTrainId(TrainId)` repository method called per train ID after the route soft delete commits.

```
trainIds = findDistinctActiveTrainIdsByRouteIds(deletedRouteIds)
orphanedTrainIds = trainIds.filter(t -> countActiveByTrainId(t) == 0)
// only orphanedTrainIds are soft-deleted and cascade-propagated
```

## Risks / Trade-offs

**Eventual consistency window** → Acceptable per decision 1. Station appears deleted immediately; its routes disappear after the next listener transaction (milliseconds to seconds depending on outbox polling interval).

**Cascade triggered by direct Route/Train/Coach bulk delete** → `BulkSoftDeleteRoutesUseCase` now emits `RoutesDeleted`, which triggers `CascadeOnRoutesDeletedListener`. This means directly deleting a Route will also cascade-delete its orphaned Trains, Coaches, and Seats. This is the correct behavior but is a behavioral change from the current "block if children exist" approach. Callers must be aware.

**Active bookings on cascade-deleted seats** → RSA records with `HELD` or `BOOKED` status will be hard-deleted as part of the cascade. The booking module is not notified in this change. A follow-up change should add a `BookingCancellationOnCascadeListener` in the booking module listening to `RoutesDeleted`.

**Outbox retry on partial cascade failure** → If `CascadeOnRoutesDeletedListener` fails mid-way (e.g., after RSA hard delete but before Train soft delete), Spring Modulith will retry the entire listener. RSA hard delete is idempotent (`DELETE WHERE id IN (...)` on already-deleted rows is a no-op), so retries are safe.

**Large IN clauses** → Bulk queries use `WHERE id IN :ids`. For very large sets (thousands of IDs), this may hit database or JDBC limits. Acceptable for current scale; batching can be added later if needed.

## Migration Plan

No schema migration required — all entities already have `deleted_at` columns. RSA hard delete uses existing table structure.

Deployment is a standard rolling deploy. The new listeners are additive; existing single-entity delete endpoints are unchanged. The behavioral change (cascade instead of block) only affects bulk delete endpoints.

Rollback: revert the `BulkSoftDelete*UseCase` event emission change and remove the cascade listener classes. No data migration needed on rollback since soft deletes are reversible (set `deleted_at = NULL`).

## Open Questions

- Should the booking module be notified of cascade deletes in this change, or deferred? (Current decision: deferred — out of scope)
- Should there be a maximum cascade depth guard (e.g., refuse to cascade-delete a Station with more than N routes) to prevent accidental mass deletes? (Not implemented — could be added as a pre-check in the use case)

# Context

The backend exposes CRUD APIs for three master-data entities — Train, Seat, and Station. Create, read, and update are implemented. Delete is absent. The PostgreSQL schema already includes `deleted_at TIMESTAMPTZ` columns and supporting partial unique indexes on all three tables, so the infrastructure is ready. The application layer is not.

The User module provides a reference implementation of the full soft-delete pattern (domain model method, use case, bulk use case, JPA `@Modifying` query, REST endpoints, JSend response). This change mirrors that pattern across Train, Seat, and Station, augmenting it with pre-delete business guards that are specific to the relationships these entities participate in.

A secondary constraint is module boundaries: the `station` module must check Route usage before deleting a Station, but `station` and `train` are sibling modules. A cross-module dependency from `station → train::repository` would be legal via Spring Modulith but would create an implicit coupling that future refactors may regret. The solution is a narrow named-interface port.

**Current state:**

```
trains/seats/stations tables  → deleted_at column EXISTS (DB ready)
TrainEntity / SeatEntity / StationEntity  → deletedAt field MISSING
Train / Seat / Station (domain)           → softDelete() / isDeleted() MISSING
*Repository interfaces                    → softDelete* methods MISSING
*Controller                               → DELETE endpoints MISSING
```

## Goals / Non-Goals

**Goals:**

- Implement soft-delete (single + bulk) for Train, Seat, Station following the existing User module pattern
- Block Train delete when active (non-deleted) Routes reference the Train
- Block Seat delete when any `route_seat_availability` record for the Seat is `HELD` or `BOOKED`
- Block Station delete when active (non-deleted) Routes reference the Station as origin or destination
- Expose a `RouteValidationPort` named-interface from the `train` module for safe cross-module access by `station`
- Bulk delete is **atomic**: if any single entity in the batch fails the guard check, the entire request is rejected with 422 before any deletion occurs
- All delete endpoints require `ADMIN` role
- No database schema migration needed

**Non-Goals:**

- Hard (physical) delete of any entity
- Cascade soft-delete from Train to its Seats (Seats are independent aggregate roots)
- Cascade soft-delete from Train/Station to Routes (Routes carry historical data)
- Restore / undelete endpoints (out of scope for this change)
- Frontend changes
- Async / batch processing for very large ID sets (max 100 IDs per bulk request, same cap as User bulk delete)

## Decisions

### Decision 1 — Soft delete, not hard delete

**Choice:** Set `deleted_at = NOW()` rather than issuing a `DELETE` statement.

**Rationale:** All foreign keys in the schema use `NO ACTION` (no cascade). Hard-deleting a Train would immediately produce FK violations from `routes.train_id` and `seats.train_id`. Beyond constraint enforcement, the booking history (`booking_seats`, `transactions`) must remain intact as financial audit data — the same rationale the User module cites for not cascading to bookings. Soft delete preserves referential integrity and aligns with the existing codebase convention.

**Alternatives considered:** `ON DELETE CASCADE` at the DB level — rejected because it would silently wipe historical booking data.

---

### Decision 2 — Block on pre-condition, not post-failure

**Choice:** Use cases check the guard condition *before* attempting the soft delete, returning a typed `Result.failure(...)` immediately.

**Rationale:** Failing fast with a domain error (`TrainInUse`, `SeatInUse`, `StationInUse`) gives the caller a clear, actionable message. Relying on a DB exception bubble-up would leak infrastructure concerns into the API response and couple error semantics to database dialect.

**Alternatives considered:** Letting the FK violation surface and catching it in the global exception handler — rejected because `deleted_at` changes don't actually trigger FK violations (the row still exists), so for soft delete there is no automatic guard at the DB level.

---

### Decision 3 — Atomic bulk delete (all-or-none)

**Choice:** For bulk operations, validate all IDs against the guard before committing any deletion. If any ID fails, return 422 with the list of conflicting IDs and abort.

**Rationale:**
- Partial success in bulk delete creates ambiguity for the caller: they must diff their original list against the response to know what happened.
- Admin workflows (the only callers given the `ADMIN` guard) typically act on a curated selection; partial deletion of that selection is more harmful than a clean rejection.
- The User bulk delete uses a non-atomic `UPDATE … WHERE id IN (…) AND deleted_at IS NULL`, which silently skips already-deleted users. For entities with active-reference guards the semantics are richer and warrant explicit control.

**Alternatives considered:** Non-atomic (partial success, return `deletedCount` + `failed[]`) — rejected for the reasoning above; may revisit if admin UX demands it.

---

### Decision 4 — RouteValidationPort as a named-interface from `train` module

**Choice:** The `train` module exposes a `RouteValidationPort` interface under a `@NamedInterface("validation")` annotation. The `station` module declares a dependency on `train::validation` in its `package-info.java`.

```
train/
└── application/
    └── port/
        └── RouteValidationPort.java      ← exposed via @NamedInterface("validation")

station/
└── package-info.java                     ← allowedDependencies = {"train::validation", ...}
└── application/
    └── usecase/
        └── SoftDeleteStationUseCase.java ← injects RouteValidationPort
```

**Rationale:** Keeps the `station` module ignorant of Route JPA internals. The port expresses intent (`hasActiveRoutesForStation`) without coupling to any persistence type. Spring Modulith enforces this boundary via ArchUnit at test time.

**Alternatives considered:**
- Direct injection of `RouteRepository` in station use case — rejected; violates module boundary.
- Domain event approach (Station publishes `StationDeletionRequested`, train module responds) — rejected; overkill for a synchronous guard.
- Shared kernel interface — rejected; `RouteValidationPort` is train-domain knowledge, not truly shared.

---

### Decision 5 — Seat guard via `RouteSeatAvailabilityRepository`, not `BookingRepository`

**Choice:** Check active seat usage through `route_seat_availability` (`status IN ('HELD', 'BOOKED')`) rather than joining `booking_seats` → `bookings`.

**Rationale:** `RouteSeatAvailability` is owned by the `train` module (same module as `Seat`), so no cross-module dependency is introduced. It is the authoritative real-time signal of seat occupancy. A seat with status `AVAILABLE` or `CANCELLED` in all availability records is safe to soft-delete regardless of historical bookings.

**Alternatives considered:** Querying `booking_seats` joined to `bookings` — rejected because `bookings` lives in a different module, requiring a cross-module port or violating the booking module boundary.

---

### Decision 6 — JPA bulk update query, not load-then-save loop

**Choice:** Bulk soft-delete use cases issue a single `@Modifying @Query("UPDATE … SET deleted_at = :now WHERE id IN :ids AND deleted_at IS NULL")` rather than loading each aggregate and calling `softDelete()`.

**Rationale:** Mirrors the User bulk delete pattern. Loading N aggregates to set a single field is wasteful. Domain events for bulk deletes are published manually by the use case (one `TrainDeleted` / `SeatDeleted` / `StationDeleted` per ID) after the batch UPDATE, consistent with `BulkSoftDeleteUsersUseCase`.

**Single delete** still loads the aggregate (to check `isDeleted()` idempotency, run the guard, and register the domain event via `train.softDelete()`).

---

### Decision 7 — Response format

**Choice:** Successful single delete → `200 OK` with `JsendResponse.success()` (null data). Successful bulk delete → `200 OK` with `JsendResponse.success(Map.of("deletedCount", n))`. Guard failure → `422 Unprocessable Entity` with `JsendResponse.fail(...)`.

**Rationale:** Consistent with UserController pattern. `422` (rather than `409 Conflict`) is more appropriate: the request is syntactically valid but violates business rules (active references prevent deletion).

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Bulk guard check does N existence queries (one per entity) | Max 100 IDs per request; queries are indexed PKs — acceptable at this scale |
| Soft-deleted Trains still appear as `total_seats` source in availability seeder | `SeatAvailabilitySeeder` listens to `RouteCreated`; route creation is already blocked for deleted trains (guard added to `CreateRouteUseCase` separately if needed) |
| `RouteValidationPort` leaks `StationId` type into `train` module's port | `StationId` is in `station::model` named-interface; `train` module must declare `allowedDependencies = {"station::model"}` — this is a new dependency direction to document |
| Idempotent soft-delete on already-deleted entities silently succeeds | Intentional; reduces noise in retry scenarios |
| Atomic bulk reject on partial conflict may frustrate admins deleting large mixed lists | Admin UI can pre-filter "in use" entities before sending bulk request; the error response will include the conflicting IDs |

## Migration Plan

1. Deploy backend — no DB migration needed (`deleted_at` columns exist).
2. Existing queries that do not filter `deleted_at IS NULL` will start returning soft-deleted records unless updated. Audit all `findAll` queries in Train, Seat, Station JPA repositories and add the filter where missing (covered in tasks).
3. Rollback: revert the backend deployment; no data is lost (soft delete is non-destructive).

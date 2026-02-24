## Context

The `seats` table has existed since V1 but the `train` module has never been given a domain layer for Seat. The column `seats.status` was added with values `AVAILABLE | RESERVED | BLOCKED` but is never queried, never updated by any code, and models the wrong concept — a globally-mutable per-seat status cannot correctly represent "is this seat available on Route X?" when the same physical seat may be available on Route Y at the same time. Seat availability is inherently a per-route concern.

The booking module already references `SeatId` as a value object, and `CreateBookingUseCase` accepts a `seatId` blindly without verifying that the seat exists or is available for the requested route. This is a correctness gap.

## Goals / Non-Goals

**Goals:**

- Introduce a complete `Seat` vertical slice in the `train` module (domain → application → infrastructure)
- Replace the incorrect `seats.status` column with a dedicated `route_seat_availability` table that tracks availability per route
- Pre-populate `route_seat_availability` rows when a route is created (via Spring Modulith event listener)
- Add `GetAvailableSeatsForRouteUseCase` so callers can query available seats before booking
- Update `CreateBookingUseCase` to validate seat availability (with optimistic locking) before persisting a booking
- Deliver a Flyway migration `V4.0.0` that removes `seats.status` / its index and creates `route_seat_availability`

**Non-Goals:**

- Coach/wagon entity — seat hierarchy remains flat (`Train → Seat`); coach grouping can be encoded via `seat_number` convention if needed
- Seat hold/expiry timer (temporary HELD state before payment) — deferred to a future change
- Frontend/UI changes — no customer or admin UI work in this change
- Seat layout or visual seat-map rendering
- Seat pricing tiers per seat class (pricing lives on Route, not Seat)

## Decisions

### Decision 1 — `seats.status` is removed, not renamed

**Rationale**: The column is entirely unused (no Java code reads or writes it). Renaming it to `operational_status` would imply a maintenance/physical-state concept that adds complexity without a current use case. Removing it keeps the `seats` table as pure reference data (seat inventory). If a physical-status concept is needed in the future, a new migration can add it with the correct semantics.

### Decision 2 — Per-route availability via `route_seat_availability` table

**Rationale**: The same physical seat appears on multiple routes (e.g., daily train runs). A global status on the seat row cannot represent "available on Monday's run, booked on Tuesday's run." A join table with `(route_id, seat_id)` composite PK is the correct model.

**Schema**:
```
route_seat_availability
  route_id   UUID  NOT NULL  FK → routes(id)
  seat_id    UUID  NOT NULL  FK → seats(id)
  status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
             CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED'))
  version    INTEGER NOT NULL DEFAULT 0   -- optimistic locking
  PRIMARY KEY (route_id, seat_id)
```

`HELD` status is intentionally omitted for now (hold/expiry is deferred). `CANCELLED` allows a seat to return to a bookable state when a booking is cancelled.

### Decision 3 — Optimistic locking, not pessimistic

**Rationale**: Pessimistic locking (`SELECT … FOR UPDATE`) serializes all seat writes and creates bottlenecks under concurrent load. Optimistic locking with a `version` column allows concurrent reads and only conflicts at commit time, which is the right tradeoff for a web booking system. The `version` column is incremented on every status transition; if two requests race to book the same seat, exactly one wins and the other returns a `409 Conflict`.

### Decision 4 — Availability pre-population via `RouteCreated` domain event

**Rationale**: When a route is created, `route_seat_availability` rows must be seeded for every seat belonging to that route's train. Using the `RouteCreated` domain event and a Spring Modulith `@ApplicationModuleListener` keeps the train module decoupled from the route creation flow. The listener queries `SeatRepository.findByTrainId(trainId)` and bulk-inserts `route_seat_availability` rows with `status = AVAILABLE`.

### Decision 5 — Cross-module seat validation in `CreateBookingUseCase`

**Rationale**: The `booking` module needs to check and update `route_seat_availability` when creating a booking. To avoid breaking Spring Modulith module boundaries, the `train` module exposes a named interface (`@NamedInterface("availability")`) containing `RouteSeatAvailabilityPort` — an application port interface. The `booking` module declares `allowedDependencies = {"train::availability"}` and depends only on that port, not on JPA internals. The adapter lives in the `train` module's infrastructure layer.

### Decision 6 — `Seat` as a simple aggregate (no lifecycle events on creation)

**Rationale**: Seats are administrative reference data created by admins. Unlike `Booking` (which triggers downstream effects on creation), a new `Seat` does not need to publish events at creation time. `SeatCreated` event is not produced in this change. If future changes need to react to seat creation (e.g., adding seat to an existing route), an event can be added then.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|---|---|---|
| Migration V4.0.0 drops `seats.status` which is referenced by `idx_seats_train_status` index — must drop index first | High (already handled) | Migration drops index before dropping column |
| Bulk insert of `route_seat_availability` rows on `RouteCreated` may be slow for trains with many seats | Low (typical: < 100 seats) | Batch insert; if trains scale to 1000+ seats, switch to async listener |
| Optimistic lock conflict in `CreateBookingUseCase` needs clean error mapping to `409 Conflict` | Medium | `GlobalExceptionHandler` handles `ObjectOptimisticLockingFailureException` → `409` |
| `booking` module depending on `train::availability` port may feel like coupling | Low | Named interface + port abstraction keeps it clean; Spring Modulith verifies the boundary |
| `route_seat_availability` has no `HELD` status — a user could start checkout on a seat that gets booked by someone else before payment | Medium | Acceptable for now; the optimistic lock ensures exactly-once booking; the user gets a clear error and can retry |

# Context

The `booking` module directory exists but is empty. The database schema (`bookings`, `route_seat_availability`) is fully provisioned in `B1_0_0__baseline.sql`, and the `train` module already exposes a `RouteSeatAvailabilityPort` with `holdSeats`, `confirmHeldSeats`, `releaseHeldSeats`, and `cancelBookedSeats` operations. However, the port adapter currently uses pessimistic locking (`SELECT FOR UPDATE`), while the schema has a `version` column intended for optimistic locking. This change implements the booking module and migrates seat-availability concurrency to optimistic locking.

## Goals / Non-Goals

**Goals:**

- Implement the `booking` vertical slice (domain, application, infrastructure) following the established Spring Modulith + DDD pattern
- Multi-seat atomic hold: one `Booking` aggregate holds N seats in a single `@Transactional` boundary
- 15-minute seat hold with `payment_deadline`; background scheduler releases expired holds
- Status-aware cancellation: `HELD` → release seats; `CONFIRMED` → cancel seats + emit refund event
- Migrate `RouteSeatAvailabilityEntity` to JPA `@Version` optimistic locking; remove `SELECT FOR UPDATE`
- Idempotent booking creation via `idempotency_key`

**Non-Goals:**

- Payment processing or Stripe integration (handled by a future payment module)
- Booking confirmation flow (triggered by payment webhook, out of scope here)
- Frontend UI changes
- Admin-side booking management

## Decisions

### Decision 1 — Optimistic locking via JPA `@Version`, not `SELECT FOR UPDATE`

The existing `RouteSeatAvailabilityEntity` uses `@Lock(PESSIMISTIC_WRITE)` with a 3 s timeout. The schema already has `version INTEGER NOT NULL DEFAULT 1`.

**Chosen**: Add `@Version Integer version` to `RouteSeatAvailabilityEntity`. JPA automatically appends `AND version = ?` to every `UPDATE` and increments it. On version mismatch, JPA throws `OptimisticLockException`, which Spring's `@Transactional` rolls back automatically.

**Why over pessimistic**: Seat booking has low per-seat contention in practice (each seat is booked once). Optimistic locking avoids row-level blocking, improves throughput under concurrent load, and eliminates the 3 s lock-timeout edge case. The `version` column was always the intended mechanism per the original design doc.

**Multi-seat atomicity**: `saveAll()` inside a single `@Transactional` method means any `OptimisticLockException` on any seat rolls back the entire transaction — all-or-nothing is guaranteed by the transaction boundary, not by application-level count checks.

**Alternative considered**: Keep pessimistic locking for `holdSeats` only (highest contention path). Rejected because it mixes strategies in the same entity and the `version` column goes unused.

### Decision 2 — `Booking` aggregate does NOT store `List<SeatId>`; seats are tracked via `route_seat_availability.booking_id`

The `bookings` table has no `seat_ids` column. Seats are linked to a booking via `route_seat_availability.booking_id` (FK).

**Chosen**: `Booking` aggregate holds only scalar fields from the `bookings` table. When the booking module needs seat IDs (e.g., for cancellation), it calls `RouteSeatAvailabilityPort.findSeatIdsByBookingId(BookingId)` — a new method added to the port.

**Why**: Single source of truth stays in `route_seat_availability`. No denormalization, no `@ElementCollection` join table, no sync risk between two tables.

**Alternative considered**: Add `@ElementCollection List<UUID> seatIds` to `BookingEntity` backed by a `booking_seats` table. Rejected — adds a new table not in the baseline schema and duplicates data already in `route_seat_availability`.

### Decision 3 — `BookingStatus` uses `HELD / CONFIRMED / CANCELLED` (not `PENDING`)

The existing `backend-booking-slice` spec used `PENDING → CONFIRMED → CANCELLED`. The database `chk_booking_status` constraint enforces `HELD | CONFIRMED | CANCELLED`.

**Chosen**: Align domain model with the DB constraint. `HELD` is the initial state after seat reservation. `CONFIRMED` is set by the payment module (future). `CANCELLED` is terminal.

### Decision 4 — `CancelBookingUseCase` is status-aware and emits a typed event

Cancellation behavior differs by current status:
- `HELD` → call `releaseHeldSeats()` (seats: `HELD → AVAILABLE`); no refund
- `CONFIRMED` → call `cancelBookedSeats()` (seats: `BOOKED → CANCELLED`); emit `BookingCancelled { requiresRefund = true }`

**Chosen**: `Booking.cancel()` returns `Result<BookingCancelled, BookingError>` where the event carries `previousStatus`. The use case inspects `previousStatus` to call the correct port method and publish the event. The payment module will listen to `BookingCancelled` and initiate a refund when `requiresRefund = true`.

### Decision 5 — Expiry via Spring `@Scheduled` (60 s interval) + lazy check on seat availability query

**Chosen**: `BookingExpiryScheduler` runs `ExpireHeldBookingsUseCase` every 60 s. The use case finds all `HELD` bookings where `payment_deadline < NOW()`, cancels them, and calls `releaseHeldSeats()`. Additionally, `findAvailableByRouteId` in the train module treats `HELD` seats with expired deadlines as `AVAILABLE` (lazy expiry in query).

**Why**: Spring `@Scheduled` is already available in Spring Boot with zero extra dependencies. The 60 s lag is acceptable — the 15-minute window is generous. Lazy expiry in the availability query prevents users from seeing stale holds between scheduler runs.

**Alternative considered**: `pg_cron` database job. Rejected — adds operational complexity (requires PostgreSQL extension, not available in all environments) and moves business logic into SQL.

### Decision 6 — `RouteSeatAvailabilityPort` gains `findSeatIdsByBookingId`

The port currently has no query method. Cancellation requires knowing which seats belong to a booking.

**Chosen**: Add `List<SeatId> findSeatIdsByBookingId(BookingId bookingId)` to `RouteSeatAvailabilityPort`. The adapter implements it via a simple JPQL query on `route_seat_availability WHERE booking_id = ?`.

### Decision 7 — Module boundary: `booking` depends on `train::port` and `train::model`

`booking` calls `RouteSeatAvailabilityPort` (in `train::port`) and uses `RouteId`, `SeatId` (in `train::model`). The `@ApplicationModule` annotation on `booking/package-info.java` declares `allowedDependencies = {"train::port", "train::model", "user::model", "shared"}`.

## Risks / Trade-offs

- **Optimistic lock retry storms**: Under very high concurrency (flash sale scenario), many requests may fail with `OptimisticLockException` simultaneously. Mitigation: `GlobalExceptionHandler` maps `OptimisticLockException` → `409 Conflict` with a clear error code; clients are expected to retry. For now this is acceptable; a queue-based booking flow can be added later if needed.

- **Scheduler precision**: Expired holds are released up to 60 s late. Mitigation: The lazy expiry check in `findAvailableByRouteId` ensures no user sees a stale hold as unavailable. The 60 s lag only affects cleanup, not user-facing availability.

- **`idx_one_active_hold_per_user_route` constraint**: The DB enforces at most one `HELD` booking per `(user_id, route_id)`. If a user tries to create a second hold before the first expires, the DB will throw a unique constraint violation. Mitigation: `CreateBookingUseCase` checks for an existing `HELD` booking for the same `(userId, routeId)` before attempting to create, returning `BookingError.ActiveHoldExists` with `409 Conflict`.

- **`@Version` migration on existing data**: The `version` column already exists with `DEFAULT 1`, so all existing rows have `version = 1`. No data migration needed. The `RouteSeatAvailabilityEntity` change is backward-compatible.

- **`BookingCancelled` with `requiresRefund=true` has no consumer yet**: The payment module does not exist. The event will be published but unhandled. Mitigation: Spring Modulith logs unhandled events; this is expected and documented. The event contract is stable for the payment module to consume later.

## Migration Plan

1. Deploy train module changes (`@Version` on entity, remove `SELECT FOR UPDATE`) — backward-compatible, no schema change
2. Deploy booking module — new endpoints, new scheduler
3. No Flyway migration needed — all columns exist in baseline
4. Rollback: revert both modules; no data loss risk since booking module is new

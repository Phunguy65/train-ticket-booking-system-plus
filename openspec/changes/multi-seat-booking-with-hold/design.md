# Context

The booking module currently supports only single-seat, zero-priced bookings with a single `PENDING → CONFIRMED → CANCELLED` lifecycle. The `bookings` table has a direct `seat_id` foreign key, making multi-seat bookings structurally impossible. Price is hardcoded to `BigDecimal.ZERO` despite `routes.base_price` and `seats.seat_class` already existing in the schema.

This change introduces:
1. A two-phase flow: **hold** (seats reserved, price locked) → **confirm** (after payment)
2. Pessimistic locking during hold creation to prevent race conditions across concurrent requests
3. Price snapshotting per seat at hold time via a new `booking_seats` join table
4. A scheduled expiry job that releases stale holds after 15 minutes

The train module owns `RouteSeatAvailability` and its `AVAILABLE / BOOKED / CANCELLED` status FSM. The booking module calls into the train module through the `RouteSeatAvailabilityPort` port interface — this cross-module boundary must be preserved and extended.

## Goals / Non-Goals

**Goals:**

- Allow a single booking to hold 1–N seats atomically (all-or-nothing)
- Prevent double-booking via pessimistic database locks during hold creation
- Snapshot the per-seat price at hold creation time so price changes do not affect in-flight holds
- Enforce: a user may not create a new hold while an active hold exists for the same route
- Auto-expire holds after 15 minutes and release seats back to `AVAILABLE`
- Preserve idempotency on hold creation

**Non-Goals:**

- Frontend changes (API surface is new endpoints; existing endpoints unchanged)
- Per-user seat count limits (deferred)
- Redis-based real-time expiry (scheduled DB poll is sufficient for v1)
- Payment processing integration (confirmation endpoint accepts a `paymentReference` string; actual payment is out of scope)
- Discount / bulk-pricing logic

## Decisions

### D1 — Pessimistic locking over optimistic locking for hold creation

**Decision**: Use `LockModeType.PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`) when fetching `RouteSeatAvailability` rows during hold creation.

**Rationale**: Optimistic locking retries on `OptimisticLockException`, which works well for single-row updates. For multi-seat holds, a partial retry after some seats have already been marked `HELD` creates a leak window where those seats stay locked but no booking is saved. Pessimistic locking acquires all row locks before any mutation, so a failure causes the entire transaction to roll back with no leaked state.

**Deadlock prevention**: Seat IDs MUST be sorted in ascending UUID order before the `FOR UPDATE` query. This canonical ordering ensures all concurrent transactions acquire locks in the same sequence, eliminating the circular-wait condition.

**Lock timeout**: Set `javax.persistence.lock.timeout = 3000` ms. Requests that cannot acquire locks within 3 seconds receive `409 Conflict` with a retry hint, avoiding indefinite blocking.

**Alternative considered**: Optimistic lock + full rollback on partial failure. Rejected because rollback of already-mutated `RouteSeatAvailability` rows within the same transaction requires explicit reversal logic, which is error-prone and redundant when pessimistic locking prevents the problem entirely.

---

### D2 — `booking_seats` join table; drop `bookings.seat_id`

**Decision**: Replace the `bookings.seat_id` single-FK column with a `booking_seats` join table (`booking_id`, `seat_id`, `price_at_booking`, `seat_class_at_booking`). The old column is dropped entirely.

**Rationale**: The existing 1:1 column is a structural blocker for multi-seat bookings. A join table is the standard relational pattern for a 1:N relationship and naturally supports price snapshotting per row.

**Snapshot fields in `booking_seats`**: Storing `price_at_booking` and `seat_class_at_booking` at the row level means the price shown to the user at hold time can never drift, even if `routes.base_price` or a seat's class is updated by an admin between hold creation and confirmation.

**Alternative considered**: Keep `seat_id` as nullable for backward compat. Rejected: nullable columns create ambiguity ("is NULL because it's multi-seat, or because of a bug?") and require conditional logic in every mapper and query. A clean migration is preferable.

---

### D3 — `HELD` status added to `RouteSeatAvailabilityStatus` and `BookingStatus`

**Decision**:
- `RouteSeatAvailabilityStatus`: `AVAILABLE → HELD → BOOKED → CANCELLED → AVAILABLE`
- `BookingStatus`: `HELD → CONFIRMED → CANCELLED`

`PENDING` is removed; the hold IS the initial state. Confirmation only succeeds from `HELD`.

**Rationale**: Seats must be visually unavailable to other users between hold creation and confirmation. Without a `HELD` status on seat availability, there is no way to distinguish "reserved but not paid" from "paid and confirmed."

**Expiry transitions**:
- Seat: `HELD → AVAILABLE` via `RouteSeatAvailability.expire()`
- Booking: `HELD → CANCELLED` via `Booking.expire()`

**Alternative considered**: Reuse `BOOKED` for the hold phase and rely only on `BookingStatus.PENDING` to distinguish. Rejected: conflates seat states that have different business semantics and makes "how many seats are genuinely confirmed" queries ambiguous.

---

### D4 — One active hold per user per route (partial unique index)

**Decision**: `CREATE UNIQUE INDEX idx_one_active_hold_per_user_route ON bookings(user_id, route_id) WHERE status = 'HELD'`

**Rationale**: Users booking round trips need to hold seats on two different routes simultaneously (outbound + return). Restricting globally to one hold per user would break this use case. Restricting per-route is the minimal constraint that prevents duplicate holds on the same trip while allowing legitimate multi-route reservations.

Application-level pre-check is performed before hold creation for a fast, user-friendly error message. The DB partial index acts as the authoritative race-condition guard; `DataIntegrityViolationException` is caught and mapped to `409 Conflict`.

---

### D5 — Scheduled expiry job (no Redis)

**Decision**: A Spring `@Scheduled(fixedDelay = 60_000)` job polls `bookings WHERE status = 'HELD' AND payment_deadline < NOW()` and releases expired holds in batches of 100.

**Rationale**: Adds no new infrastructure dependencies. A 1-minute polling interval means maximum over-hold of ~1 minute beyond the 15-minute window — acceptable for a v1 train booking system. The confirmation endpoint performs a lazy expiry check (compares `NOW()` against `payment_deadline`) to reject stale holds even between job runs.

**Alternative considered**: Redis keyspace TTL notifications. Rejected for v1: requires Redis as a new dependency and significantly increases operational complexity. Revisit if near-real-time expiry becomes a product requirement.

---

### D6 — Price calculation: `route.basePrice × seatClass.multiplier`

**Decision**: Add `getPriceMultiplier(): BigDecimal` to the `SeatClass` enum (`ECONOMY=1.0`, `BUSINESS=1.5`, `FIRST_CLASS=2.0`). A new `PricingService` domain service computes `unitPrice = route.basePrice.multiply(seat.seatClass.getPriceMultiplier())`. `totalPrice` on the booking is `Σ(unitPrice)`.

Both `RouteRepository` and `SeatRepository` are already accessible to the booking module via the train module's named interfaces. No new cross-module dependency is introduced.

---

### D7 — New API endpoints; existing `GET /bookings/{id}` preserved

**Decision**:
- `POST /api/v1.0/bookings/hold` — create hold (new)
- `POST /api/v1.0/bookings/{id}/confirm` — confirm after payment (new)
- `DELETE /api/v1.0/bookings/{id}` — cancel hold or confirmed booking (new)
- `GET /api/v1.0/bookings/{id}` — retrieve booking (preserved, response extended with `seats[]` and `expiresAt`)

The old `POST /api/v1.0/bookings` (single-seat create) is removed; callers migrate to `POST /api/v1.0/bookings/hold` with `seatIds` as a list.

## Risks / Trade-offs

**[Risk] Hold window allows seat inventory to appear unavailable** → Users who hold seats but never confirm will block others for up to 15 minutes + scheduled job lag (~1 min). Mitigation: 15-minute deadline is industry standard; lazy check in confirm endpoint ensures no extension beyond deadline.

**[Risk] Scheduled job concurrent with hold confirm** → The expiry job and a user confirming their hold could run simultaneously. Mitigation: expiry job acquires a pessimistic write lock on the booking row before transitioning; the confirm use case does the same. Only one transaction can succeed; the other will find the status already changed and abort gracefully.

**[Risk] Migration irreversibility** → Dropping `bookings.seat_id` is irreversible without a full data restore. Mitigation: Flyway migration script must first copy existing `seat_id` values into `booking_seats` rows before dropping the column. Include rollback instructions in the migration PR description.

**[Risk] Lock contention on popular routes** → High-demand seat rows will see many `FOR UPDATE` waiters. Mitigation: 3-second lock timeout prevents pile-ups; clients receive `409 Conflict` quickly and can retry. This is acceptable given the transactional nature of seat reservation.

**[Risk] Partial test coverage for concurrency** → Unit tests cannot cover concurrent lock behavior. Mitigation: Add at least one `@DataJpaTest` integration test using `CompletableFuture` / `CountDownLatch` to assert that a second hold creation on the same route-seat pair receives `409` while the first transaction is in-flight.

## Migration Plan

1. **Database migration** (run before deploying new code):
   - `V5`: Add `HELD` to `route_seat_availability.status` CHECK constraint
   - `V6`: Create `booking_seats` table; migrate existing `bookings.seat_id` data into `booking_seats` rows with `price_at_booking = bookings.total_price` and `seat_class_at_booking = seats.seat_class`; then DROP `bookings.seat_id`
   - `V7`: Add partial unique index `idx_one_active_hold_per_user_route`
   - `V8`: Update `bookings.status` CHECK constraint to replace `PENDING` with `HELD`; update existing `PENDING` rows to `HELD`

2. **Deploy new backend**: All use cases, domain changes, port extensions, and scheduled job go live together.

3. **Rollback**: Revert application deployment to previous tag; run compensating SQL to restore `bookings.seat_id` from `booking_seats` data and re-add `PENDING` to status constraint. Document as part of migration PR.

## Open Questions

- Should `CANCELLED` bookings (user-initiated) be distinguishable from `EXPIRED` bookings (timeout)? Currently both use `CANCELLED`. A separate `EXPIRED` status would improve analytics but is not required for v1.
- Should `GET /api/v1.0/bookings` (list endpoint) be added in this change or deferred?

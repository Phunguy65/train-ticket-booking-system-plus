# Design: Seat Booking Workflow with Real-Time SSE Updates

## Context

The backend currently implements a complete booking flow:

- **Hold seats**: `CreateBookingUseCase` holds seats (AVAILABLE → HELD) with
  15-minute payment deadline
- **Payment**: `CreateCheckoutSessionUseCase` creates Stripe Checkout Session
  (30-min timeout)
- **Confirmation**: `HandlePaymentSuccessUseCase` confirms booking (HELD →
  CONFIRMED) and seats (HELD → BOOKED)
- **Expiry**: `BookingExpiryScheduler` (60s interval) releases expired held
  seats (HELD → AVAILABLE)
- **Cancellation**: `CancelBookingUseCase` releases or cancels booked seats

The frontend needs real-time seat status updates so users can see which seats
are available, held, or booked on a scheduled trip without manually refreshing
the page. This is a net-new capability — no SSE infrastructure exists today.

**Constraints:**

- Spring Boot backend (no WebFlux — use `SseEmitter` from `spring-web`)
- Vertical Slice Architecture per module
- No events between modules — use
  `@TransactionalEventListener(phase = AFTER_COMMIT)` for SSE broadcasts
- Optimistic locking via `@Version` already implemented on
  `RouteSeatAvailabilityEntity`
- SSE event format: bulk update (all changed seats in one event)
- Auth: JWT `@AuthenticationPrincipal`, any authenticated user can subscribe

## Goals / Non-Goals

**Goals:**

- SSE endpoint `GET /sse/trips/{scheduledTripId}/seats` for real-time seat
  status
- Seat status broadcasting after DB commit (AFTER_COMMIT)
- Full seat map query (not just available seats) for SSE initial state
- Broadcast on all seat status transitions: HELD, BOOKED, AVAILABLE

**Non-Goals:**

- No WebSocket — use Spring SSE (`SseEmitter`) only
- No per-seat SSE events — bulk format only
- No heartbeat — only change events
- No admin-only SSE — any authenticated user can subscribe
- No frontend implementation (this is backend-only change)
- No SSE for booking status changes (only seat status)

## Decisions

### 1. New `seat` module structure

**Decision**: Create a new top-level `seat` module under
`backend/src/main/java/io/github/phunguy65/ttbs/backend/seat/` with the SSE
infrastructure.

**Rationale**: The SSE infrastructure is a self-contained capability. The
`train` module owns seat data, but the SSE subscription/broadcast logic is a
distinct responsibility. The Vertical Slice Architecture favors putting each
capability in its own module.

**Alternatives considered:**

- Put SSE in `train` module → violates SRP, train module already has seat
  management
- Put SSE in `booking` module → booking owns the booking aggregate, not the seat
  availability aggregate
- Put SSE in `shared` → SSE is too domain-specific for a shared module

### 2. `@TransactionalEventListener(phase = AFTER_COMMIT)` for broadcasting

**Decision**: SSE broadcasts are triggered via Spring's
`@TransactionalEventListener` listening to `SeatStatusChangedEvent` domain
events, with phase set to `AFTER_COMMIT`.

**Rationale**: Ensures clients only see seat states that have been successfully
committed to the database. If a transaction rolls back, no SSE event is sent.
This prevents clients from seeing "ghost" holds that never persisted.

**Alternatives considered:**

- Broadcast inside `@Transactional` method → client may see event before DB
  commit (race condition)
- Broadcast in `@Transactional.afterCommit()` callback → works but less
  idiomatic than `@TransactionalEventListener`
- Async queue (Redis, Kafka) → overkill for this use case; adds operational
  complexity

### 3. `ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>` for subscriptions

**Decision**: Store SSE emitters in a thread-safe map, keyed by
`scheduledTripId`.

**Rationale**: `CopyOnWriteArrayList` is ideal for read-heavy workloads (many
subscribers, infrequent modifications). `ConcurrentHashMap` provides atomic
put-if-absent semantics. Since each emitter is short-lived per HTTP request,
memory is not a concern.

**Alternatives considered:**

- `Collections.synchronizedMap` with `ArrayList` → every operation acquires
  lock, worse contention
- External pub/sub (Redis) → adds infrastructure dependency; SSE is local to
  this app
- Database-backed subscription tracking → adds DB overhead for every event

### 4. `SseEmitter` timeout = 0 (no server-side timeout)

**Decision**: Create `SseEmitter` with no timeout. The client controls when to
disconnect.

**Rationale**: Browser SSE clients handle their own reconnection logic.
Server-side timeouts create spurious disconnects. Spring's default timeout is 30
minutes which is acceptable but 0 (infinite) is simpler and matches the
client-reconnect error handling strategy.

### 5. SSE event type name: `seat-changed`

**Decision**: SSE events use type name `seat-changed`. Initial state on connect
uses type `seat-initial`.

**Rationale**: Simple, clear naming. `seat-changed` indicates a state
transition; `seat-initial` conveys the full snapshot on subscription.

### 6. Broadcast on all seat status transitions (HELD, BOOKED, AVAILABLE)

**Decision**: SSE broadcasts are triggered on ALL seat status changes across all
use cases:

- `CreateBookingUseCase` → seats transition AVAILABLE → HELD → emit
  `SeatStatusChangedEvent` with status=HELD
- `HandlePaymentSuccessUseCase` → seats transition HELD → BOOKED → emit
  `SeatStatusChangedEvent` with status=BOOKED
- `ExpireHeldBookingsUseCase` → seats transition HELD → AVAILABLE → emit
  `SeatStatusChangedEvent` with status=AVAILABLE
- `CancelBookingUseCase` → seats transition HELD → AVAILABLE or BOOKED →
  CANCELLED → emit `SeatStatusChangedEvent` with status=AVAILABLE

**Rationale**: Users need to see all seat state changes, not just their own.
When someone else holds a seat, all subscribers of that trip should see it
become HELD so they know it's no longer available.

### 7. Repository enhancement: `findAllByScheduledTripId`

**Decision**: Add `findAllByScheduledTripId(ScheduledTripId)` to
`RouteSeatAvailabilityRepository` (no status filter — returns ALL seats:
AVAILABLE + HELD + BOOKED + CANCELLED).

**Rationale**: SSE initial state needs the full seat map. The existing
`findAvailableByScheduledTripId` only returns AVAILABLE seats (and lazy-expired
HELD seats), which is insufficient for showing the complete seat map to the
client.

**Implementation**: New JPQL query in `RouteSeatAvailabilityJpaRepository`
ordered by `seatId ASC` (consistent ordering prevents UI flicker).

### 8. Keep Stripe session timeout at 30 minutes

**Decision**: No change to `StripeGatewayAdapter.SESSION_EXPIRY_SECONDS = 1800L`
(30 minutes).

**Rationale**: The current 30-minute value is already at Stripe's minimum. The
15-minute booking hold is enforced server-side via `paymentDeadline` on the
Booking aggregate. The two timeouts serve different purposes: Stripe's session
timeout prevents indefinite open sessions; the booking hold deadline triggers
automatic expiry on the server.

## Risks / Trade-offs

### Risk: SSE emitter memory leaks

**[Risk]**: `SseEmitter` objects are stored in memory. If clients disconnect
abnormally (network drop, browser crash), the emitter stays in the map until the
HTTP connection times out.

**[Mitigation]**: Use `SseEmitter.onCompletion()` and `SseEmitter.onTimeout()`
callbacks to automatically remove dead emitters from the subscription map. This
ensures cleanup even on abnormal disconnects.

### Risk: Stale emitter references

**[Risk]**: Spring MVC's `SseEmitter.send()` can throw an
`IOException: "Broken pipe"` if the underlying HTTP connection is closed but the
callback hasn't fired yet.

**[Mitigation]**: Wrap `emitter.send()` in try-catch in
`SeatEventBroadcaster.broadcast()`. On any exception (including broken pipe),
remove the emitter from the subscription map and log at DEBUG level (not ERROR —
this is expected behavior for disconnected clients).

### Risk: High-volume trip with many concurrent subscribers

**[Risk]**: A popular scheduled trip could have hundreds of concurrent SSE
connections. Each event broadcast iterates over all subscribers.

**[Mitigation]**: `CopyOnWriteArrayList` is optimized for read-heavy workloads
(subscribers outnumber broadcasts). For very high traffic, consider moving to
Redis pub/sub in the future — the `SeatEventBroadcaster` interface can be
adapted without changing use cases.

### Risk: Transaction committed but SSE broadcast fails

**[Risk]**: If `SeatEventBroadcaster.broadcast()` throws an exception inside
`@TransactionalEventListener(phase = AFTER_COMMIT)`, the transaction has already
committed — seat state is correct but SSE event is lost.

**[Mitigation]**: `SeatEventBroadcaster.broadcast()` is designed to be
fire-and-forget. All `send()` calls are wrapped in try-catch. Exceptions are
logged at WARN level but do not propagate — this is intentional since SSE is
best-effort, not a critical path.

### Risk: Client reconnects and sees stale state

**[Risk]**: If a client disconnects and reconnects, they receive the initial
state snapshot (from `findAllByScheduledTripId`). However, between their
disconnect and reconnect, other clients may have changed seat states — they miss
those events.

**[Mitigation]**: Document in client integration guide that clients should:

1. On SSE reconnect, call the REST endpoint
   `GET /v1/scheduled-trips/{id}/seats/available` to refresh full state
2. Maintain local state and reconcile with SSE events
3. Handle connection drops with exponential backoff reconnection

This is a client-side concern — the server provides all necessary data via
initial state + events.

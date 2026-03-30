# Tasks

## 1. Repository Enhancement

- [x] 1.1 Add `findAllByScheduledTripId` method to
      `RouteSeatAvailabilityRepository` domain interface
- [x] 1.2 Add JPQL query to `RouteSeatAvailabilityJpaRepository` (ordered by
      seatId ASC)
- [x] 1.3 Implement `findAllByScheduledTripId` in
      `RouteSeatAvailabilityRepositoryAdapter`

## 2. SSE Infrastructure Module

- [x] 2.1 Create `seat` module package structure
      (`backend/src/main/java/.../seat/`)
- [x] 2.2 Create `SeatStatusChangedEvent` domain event record in
      `seat/domain/event/`
- [x] 2.3 Create `SeatEventBroadcaster` singleton service in `seat/application/`
- [x] 2.4 Create `SeatEventListener` with
      `@TransactionalEventListener(phase = AFTER_COMMIT)` in `seat/application/`

## 3. SSE Controller

- [x] 3.1 Create `SeatEventController` with
      `GET /sse/trips/{scheduledTripId}/seats` endpoint
- [x] 3.2 Implement emitter subscription with cleanup callbacks (`onCompletion`,
      `onTimeout`, `onError`)
- [x] 3.3 Send initial `seat-initial` event on connect (using
      `findAllByScheduledTripId`)
- [x] 3.4 Add JWT authentication via `@AuthenticationPrincipal`

## 4. SSE Integration — Hold Seats

- [x] 4.1 Emit `SeatStatusChangedEvent` from `CreateBookingUseCase` after
      successful `holdSeats()` and booking save
- [x] 4.2 Include all held seats (seatId, HELD status, bookingId) in the event
      payload

## 5. SSE Integration — Payment Success

- [x] 5.1 Emit `SeatStatusChangedEvent` from `HandlePaymentSuccessUseCase` after
      successful `confirmHeldSeats()`
- [x] 5.2 Include all confirmed seats (seatId, BOOKED status, bookingId) in the
      event payload

## 6. SSE Integration — Expiry

- [x] 6.1 Emit `SeatStatusChangedEvent` from `ExpireHeldBookingsUseCase` after
      successful `releaseHeldSeats()`
- [x] 6.2 Include all released seats (seatId, AVAILABLE status, bookingId: null)
      in the event payload

## 7. SSE Integration — Cancellation

- [x] 7.1 Emit `SeatStatusChangedEvent` from `CancelBookingUseCase` after
      successful seat release/cancel
- [x] 7.2 Include all affected seats (seatId, AVAILABLE status, bookingId: null)
      in the event payload

## 8. Unit Tests

- [x] 8.1 Add unit test for `SeatEventBroadcaster` (subscribe, unsubscribe,
      broadcast, dead emitter cleanup)
- [x] 8.2 Add unit test for `SeatEventListener` (verifies AFTER_COMMIT phase)
- [x] 8.3 Add unit test for `SeatStatusChangedEvent` record creation

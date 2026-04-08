# ADDED Requirements

## Requirement: Authenticated users can subscribe to real-time seat status updates

The system SHALL provide a Server-Sent Events (SSE) endpoint at
`GET /sse/trips/{scheduledTripId}/seats` that allows any authenticated user
(valid JWT) to subscribe to real-time seat status updates for a specific
scheduled trip.

The endpoint MUST:

-  Return an `SseEmitter` that remains open until the client disconnects or the
  connection times out
-  Send an initial `seat-initial` event containing all seats for the scheduled
  trip (AVAILABLE, HELD, BOOKED, CANCELLED)
-  Send subsequent `seat-changed` events whenever any seat status changes for
  that trip
-  Support multiple concurrent subscribers per `scheduledTripId`

### Scenario: Client connects to SSE endpoint

-  **WHEN** a client with a valid JWT calls `GET /sse/trips/{tripId}/seats`
-  **THEN** the server responds with HTTP 200 and
  `Content-Type: text/event-stream`
-  **AND** the server immediately sends a `seat-initial` event with all seats for
  that scheduled trip
-  **AND** the SSE connection remains open for subsequent event delivery

### Scenario: Client disconnects

-  **WHEN** the SSE client disconnects (browser close, network drop, or timeout)
-  **THEN** the server removes the `SseEmitter` from the subscription map
-  **AND** no further events are sent to that emitter

### Scenario: Unauthenticated request is rejected

-  **WHEN** a request without a valid JWT calls `GET /sse/trips/{tripId}/seats`
-  **THEN** the server responds with HTTP 401 Unauthorized

---

## Requirement: SSE broadcasts seat status changes after transaction commit

After any seat status transition (AVAILABLE → HELD, HELD → BOOKED, HELD →
AVAILABLE, BOOKED → CANCELLED) is successfully committed to the database, the
system SHALL broadcast a `SeatStatusChangedEvent` to all subscribed SSE clients
for that `scheduledTripId`.

The event SHALL be broadcast via
`@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure clients only see
committed state.

### Scenario: Hold seats triggers SSE broadcast

-  **WHEN** `CreateBookingUseCase` successfully holds seats and the transaction
  commits
-  **THEN** a `seat-changed` event is sent to all SSE subscribers of that
  `scheduledTripId`
-  **AND** the event payload contains each held seat's `seatId`, `status: HELD`,
  and `bookingId`

### Scenario: Confirm seats triggers SSE broadcast

-  **WHEN** `HandlePaymentSuccessUseCase` successfully confirms held seats and
  the transaction commits
-  **THEN** a `seat-changed` event is sent to all SSE subscribers of that
  `scheduledTripId`
-  **AND** the event payload contains each confirmed seat's `seatId`,
  `status: BOOKED`, and `bookingId`

### Scenario: Expire held seats triggers SSE broadcast

-  **WHEN** `ExpireHeldBookingsUseCase` successfully releases expired held seats
  and the transaction commits
-  **THEN** a `seat-changed` event is sent to all SSE subscribers of that
  `scheduledTripId`
-  **AND** the event payload contains each released seat's `seatId`,
  `status: AVAILABLE`, and `bookingId: null`

### Scenario: Cancel booking triggers SSE broadcast

-  **WHEN** `CancelBookingUseCase` successfully releases or cancels booked seats
  and the transaction commits
-  **THEN** a `seat-changed` event is sent to all SSE subscribers of that
  `scheduledTripId`
-  **AND** the event payload contains each affected seat's `seatId`,
  `status: AVAILABLE`, and `bookingId: null`

---

## Requirement: SSE event format is bulk update

All SSE events (both `seat-initial` and `seat-changed`) SHALL use a bulk update
format containing all changed seats in a single event payload.

The event format SHALL be:

```json
{
    "scheduledTripId": "uuid",
    "seats": [
        {
            "seatId": "uuid",
            "status": "AVAILABLE|HELD|BOOKED|CANCELLED",
            "bookingId": "uuid|null"
        }
    ],
    "timestamp": "ISO-8601 instant"
}
```

### Scenario: Multiple seats change in one transaction

-  **WHEN** a client holds 3 seats in a single booking
-  **THEN** one `seat-changed` event is sent with all 3 seats in the `seats`
  array
-  **AND NOT** 3 separate single-seat events

### Scenario: Bulk release of expired seats

-  **WHEN** the expiry scheduler releases 10 expired held seats
-  **THEN** one `seat-changed` event is sent with all 10 seats in the `seats`
  array

---

## Requirement: SSE subscriber management is thread-safe

The system SHALL manage SSE subscriptions using a thread-safe data structure
(`ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>`).

### Scenario: Concurrent subscribe/unsubscribe

-  **WHEN** multiple clients subscribe and unsubscribe concurrently on the same
  `scheduledTripId`
-  **THEN** no race conditions occur in the subscription map
-  **AND** each subscriber receives all events for their subscribed
  `scheduledTripId`

### Scenario: Dead emitter cleanup

-  **WHEN** an `SseEmitter.send()` throws an exception (broken pipe, client
  disconnected)
-  **THEN** the dead emitter is removed from the subscription map
-  **AND** subsequent broadcasts do not iterate over the dead emitter

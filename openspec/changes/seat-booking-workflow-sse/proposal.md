# Proposal: Seat Booking Workflow with Real-Time SSE Updates

## Why

The current booking system lacks real-time seat availability visibility for
users. When a user is selecting seats, they cannot see which seats are being
held or booked by others in real-time, leading to potential booking conflicts
and poor user experience. Additionally, the system needs to ensure proper
payment timeout handling with Stripe session expiration aligned with hold
duration requirements.

## What Changes

- **SSE Endpoint**: New `GET /sse/trips/{scheduledTripId}/seats` endpoint for
  real-time seat status updates
- **Seat Event System**: New module for broadcasting seat status changes to all
  subscribed clients
- **Bulk Event Format**: SSE events contain all changed seats in one payload
  (not per-seat events)
- **After-Commit Broadcasting**: SSE broadcasts via
  `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure clients see
  committed state only
- **Stripe Session Timeout**: Keep existing 30-minute expiration (already
  correct per Stripe minimum)
- **Auth**: JWT-authenticated users (any logged-in user can subscribe to see
  seat availability)

## Capabilities

### New Capabilities

- `seat-event-subscription`: Real-time SSE subscription for seat status changes
  per scheduled trip, allowing authenticated users to see all seat states
  (AVAILABLE, HELD, BOOKED) and receive updates when seats are held, booked, or
  released
- `seat-availability-query`: Query all seats (not just available) for a
  scheduled trip to support SSE initial state and full seat map display

### Modified Capabilities

_(none — this is a net-new capability)_

## Impact

**New Components:**

- `seat` module: new package structure for SSE infrastructure
- `SeatEventBroadcaster`: singleton service managing SSE emitter subscriptions
  per scheduled trip
- `SeatEventListener`: `@TransactionalEventListener` for after-commit
  broadcasting
- `SeatEventController`: REST controller for SSE endpoint
- `SeatStatusChangedEvent`: domain event record for seat status changes

**Modified Components:**

- `RouteSeatAvailabilityRepository`: add `findAllByScheduledTripId` for full
  seat map query
- `CreateBookingUseCase`: emit `SeatStatusChangedEvent` after hold seats
- `HandlePaymentSuccessUseCase`: emit `SeatStatusChangedEvent` after confirm
  seats
- `ExpireHeldBookingsUseCase`: emit `SeatStatusChangedEvent` after release seats
- `CancelBookingUseCase`: emit `SeatStatusChangedEvent` after release/cancel
  seats
- `StripeGatewayAdapter`: confirm 30-minute session expiration is maintained

**Infrastructure:**

- New SSE endpoint: `GET /sse/trips/{scheduledTripId}/seats`
- SseEmitter timeout: 0 (client-controlled, no server-side timeout)
- Concurrent subscription map:
  `ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>`

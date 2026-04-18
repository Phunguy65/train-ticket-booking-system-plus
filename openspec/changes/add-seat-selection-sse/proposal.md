# Why

The seat-selection page currently relies on a static seat-map query, so
customers can act on stale availability while other bookings are changing in
real time. Adding authenticated SSE updates now lets the frontend reflect
backend seat-state changes immediately and reduces avoidable booking conflicts.

## What Changes

- Update the customer seat-selection experience to subscribe to the backend seat
  SSE stream after the initial seat map loads.
- Add a frontend hook that connects with Bearer-token authentication, parses
  `seat-initial` and `seat-changed` events, merges seat updates into the current
  seat map, reconnects with exponential backoff, and cleans up on unmount.
- Update the seat-selection UI to apply live seat-status changes, surface
  localized connection-state feedback, and keep the existing query as the
  initial data source.
- Add TypeScript event types and unit tests for the SSE hook.

## Capabilities

### Modified Capabilities

- `customer-seat-booking-flow`: seat selection requirements now include
  authenticated real-time seat-status synchronization and connection-state
  feedback.
- `i18n`: seat-selection translations now include localized SSE
  connection-status messages.

## Impact

- Affected frontend code in
  `frontend/customer/src/components/seats/seat-selection.tsx` and the new
  `frontend/customer/src/lib/hooks/use-seat-sse.ts` hook.
- Affected locale message catalogs in `frontend/customer/src/messages/en.json`
  and `frontend/customer/src/messages/vi.json`.
- Adds unit-test coverage for SSE stream parsing, merge behavior, reconnect
  backoff, and cleanup.
- Depends on the existing backend SSE endpoint at
  `GET /sse/trips/{scheduledTripId}/seats` and current frontend authentication
  token access.

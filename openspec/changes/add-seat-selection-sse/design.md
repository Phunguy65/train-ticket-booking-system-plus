# Context

The customer seat-selection page currently hydrates from `getCoachSeatMap` via
React Query and renders one or more coach tabs with seat status styling and
booking-summary interactions. The backend already exposes an authenticated SSE
stream at `/sse/trips/{scheduledTripId}/seats` that sends a full `seat-initial`
payload on connect and `seat-changed` delta payloads afterward, but the frontend
does not yet consume that stream.

This change is frontend-focused, but it crosses data fetching, auth-aware
networking, UI state reconciliation, testing, and localization. Because native
`EventSource` cannot attach Authorization headers, the frontend must consume the
stream through `fetch` and manually parse the SSE protocol from a
`ReadableStream`.

## Goals / Non-Goals

**Goals:**

- Add a reusable `useSeatSSE` hook that opens an authenticated SSE connection
  with `fetch`.
- Parse `seat-initial` and `seat-changed` events into typed payloads and expose
  live seat updates plus connection state.
- Merge incoming seat updates into the seat-selection page without replacing the
  initial React Query fetch flow.
- Reconnect automatically with exponential backoff and cleanly stop all work on
  unmount.
- Add localized connection-status copy and unit tests for the hook's core
  behavior.

**Non-Goals:**

- Changing backend SSE payload shape, endpoint behavior, or auth requirements.
- Replacing the initial `getCoachSeatMap` query with SSE-only loading.
- Adding polling fallback, offline behavior, or cross-page real-time
  infrastructure.

## Decisions

### 1. Keep React Query as the source of initial hydration and layer SSE updates on top

The page will continue using `getCoachSeatMap` to fetch the first complete seat
map, then apply SSE events as incremental updates in client state. This
preserves existing loading and error behavior while avoiding a blank UI during
stream connection establishment.

- **Why this approach:** it minimizes regression risk, keeps current trip/route
  fetching logic intact, and uses the backend's `seat-initial` event as a
  consistency refresh rather than a required first render dependency.
- **Alternative considered:** drive the seat map entirely from the SSE stream
  after mount. Rejected because it would delay first usable render, complicate
  error handling, and make the page depend on successful stream establishment
  for basic functionality.

### 2. Implement SSE transport with `fetch` + `ReadableStream` parsing inside a dedicated hook

The hook will create a `fetch` request to `/sse/trips/{scheduledTripId}/seats`
with the Bearer token in the `Authorization` header, consume the response body
stream with `TextDecoder`, split SSE frames by blank lines, and parse `event:`
and `data:` fields into typed events.

- **Why this approach:** it satisfies the Authorization-header requirement while
  isolating low-level stream parsing away from UI components.
- **Alternative considered:** `EventSource`. Rejected because it cannot meet the
  auth-header requirement in the browser environment.

### 3. Model live updates as seat-level merges against the existing coach seat map

The page will maintain a derived seat-map state that starts from the query
result and updates seat records by `seatId` as SSE events arrive. `seat-initial`
will refresh all matching seats in the current map, while `seat-changed` will
patch only the referenced seats. Selected seats that become non-selectable
should be removed from the local selection set to prevent continuing with stale
selections.

- **Why this approach:** the backend event payload is seat-centric, and
  seat-level merging avoids unnecessary full-page rerenders or assumptions about
  coach ordering.
- **Alternative considered:** refetch the whole seat map whenever a delta
  arrives. Rejected because it defeats the real-time benefit and adds avoidable
  API load.

### 4. Expose a simple connection-state model for UI feedback and reconnect behavior

The hook will expose a connection status enum/string such as `connecting`,
`connected`, and `reconnecting`, plus any derived metadata needed by the page.
On stream failure or premature close, the hook will retry with exponential
backoff starting at 1 second, doubling to a 30-second cap, and reset the delay
after a stable reconnection.

- **Why this approach:** the page only needs lightweight, user-facing status
  feedback, while the hook owns retry timing and lifecycle concerns.
- **Alternative considered:** keep retry state entirely internal and render no
  UI status. Rejected because explicit connection feedback is a stated
  requirement and helps users understand transient sync delays.

### 5. Test the hook by mocking `fetch`, stream chunks, timers, and unmount cleanup

Unit tests will validate parsing of `seat-initial` and `seat-changed`, merge
callbacks/state transitions, exponential backoff progression, and
cancellation/cleanup when the component unmounts.

- **Why this approach:** the most failure-prone logic is in stream parsing and
  reconnect timing, so targeted hook tests provide the highest confidence.
- **Alternative considered:** cover the behavior only through component tests.
  Rejected because lower-level stream timing is harder to assert and maintain
  from the page boundary.

## Risks / Trade-offs

- **[Risk] Auth token access for browser-side SSE may differ from existing query
  auth handling** → **Mitigation:** align the hook with the current frontend
  auth/session utility used for authenticated API access, and fail into
  reconnecting/error-aware states without breaking the initial seat-map render.
- **[Risk] Manual SSE parsing can mishandle chunk boundaries or multi-line
  payloads** → **Mitigation:** centralize parser logic in the hook, process
  buffered frames only after complete separators, and cover chunk-splitting
  behavior in unit tests.
- **[Risk] Real-time seat changes can invalidate a user's selected seats
  mid-flow** → **Mitigation:** reconcile selected seats against merged
  availability updates and remove seats that are no longer selectable before
  booking continuation.
- **[Risk] Reconnect loops can create noisy network behavior during outages** →
  **Mitigation:** use capped exponential backoff, abort prior requests during
  retries/unmount, and keep the static seat-map UI usable without fallback
  polling.

## Migration Plan

1. Add typed SSE event definitions and the `useSeatSSE` hook behind the existing
   seat-selection page.
2. Integrate the hook into `seat-selection.tsx` so the page hydrates from React
   Query and then applies live seat updates and connection-state messaging.
3. Add English and Vietnamese translation keys for connection states.
4. Add unit tests covering parsing, reconnect timing, merge behavior, and
   cleanup.
5. Rollback, if needed, by removing the hook integration and translation keys;
   the page will continue functioning with the existing static query flow.

## Open Questions

- The change assumes the frontend already has an established way to obtain the
  current Bearer token in client components; implementation should follow the
  project’s existing auth/session pattern discovered during coding.
- The status indicator wording is expected to cover at least `connected` and
  `reconnecting`; implementation can include an internal `connecting` state if
  useful, provided the user-facing copy remains localized and unobtrusive.

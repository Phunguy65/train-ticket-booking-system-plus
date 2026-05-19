# MODIFIED Requirements

## Requirement: Coach navigation and seat map status rendering

The system SHALL use `getCoachSeatMap` to render coach tabs and a visual seat
grid whose seats expose availability status, then keep the rendered seat state
synchronized with the authenticated seat SSE stream for the active scheduled
trip.

### Scenario: Switch between coaches

- **WHEN** the seat-map response contains multiple coaches and the user selects
  a different coach tab
- **THEN** the system renders the seat grid for the selected coach

### Scenario: Render seat statuses visually and accessibly

- **WHEN** the seat grid is displayed
- **THEN** each seat communicates whether it is available, held, or booked using
  visible status styling and screen-reader-readable state

### Scenario: Non-available seats cannot be selected

- **WHEN** the user attempts to activate a held or booked seat
- **THEN** the system prevents selection of that seat

### Scenario: Apply initial seat snapshot from SSE

- **WHEN** the authenticated SSE connection emits a `seat-initial` event for the
  current scheduled trip
- **THEN** the system merges the streamed seat snapshot into the current seat
  map without discarding the existing page state

### Scenario: Apply seat delta updates in real time

- **WHEN** the authenticated SSE connection emits a `seat-changed` event for the
  current scheduled trip
- **THEN** the system updates only the referenced seats in the rendered seat map
  so availability changes appear without a manual refetch

### Scenario: Remove invalid selected seats after live updates

- **WHEN** a live seat update changes a currently selected seat to a non-
  selectable status
- **THEN** the system removes that seat from the current selection summary
  before the user continues to booking

# ADDED Requirements

## Requirement: Seat-selection real-time connection feedback

The system SHALL expose localized connection-state feedback on the seat-
selection page while consuming the authenticated SSE seat stream.

### Scenario: Show connected status

- **WHEN** the seat SSE stream is connected successfully
- **THEN** the seat-selection page displays a localized indicator that live seat
  updates are connected

### Scenario: Show reconnecting status during retry

- **WHEN** the seat SSE stream disconnects or fails after initial connection
- **THEN** the seat-selection page displays a localized reconnecting indicator
  while retry attempts are scheduled

### Scenario: Reconnect with exponential backoff

- **WHEN** the seat SSE stream disconnects repeatedly
- **THEN** the frontend retries with exponential backoff intervals of 1 second,
  2 seconds, 4 seconds, and doubling thereafter up to a maximum 30-second delay

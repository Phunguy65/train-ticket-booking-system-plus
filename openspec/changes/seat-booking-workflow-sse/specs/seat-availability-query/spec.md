# ADDED Requirements

## Requirement: Repository can query all seats for a scheduled trip

The system SHALL provide a repository method
`findAllByScheduledTripId(ScheduledTripId)` that returns all
`RouteSeatAvailability` records for a given scheduled trip, regardless of status
(AVAILABLE, HELD, BOOKED, CANCELLED).

This query is required to support:

-  SSE initial state on connect (full seat map)
-  Full seat map display for trip overview UI

### Scenario: Query returns all seats including held and booked

-  **WHEN** `findAllByScheduledTripId(tripId)` is called
-  **THEN** the result contains ALL `RouteSeatAvailability` records for that
  `scheduledTripId`
-  **AND** records are ordered by `seatId` ascending (consistent ordering)

### Scenario: Query returns empty list for trip with no seats

-  **WHEN** `findAllByScheduledTripId(unusedTripId)` is called for a trip with no
  seat availability records
-  **THEN** the result is an empty list

---

## Requirement: SSE initial state includes all seat statuses

When a client connects to the SSE endpoint, the server SHALL send an initial
`seat-initial` event containing the current status of ALL seats for the
`scheduledTripId`, not just available seats.

The initial state event SHALL include:

-  All seats with status AVAILABLE, HELD, BOOKED, or CANCELLED
-  `bookingId` populated for HELD and BOOKED seats
-  `bookingId: null` for AVAILABLE and CANCELLED seats
-  Timestamp of when the state was captured

### Scenario: Initial state includes held seats

-  **WHEN** a client connects to SSE for a scheduled trip where 2 seats are
  currently HELD
-  **THEN** the `seat-initial` event includes those 2 HELD seats with their
  `bookingId`
-  **AND** includes all other seats in their current state (AVAILABLE or BOOKED)

### Scenario: Initial state reflects current committed state

-  **WHEN** a client connects to SSE at time T0
-  **AND** another client's booking committed at T0 (same millisecond)
-  **THEN** the `seat-initial` event reflects the committed state at T0
-  **AND** the subsequent `seat-changed` event will reflect the change that
  committed at T0

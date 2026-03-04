# ADDED Requirements

## Requirement: Bulk soft delete emits a single aggregate event

When a bulk soft delete use case (`BulkSoftDeleteStationsUseCase`, `BulkSoftDeleteRoutesUseCase`, `BulkSoftDeleteTrainsUseCase`, `BulkSoftDeleteCoachesUseCase`, `BulkSoftDeleteSeatsUseCase`) successfully deletes one or more records, the system SHALL publish exactly one bulk domain event (`StationsDeleted`, `RoutesDeleted`, `TrainsDeleted`, `CoachesDeleted`, or `SeatsDeleted` respectively) containing the full list of deleted IDs, instead of emitting one individual event per record.

### Scenario: Bulk delete of multiple stations emits one event

- **WHEN** `BulkSoftDeleteStationsUseCase` is executed with a list of N station IDs
- **THEN** exactly one `StationsDeleted` event is published to the outbox containing all N station IDs

### Scenario: Bulk delete of zero affected records emits no event

- **WHEN** `BulkSoftDeleteStationsUseCase` is executed but `softDeleteByIds` affects 0 rows (all already deleted)
- **THEN** no bulk event is published

### Scenario: Single-entity delete use cases are unaffected

- **WHEN** `SoftDeleteStationUseCase` is executed for a single station
- **THEN** the existing `StationDeleted` (single-entity) event is published as before

---

## Requirement: Cascade soft delete propagates from Station down the hierarchy

When a Station is soft-deleted (single or bulk), the system SHALL eventually soft-delete all active Routes that reference that station as origin or destination, and subsequently cascade further down the hierarchy.

### Scenario: Deleting a station cascades to its routes

- **WHEN** a `StationDeleted` or `StationsDeleted` event is processed by the cascade listener
- **THEN** all active Routes with `originStationId` or `destinationStationId` matching the deleted station(s) are soft-deleted
- **THEN** a `RoutesDeleted` event is published containing the IDs of all newly soft-deleted routes

### Scenario: Deleting a station with no active routes produces no route cascade

- **WHEN** a `StationsDeleted` event is processed and no active routes reference the deleted stations
- **THEN** no routes are soft-deleted and no `RoutesDeleted` event is published

---

## Requirement: Cascade soft delete propagates from Route to orphaned Trains

When Routes are cascade-deleted, the system SHALL soft-delete any Train that has no remaining active Routes after the deletion.

### Scenario: Train becomes orphaned after route cascade and is soft-deleted

- **WHEN** a `RoutesDeleted` event is processed
- **THEN** for each Train referenced by the deleted routes, the system checks if any active routes still reference that train
- **THEN** Trains with zero remaining active routes are soft-deleted
- **THEN** a `TrainsDeleted` event is published for the orphaned trains

### Scenario: Shared train is not deleted when it still has active routes

- **WHEN** a `RoutesDeleted` event is processed and a referenced Train still has at least one active Route not in the deleted set
- **THEN** that Train is NOT soft-deleted

---

## Requirement: Cascade soft delete propagates from Train to Coaches and Seats

When Trains are cascade-deleted, the system SHALL soft-delete all active Coaches belonging to those trains, and subsequently all active Seats belonging to those coaches.

### Scenario: Deleting trains cascades to coaches

- **WHEN** a `TrainsDeleted` event is processed
- **THEN** all active Coaches with `trainId` in the deleted train IDs are soft-deleted
- **THEN** a `CoachesDeleted` event is published

### Scenario: Deleting coaches cascades to seats

- **WHEN** a `CoachesDeleted` event is processed
- **THEN** all active Seats with `coachId` in the deleted coach IDs are soft-deleted
- **THEN** a `SeatsDeleted` event is published

---

## Requirement: RouteSeatAvailability records are hard-deleted when their Route or Seat is deleted

The system SHALL hard-delete `RouteSeatAvailability` rows when the associated Route or Seat is soft-deleted, to prevent orphaned availability data.

### Scenario: RSA rows are hard-deleted when routes are cascade-deleted

- **WHEN** a `RoutesDeleted` event is processed by the cascade listener
- **THEN** all `RouteSeatAvailability` rows with `routeId` in the deleted route IDs are hard-deleted from the database

### Scenario: RSA rows are hard-deleted when seats are cascade-deleted

- **WHEN** a `CoachesDeleted` event is processed and seats are about to be soft-deleted
- **THEN** all `RouteSeatAvailability` rows with `seatId` in the affected seat IDs are hard-deleted before the seats are soft-deleted

### Scenario: RSA hard delete is idempotent on retry

- **WHEN** a cascade listener is retried by the Spring Modulith outbox after a transient failure
- **THEN** the hard delete of already-deleted RSA rows completes without error

---

## Requirement: Cascade operations use bulk queries, not per-record queries

All cascade listeners SHALL fetch child entity IDs and perform soft deletes using bulk `IN`-clause queries, not per-record loops.

### Scenario: Cascade from routes to trains uses a single query to find train IDs

- **WHEN** `CascadeOnRoutesDeletedListener` processes a `RoutesDeleted` event with N route IDs
- **THEN** train IDs are retrieved with a single `SELECT DISTINCT trainId FROM routes WHERE id IN (...)` query
- **THEN** the soft delete is performed with a single `UPDATE trains SET deleted_at = ? WHERE id IN (...)` query

### Scenario: Cascade from coaches to seats uses a single query to find seat IDs

- **WHEN** `CascadeOnCoachesDeletedListener` processes a `CoachesDeleted` event with N coach IDs
- **THEN** seat IDs are retrieved with a single `SELECT id FROM seats WHERE coachId IN (...)` query
- **THEN** the soft delete is performed with a single `UPDATE seats SET deleted_at = ? WHERE id IN (...)` query

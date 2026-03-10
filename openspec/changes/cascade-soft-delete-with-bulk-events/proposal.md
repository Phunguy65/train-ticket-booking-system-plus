# Why

The system currently blocks soft delete on Station, Route, Train, and Coach when child entities exist, preventing any top-down deletion. There is no cascade logic, and bulk delete operations emit one domain event per record — causing N+1 event writes to the Spring Modulith outbox table. As data grows, deleting a Station with hundreds of associated routes, trains, coaches, and seats becomes impossible without manual bottom-up cleanup, and any bulk operation creates significant outbox bloat.

## What Changes

- Remove "in-use" guards from `BulkSoftDelete*UseCase` for Station, Route, Train, and Coach — replace with cascade behavior
- Add bulk domain events (`StationsDeleted`, `RoutesDeleted`, `TrainsDeleted`, `CoachesDeleted`, `SeatsDeleted`) to replace per-record event loops
- Add event-driven cascade listeners in the `train` module that react to bulk delete events and propagate soft deletes down the hierarchy: Station → Route → Train → Coach → Seat
- Add hard delete of `RouteSeatAvailability` records as part of cascade (triggered on `RoutesDeleted` and `CoachesDeleted`)
- Add missing repository query methods needed to fetch child IDs in bulk for cascade operations

## Capabilities

### New Capabilities

- `cascade-soft-delete`: Event-driven cascade soft delete across the Station → Route → Train → Coach → Seat hierarchy, with hard delete of orphaned `RouteSeatAvailability` records and optimized bulk event emission

### Modified Capabilities

<!-- No existing spec-level requirements are changing — this is new behavior layered on top of existing soft delete infrastructure -->

## Impact

- **station module**: `BulkSoftDeleteStationsUseCase`, `SoftDeleteStationUseCase` — guard logic removed, bulk event emitted
- **train module**: All `BulkSoftDelete*UseCase` files (Route, Train, Coach, Seat) — for-loop event emission replaced with single bulk event; new cascade listeners added
- **Repositories**: `RouteRepository`, `CoachRepository`, `SeatRepository`, `RouteSeatAvailabilityRepository` — new bulk query and hard delete methods
- **Domain events**: 5 new bulk event records added alongside existing single-entity events
- **Spring Modulith outbox**: Significantly fewer rows per bulk operation (5 events instead of N)
- **No API changes**: REST endpoints and HTTP contracts unchanged
- **No breaking changes** to existing single-entity delete flows

# Why

The backend API for Train, Seat, and Station entities currently only supports create, read, and update operations. There are no delete endpoints, which means admin users cannot decommission trains, remove invalid seats, or retire stations through the API. The database schema already has `deleted_at` columns on all three tables (with supporting partial indexes), but the application layer has never been wired up. The User module already demonstrates the full soft-delete pattern — this change applies that same pattern to the three remaining master-data entities.

## What Changes

- Add `DELETE /api/{v}/trains/{id}` — single soft-delete for a Train, blocked if active Routes reference it
- Add `DELETE /api/{v}/trains` — bulk soft-delete for multiple Trains (atomic: all-or-none), blocked if any Train in the list has active Routes
- Add `DELETE /api/{v}/seats/{id}` — single soft-delete for a Seat, blocked if any `route_seat_availability` record for that Seat is `HELD` or `BOOKED`
- Add `DELETE /api/{v}/seats` — bulk soft-delete for multiple Seats (atomic), blocked if any Seat in the list is currently active in availability
- Add `DELETE /api/{v}/stations/{id}` — single soft-delete for a Station, blocked if active Routes reference it (as origin or destination)
- Add `DELETE /api/{v}/stations` — bulk soft-delete for multiple Stations (atomic), blocked if any Station in the list has active Routes
- Expose a new `RouteValidationPort` named-interface from the `train` module so the `station` module can check route usage without a circular dependency
- Add `deletedAt` field to `TrainEntity`, `SeatEntity`, and `StationEntity` (the DB column already exists)
- Add `softDelete()` / `isDeleted()` to `Train`, `Seat`, and `Station` domain models
- Add `TrainDeleted`, `SeatDeleted`, `StationDeleted` domain events
- Add new error cases: `TrainError.TrainNotFound`, `TrainError.TrainInUse`, `SeatError.SeatInUse`, `StationError.StationInUse`
- Add `TRAIN_NOT_FOUND`, `TRAIN_IN_USE`, `SEAT_IN_USE`, `STATION_IN_USE` to the shared `ErrorCode` enum
- All delete endpoints require `ADMIN` role (consistent with create/update on these entities)

## Capabilities

### New Capabilities

- `train-delete`: Soft-delete single and bulk Trains with active-route guard
- `seat-delete`: Soft-delete single and bulk Seats with active-availability guard
- `station-delete`: Soft-delete single and bulk Stations with active-route guard

### Modified Capabilities

- `backend-api`: New DELETE endpoints added to Train, Seat, and Station REST API surface

## Impact

**Backend modules affected:**
- `train` module — Train aggregate, Seat aggregate, Route domain (new validation port), persistence layer, web layer
- `station` module — Station aggregate, persistence layer, web layer; new dependency on `train::validation` named-interface
- `shared` module — `ErrorCode` enum extended with new codes

**Database:** No schema migration needed — `deleted_at` columns and partial indexes already exist on `trains`, `seats`, and `stations` tables.

**API contract change:** New DELETE endpoints added; existing endpoints unchanged. No breaking changes.

**Authorization:** All 6 new endpoints require `ROLE_ADMIN`. Security config needs corresponding `requestMatchers` entries for DELETE on `/api/*/trains`, `/api/*/seats`, `/api/*/stations`.

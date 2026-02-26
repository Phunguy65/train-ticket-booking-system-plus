# Why

The backend currently supports creating and reading Train, Seat, Route, and Station resources but lacks PATCH (partial update) endpoints. Admin operators need the ability to update individual fields of these resources without re-submitting the full payload — for example, changing a train's name, updating a route's departure time, or correcting a station's city — without affecting other fields. This is a missing CRUD capability that blocks admin workflows.

## What Changes

- Add `PATCH /api/v1.0/trains/{id}` endpoint (ADMIN only) — allows partial update of `trainNumber`, `name`, `totalSeats`
- Add `PATCH /api/v1.0/seats/{id}` endpoint (ADMIN only) — allows partial update of `seatNumber`, `seatClass`
- Add `PATCH /api/v1.0/routes/{id}` endpoint (ADMIN only) — allows partial update of `departureTime`, `arrivalTime`, `basePrice`, `status`
- Add `PATCH /api/v1.0/stations/{id}` endpoint (ADMIN only) — allows partial update of `code`, `name`, `city`
- Each endpoint uses `JsonNullable<T>` fields to distinguish between "field omitted" and "field explicitly nulled"
- Unique constraint validation on update (e.g., `trainNumber`, `stationCode`, `seatNumber` within a train)
- All updates follow the existing `UpdateUserUseCase` pattern: fetch → validate → reconstitute → save

## Capabilities

### New Capabilities

- `train-patch-update`: Partial update endpoint for the Train resource — update `trainNumber`, `name`, and/or `totalSeats` via PATCH with admin authorization and uniqueness validation
- `seat-patch-update`: Partial update endpoint for the Seat resource — update `seatNumber` and/or `seatClass` via PATCH with admin authorization and uniqueness validation within a train
- `route-patch-update`: Partial update endpoint for the Route resource — update `departureTime`, `arrivalTime`, `basePrice`, and/or `status` via PATCH with admin authorization
- `station-patch-update`: Partial update endpoint for the Station resource — update `code`, `name`, and/or `city` via PATCH with admin authorization and uniqueness validation

### Modified Capabilities

- `backend-api`: Four new PATCH endpoints added to the REST API surface for Admin operations

## Impact

- **Backend modules affected**: `train` module (Train, Seat, Route), `station` module (Station)
- **New files per resource**: `Update{Resource}HttpRequest.java`, `Update{Resource}Command.java`, `Update{Resource}UseCase.java`, mapper method, controller endpoint
- **No database schema changes** — all updatable fields already exist in the database
- **No breaking changes** — existing GET/POST endpoints are unchanged
- **No new dependencies** — `JsonNullable` infrastructure already configured
- **Auth**: All PATCH endpoints require `ADMIN` role (consistent with POST endpoints)

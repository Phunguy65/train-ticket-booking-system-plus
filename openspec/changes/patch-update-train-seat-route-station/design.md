## Context

The backend currently implements Create and Read operations for Train, Seat, Route, and Station resources but is missing PATCH (partial update) support. A reference implementation already exists in the `user` module (`UpdateUserUseCase`, `UpdateUserHttpRequest`) which uses the `JsonNullable<T>` pattern from `jackson-databind-nullable` to distinguish between "field omitted" and "field explicitly set". This design replicates that exact pattern across four additional resources spanning two Spring Modulith modules: `train` (owns Train, Seat, Route) and `station` (owns Station).

Current state:
- `train` module: `CreateTrainUseCase`, `CreateSeatUseCase`, `CreateRouteUseCase` + their Read counterparts exist
- `station` module: `CreateStationUseCase` + Read counterpart exists
- `user` module: `UpdateUserUseCase` — the reference implementation to follow
- No PATCH endpoints for any of the four target resources

## Goals / Non-Goals

**Goals:**
- Add `PATCH /api/v1.0/trains/{id}` for partial update of Train
- Add `PATCH /api/v1.0/seats/{id}` for partial update of Seat
- Add `PATCH /api/v1.0/routes/{id}` for partial update of Route
- Add `PATCH /api/v1.0/stations/{id}` for partial update of Station
- Reuse `JsonNullable<T>` for three-state field semantics (undefined / value / null)
- Enforce unique constraints on update (trainNumber, seatNumber per train, stationCode)
- Return the updated resource (200 OK + JSend success wrapper) on success
- Require ADMIN role for all PATCH endpoints

**Non-Goals:**
- No database schema migrations (all columns already exist)
- No frontend changes in this change
- No PATCH endpoint for Booking (out of scope)
- No bulk/batch update endpoints
- No route for changing `trainId`, `originStationId`, or `destinationStationId` on Route (immutable business keys)

## Decisions

### Decision 1: Follow `UpdateUserUseCase` pattern exactly

**Chosen**: Replicate the `JsonNullable` + `reconstitute()` pattern from the User module.

**Rationale**: The pattern is already established, tested, and understood by the team. Diverging would introduce inconsistency across the codebase. `JsonNullable` correctly handles the three-state problem (undefined vs null vs value), which plain `Optional<T>` or nullable fields cannot.

**Alternatives considered**:
- **Plain `Optional<T>` fields**: Cannot distinguish `Optional.empty()` (not sent) from `null` (explicitly cleared). Rejected.
- **PUT (full replacement)**: Forces clients to re-send all fields. Rejected as it breaks admin UX for partial edits.
- **JSON Merge Patch (`application/merge-patch+json`)**: Would require a custom deserializer and content-type negotiation. The existing `JsonNullable` approach achieves the same semantics with less complexity. Rejected.

### Decision 2: Standalone `/api/v1.0/seats/{id}` URL for PATCH

**Chosen**: `PATCH /api/v1.0/seats/{id}` (not nested under `trains/{trainId}/seats/{id}`).

**Rationale**: The seat's `trainId` is an immutable field (marked `updatable = false` in JPA). Since the train cannot change on update, nesting under a train ID adds verbosity without meaning. The User module also uses non-nested IDs. Consistent with how other standalone updates work.

**Alternatives considered**:
- `PATCH /api/v1.0/trains/{trainId}/seats/{id}`: Verbose, trainId redundant for update. Rejected.

### Decision 3: One `Update{Resource}UseCase` per resource (no god service)

**Chosen**: Four separate use case classes, mirroring the one-use-case-per-operation pattern.

**Rationale**: ArchUnit rules enforce no god-service classes. Spring Modulith module boundaries are maintained. Each use case is independently testable and has a single reason to change.

### Decision 4: Return 200 OK with updated resource body

**Chosen**: Return `ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto)))`.

**Rationale**: Consistent with how `UpdateUserUseCase` result is returned. Clients get the authoritative updated state immediately without a follow-up GET.

**Alternatives considered**:
- `204 No Content`: Saves bandwidth but forces client to re-fetch. Rejected for consistency.

### Decision 5: Validate unique constraints at use case layer, not domain layer

**Chosen**: Uniqueness checks happen inside each `Update{Resource}UseCase` before reconstituting.

**Rationale**: Uniqueness requires a repository query, which is an infrastructure concern. Domain model stays pure (no repository deps). Same approach used in `UpdateUserUseCase` for email uniqueness.

## Risks / Trade-offs

- **[Risk] TOCTOU on unique constraint check** → The `existsBy*()` check and `save()` are not atomic. A concurrent request could pass both checks and cause a DB unique constraint violation. Mitigation: `GlobalExceptionHandler` already handles `DataIntegrityViolationException` and maps it to a 409; additionally JPA optimistic locking (`@Version`) may be leveraged if needed.
- **[Risk] `totalSeats` reduction on Train** → Decreasing `totalSeats` below the number of existing seats is currently not validated at the application layer. Mitigation: Add a guard in `UpdateTrainUseCase` to reject updates where `newTotalSeats < existingSeatCount`; or defer as a follow-up.
- **[Risk] Route `status` transitions** → Updating `status` to `CANCELLED` or `COMPLETED` may have downstream effects on existing bookings. Mitigation: For now, status update is unrestricted (admin privilege); a booking-aware validation can be added in a subsequent change.
- **[Trade-off] `reconstitute()` loads full entity** → Fetching the entity before update is less efficient than a direct `UPDATE` query, but ensures domain invariants are applied and is consistent with the existing pattern.

## Migration Plan

- No database migrations required.
- Deploy as a backward-compatible addition — existing endpoints unchanged.
- Rollback: remove the new endpoints; no data changes to reverse.

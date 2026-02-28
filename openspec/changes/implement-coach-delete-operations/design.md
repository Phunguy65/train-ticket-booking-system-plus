# Context

The train module's Coach aggregate supports create and read operations, but has no delete endpoints. Admins managing train configurations need to remove invalid or retired coaches without resorting to direct database access. The codebase already implements soft-delete with referential integrity checks for Train, Seat, and Station resources — Coach needs to follow the same pattern.

All existing delete operations in the project use `@RequestBody` for bulk IDs. For this change, bulk delete will instead accept IDs via **query parameters** (`?ids=uuid1&ids=uuid2`), which is a conscious departure from the request-body convention and is documented below.

## Goals / Non-Goals

**Goals:**

- Implement `DELETE /api/1.0/trains/{trainId}/coaches/{id}` — soft-delete a single coach
- Implement `DELETE /api/1.0/coaches?ids=uuid1&ids=uuid2` — bulk soft-delete coaches atomically
- Enforce referential integrity: a coach with active seats must not be deleted
- Publish `CoachDeleted` domain event for every soft-deleted coach
- Return typed `CoachError.CoachInUse` with the full list of blocking coach IDs when deletion is rejected

**Non-Goals:**

- Hard (physical) deletion of coaches
- PATCH / update coach fields
- Cascade deletion of child Seat records
- Restoring (un-deleting) coaches

## Decisions

### Decision 1 — Soft Delete (not hard delete)

**Choice:** Set `deletedAt = NOW()` on the `coaches` row; never issue a `DELETE` SQL statement.

**Rationale:** Consistent with every other aggregate in the project (Train, Seat, Station, User). Preserves audit history and enables event-sourcing patterns downstream. The existing `Coach.softDelete()` domain method already implements this.

**Alternative considered:** Hard delete with CASCADE on seats. Rejected because it destroys audit history and would require additional migration work.

---

### Decision 2 — Fail-All semantics for bulk delete

**Choice:** Before deleting any coach, check every supplied ID for active seats. If _any_ ID has active seats, reject the entire request with `422 Unprocessable Entity` and return all conflicting IDs.

**Rationale:** Consistent with `BulkSoftDeleteTrainsUseCase`, `BulkSoftDeleteSeatsUseCase`, and `BulkSoftDeleteStationsUseCase`. Admin bulk-delete acts on a curated selection; partial deletion creates ambiguity and leaves the data in an unexpected state. Returning all conflicting IDs in one response lets the admin resolve all blockers at once.

**Alternative considered:** Succeed-partial (delete what's possible, skip coaches with seats). Rejected because it silently changes the caller's intent and is inconsistent with every other entity in the project.

---

### Decision 3 — Query params for bulk delete IDs (`?ids=uuid1&ids=uuid2`)

**Choice:** Accept coach IDs as repeated query parameters: `DELETE /api/1.0/coaches?ids={uuid}&ids={uuid}`.

**Rationale:** The frontend table's bulk-delete flow constructs the request URL dynamically; query params align with how the admin dashboard filter/select patterns already work. Spring MVC binds `@RequestParam("ids") List<UUID> ids` natively without a DTO wrapper.

**Alternatives considered:**
- `@RequestBody` JSON `{"coachIds": [...]}` — used by all other bulk deletes in this project; rejected here per explicit product decision to use query params for Coach.
- `POST /api/1.0/coaches/bulk-delete` — more HTTP-compliant but introduces a non-RESTful verb-in-path pattern; rejected to keep DELETE semantics explicit.

**Trade-off:** `@Valid` on `@RequestParam` lists requires manual size/null validation in the use case or controller; Bean Validation annotations cannot be applied to the list the same way as on a `@RequestBody` DTO. Validation will be done manually in the controller before delegating to the use case.

---

### Decision 4 — Referential integrity check: Coach → Seat

**Choice:** Before soft-deleting, call `seatRepository.findByCoachId(coachId)` and reject if the result is non-empty.

**Rationale:** `SeatRepository` already exposes `findByCoachId(CoachId)` which returns only active (non-soft-deleted) seats. This is the same pattern used by `SoftDeleteTrainUseCase` (checks `routeRepository.existsActiveByTrainId`) and `SoftDeleteSeatUseCase` (checks `availabilityRepository.existsActiveBySeatId`).

**Alternative considered:** Add a dedicated `existsActiveByCoachId(CoachId)` boolean query to avoid loading full Seat objects. Acceptable optimisation but deferred — `findByCoachId` is already available and the list is needed to populate `conflictingIds` anyway (indirectly via the coachId).

---

### Decision 5 — Bulk delete uses direct JPA batch UPDATE (no aggregate load)

**Choice:** `coachRepository.softDeleteByIds(List<CoachId>, Instant)` issues a single `UPDATE coaches SET deleted_at = ? WHERE id IN (?) AND deleted_at IS NULL`. Domain events are published manually afterwards.

**Rationale:** Loading every Coach aggregate just to call `softDelete()` and collect its event is expensive and unnecessary for a bulk operation. The `BulkSoftDeleteTrainsUseCase` uses the same direct-update pattern and publishes `TrainDeleted.of(id)` in a loop. `CoachDeleted` already exists and follows the same factory method convention.

**Alternative considered:** Load each aggregate, call `softDelete()`, save — provides domain-method symmetry with single delete. Rejected for bulk due to N+1 query cost.

## Risks / Trade-offs

- **Query param URL length limit** → Large batches (hundreds of UUIDs) could exceed browser/server URL limits (~8 KB). Mitigation: enforce a max of 100 IDs in the controller; log a warning if the limit is approached. If larger batches become necessary, the endpoint can be migrated to `@RequestBody` with minimal client changes.

- **Manual validation for `@RequestParam`** → Without a DTO, `@Size` / `@NotEmpty` annotations cannot be applied declaratively. Mitigation: add explicit guard clauses at the top of the controller method and return `400 Bad Request` with a JSend fail body before delegating to the use case.

- **Events fired for IDs that were already soft-deleted** → `softDeleteByIds` skips already-deleted rows (`AND deleted_at IS NULL`), so the affected count may be less than `ids.size()`. However, the event loop iterates all supplied IDs including non-matching ones. Mitigation: publish events only for the IDs where `deletedAt` was actually set — but since we don't load aggregates, the safest approach is to trust the JPA `@Modifying` count and document the edge case. This is consistent with the Train bulk-delete implementation.

# Why

The Coach resource is missing delete operations in the backend API. Currently, admins can create and view coaches but cannot remove them — making it impossible to retire invalid coaches or clean up train configurations without direct database intervention. This change adds the two missing write endpoints to complete the Coach resource lifecycle.

## What Changes

- Add `DELETE /api/1.0/trains/{trainId}/coaches/{id}` — soft-delete a single coach by ID
- Add `DELETE /api/1.0/coaches` with `?ids=uuid1&ids=uuid2` query params — bulk soft-delete multiple coaches atomically
- Add `CoachError.CoachInUse` domain error to represent referential integrity violations when a coach has active seats
- Add `COACH_IN_USE` to the `ErrorCode` enum
- Add `softDeleteByIds()` to `CoachRepository`, `CoachJpaRepository`, and `CoachRepositoryAdapter`
- Bulk delete uses **fail-all** semantics: if any coach in the batch has active seats, the entire operation is rejected with the list of conflicting IDs

## Capabilities

### New Capabilities

- `coach-delete`: Soft-delete a single coach and bulk soft-delete multiple coaches with atomic fail-all referential integrity checks against active seats

### Modified Capabilities

_(none — no existing spec-level requirements are changing)_

## Impact

- **Backend API**: Two new DELETE endpoints on the `train` module's Coach controller
- **Domain layer**: `CoachError` sealed interface gains `CoachInUse` record; `CoachRepository` port gains `softDeleteByIds()`
- **Persistence layer**: `CoachJpaRepository` gains a `@Modifying` JPQL bulk-update query; `CoachRepositoryAdapter` implements the new port method
- **Security**: Both endpoints require `ROLE_ADMIN` via `@PreAuthorize`
- **No breaking changes**: existing GET and POST endpoints are untouched

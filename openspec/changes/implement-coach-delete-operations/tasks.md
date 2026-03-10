# Tasks

## 1. Domain Layer — Error & Repository Port

- [x] 1.1 Add `CoachInUse(List<UUID> conflictingIds)` record to `CoachError.java` sealed interface with message "One or more coaches have active seats and cannot be deleted"
- [x] 1.2 Add `COACH_IN_USE` constant to `ErrorCode.java` enum
- [x] 1.3 Add `softDeleteByIds(List<CoachId> ids, Instant deletedAt)` method signature to `CoachRepository.java` (domain port interface)

## 2. Persistence Layer — JPA & Repository Adapter

- [x] 2.1 Add `@Modifying @Query("UPDATE CoachEntity c SET c.deletedAt = :deletedAt WHERE c.id IN :ids AND c.deletedAt IS NULL") int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt)` to `CoachJpaRepository.java`
- [x] 2.2 Implement `softDeleteByIds(List<CoachId> ids, Instant deletedAt)` in `CoachRepositoryAdapter.java`, mapping `List<CoachId>` → `List<UUID>` and delegating to the JPA repository

## 3. Application Layer — Single Delete Use Case

- [x] 3.1 Create `SoftDeleteCoachCommand.java` record with fields `CoachId coachId` and `TrainId trainId`
- [x] 3.2 Create `SoftDeleteCoachUseCase.java` annotated `@Service @Transactional`:
  - Fetch coach via `coachRepository.findById(coachId)` → return `CoachError.CoachNotFound` if absent
  - Validate `coach.getTrainId().equals(command.trainId())` → return `CoachError.CoachNotFound` if mismatch
  - Return success immediately if `coach.isDeleted()` (idempotency)
  - Call `seatRepository.findByCoachId(coachId)` → return `CoachError.CoachInUse(List.of(coachId.value()))` if non-empty
  - Call `coach.softDelete()`, then `coachRepository.save(coach)`
  - Publish all domain events via `eventPublisher.publishEvent(e)` then `coach.clearDomainEvents()`
  - Return `Result.success()`

## 4. Application Layer — Bulk Delete Use Case

- [x] 4.1 Create `BulkSoftDeleteCoachesCommand.java` record with field `List<CoachId> coachIds`
- [x] 4.2 Create `BulkSoftDeleteCoachesUseCase.java` annotated `@Service @Transactional`:
  - Collect all conflicting IDs: for each `coachId` in `command.coachIds()`, check `seatRepository.findByCoachId(coachId).isEmpty()`, accumulate those where seats are non-empty
  - If `conflictingIds` is not empty, return `Result.failure(new CoachError.CoachInUse(conflictingIds))`
  - Call `coachRepository.softDeleteByIds(command.coachIds(), Instant.now())` and capture `affected` count
  - Publish `CoachDeleted.of(coachId)` for each ID in `command.coachIds()` via `eventPublisher`
  - Return `Result.success(affected)`

## 5. Web Layer — Controller Endpoints

- [x] 5.1 Update `coachErrorResponse()` in `CoachController.java` to handle `CoachError.CoachInUse` → `422 UNPROCESSABLE_ENTITY` with error code `COACH_IN_USE`
- [x] 5.2 Inject `SoftDeleteCoachUseCase` and `BulkSoftDeleteCoachesUseCase` into `CoachController` constructor
- [x] 5.3 Add single delete endpoint to `CoachController.java`:
  ```
  @DeleteMapping(value = "/{version}/trains/{trainId}/coaches/{id}", version = "1.0")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID trainId, @PathVariable UUID id)
  ```
  Delegates to `softDeleteCoachUseCase.execute(new SoftDeleteCoachCommand(CoachId.of(id), TrainId.of(trainId)))`, returns `200 OK` on success
- [x] 5.4 Add bulk delete endpoint to `CoachController.java`:
  ```
  @DeleteMapping(value = "/{version}/coaches", version = "1.0")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<JsendResponse<?>> bulkDelete(@RequestParam("ids") List<UUID> ids)
  ```
  - Validate `ids` is not null/empty → return `400 Bad Request` with JSend fail if so
  - Validate `ids.size() <= 100` → return `400 Bad Request` with JSend fail if exceeded
  - Map to `List<CoachId>`, delegate to `bulkSoftDeleteCoachesUseCase.execute(...)`
  - Return `200 OK` with `{ "deletedCount": <n> }` on success

## 6. Tests — Unit Tests

- [x] 6.1 Write unit tests for `SoftDeleteCoachUseCase` (`@ExtendWith(MockitoExtension)`):
  - Happy path: coach exists, no seats → soft-deletes and publishes event
  - Idempotent: already deleted coach → returns success, no save/event
  - Not found: absent coach → returns `CoachNotFound`
  - Train mismatch: coach exists but different trainId → returns `CoachNotFound`
  - In use: coach has active seats → returns `CoachInUse`
- [x] 6.2 Write unit tests for `BulkSoftDeleteCoachesUseCase` (`@ExtendWith(MockitoExtension)`):
  - Happy path: all coaches have no seats → deletes all, publishes events, returns count
  - Fail-all: one coach has seats → returns `CoachInUse` with conflicting ID list, no deletion
  - All coaches have seats → returns `CoachInUse` with all IDs

## 7. Tests — Web Layer Tests

- [x] 7.1 Write `@WebMvcTest` tests for `DELETE /api/1.0/trains/{trainId}/coaches/{id}`:
  - 200 OK on successful delete
  - 200 OK on idempotent delete (already deleted)
  - 404 when coach not found
  - 422 when coach has active seats
  - 401 when unauthenticated
  - 403 when authenticated as non-admin
- [x] 7.2 Write `@WebMvcTest` tests for `DELETE /api/1.0/coaches?ids=...`:
  - 200 OK with `deletedCount` on success
  - 422 when any coach has active seats (fail-all), response includes `conflictingIds`
  - 400 when `ids` param is missing or empty
  - 400 when `ids` param exceeds 100 entries
  - 401 when unauthenticated
  - 403 when authenticated as non-admin

# Why

Creating coaches and seats one at a time is tedious when setting up a new train — a 20-coach train with 60 seats each means 1,220 sequential API calls. Bulk create endpoints allow admins to provision an entire train's coaches and seats in two requests.

## What Changes

- **New endpoint**: `POST /{version}/trains/{trainId}/coaches:bulkCreate` — creates multiple coaches for a train in one atomic operation
- **New endpoint**: `POST /{version}/coaches/{coachId}/seats:bulkCreate` — creates multiple seats for a coach in one atomic operation
- Both endpoints follow fail-all semantics: if any item fails validation or conflicts with existing data, the entire request is rejected and nothing is persisted
- `saveAll()` is added to `CoachRepository` and `SeatRepository` domain interfaces and their adapters for efficient batch inserts
- New error variants added to `CoachError` and `SeatError` sealed interfaces to represent bulk-specific conflicts
- Fix existing bug: `CreateSeatUseCase` currently returns `SeatError.TrainNotFound` when coach is missing — replace with new `SeatError.CoachNotFound` variant

## Capabilities

### New Capabilities

- `bulk-create-coaches`: Bulk creation of coaches under a train, with duplicate and conflict detection, fail-all semantics, max 100 items per request
- `bulk-create-seats`: Bulk creation of seats under a coach, with duplicate and conflict detection, fail-all semantics, max 100 items per request

### Modified Capabilities

*(none — no existing spec-level behavior changes)*

## Impact

**Backend files affected:**
- `domain/errors/CoachError.java` — add `CarNumbersAlreadyExist`, `DuplicateCarNumbersInRequest`
- `domain/errors/SeatError.java` — add `SeatNumbersAlreadyExist`, `DuplicateSeatNumbersInRequest`, `CoachNotFound`
- `domain/repository/CoachRepository.java` — add `saveAll()`
- `domain/repository/SeatRepository.java` — add `saveAll()`
- `infrastructure/persistence/CoachRepositoryAdapter.java` — implement `saveAll()`
- `infrastructure/persistence/SeatRepositoryAdapter.java` — implement `saveAll()`
- New: `application/command/BulkCreateCoachesCommand.java`
- New: `application/command/BulkCreateSeatsCommand.java`
- New: `application/usecase/BulkCreateCoachesUseCase.java`
- New: `application/usecase/BulkCreateSeatsUseCase.java`
- New: `infrastructure/web/BulkCreateCoachesHttpRequest.java`
- New: `infrastructure/web/BulkCreateSeatsHttpRequest.java`
- `infrastructure/web/CoachController.java` — add `bulkCreate` endpoint
- `infrastructure/web/SeatController.java` — add `bulkCreate` endpoint
- `infrastructure/web/CoachRequestMapper.java` — add bulk mapping
- `infrastructure/web/SeatRequestMapper.java` — add bulk mapping
- `shared/infrastructure/web/ErrorCode.java` — add 4 new error codes

**APIs:** Two new POST endpoints, admin-only (`hasRole('ADMIN')`)
**No breaking changes** to existing endpoints or response formats

# Tasks

## 1. Domain Layer — Errors & Repository Contracts

- [x] 1.1 Add `CarNumbersAlreadyExist(List<Integer> conflictingCarNumbers)` and `DuplicateCarNumbersInRequest(List<Integer> duplicates)` variants to `CoachError` sealed interface
- [x] 1.2 Add `SeatNumbersAlreadyExist(List<String> conflictingNumbers)`, `DuplicateSeatNumbersInRequest(List<String> duplicates)`, and `CoachNotFound()` variants to `SeatError` sealed interface
- [x] 1.3 Add `List<Coach> saveAll(List<Coach> coaches)` method to `CoachRepository` interface
- [x] 1.4 Add `List<Seat> saveAll(List<Seat> seats)` method to `SeatRepository` interface

## 2. Infrastructure Layer — Persistence Adapters

- [x] 2.1 Implement `saveAll()` in `CoachRepositoryAdapter` — map domain list to entities, delegate to `CoachJpaRepository.saveAll()`, map results back to domain
- [x] 2.2 Implement `saveAll()` in `SeatRepositoryAdapter` — map domain list to entities, delegate to `SeatJpaRepository.saveAll()`, map results back to domain

## 3. Application Layer — Bulk Create Coaches

- [x] 3.1 Create `BulkCreateCoachesCommand` record: `(UUID trainId, List<CoachItem> coaches)` with nested `CoachItem(int carNumber, int totalSeats)`
- [x] 3.2 Create `BulkCreateCoachesUseCase` with `@Transactional` — implement three-gate validation (train exists → in-request duplicates → DB conflicts via `findByTrainId()`) followed by `saveAll()`, return `Result<List<CoachDto>, CoachError>`

## 4. Application Layer — Bulk Create Seats

- [x] 4.1 Create `BulkCreateSeatsCommand` record: `(UUID coachId, List<SeatItem> seats)` with nested `SeatItem(String seatNumber)`
- [x] 4.2 Create `BulkCreateSeatsUseCase` with `@Transactional` — implement three-gate validation (coach exists → in-request duplicates → DB conflicts via `findByCoachId()`) followed by `saveAll()`, return `Result<List<SeatDto>, SeatError>`

## 5. Application Layer — Bug Fix

- [x] 5.1 Fix `CreateSeatUseCase`: replace `return Result.failure(new SeatError.TrainNotFound())` with `return Result.failure(new SeatError.CoachNotFound())` when coach lookup returns empty

## 6. Shared Infrastructure — Error Codes

- [x] 6.1 Add four new error codes to `ErrorCode` enum: `COACH_CAR_NUMBERS_ALREADY_EXIST`, `COACH_DUPLICATE_CAR_NUMBERS_IN_REQUEST`, `SEAT_NUMBERS_ALREADY_EXIST`, `SEAT_DUPLICATE_SEAT_NUMBERS_IN_REQUEST`
- [x] 6.2 Add `COACH_NOT_FOUND` error code for the seat module's coach-not-found case (if not already present under that name)

## 7. Infrastructure Layer — Web (Coaches)

- [x] 7.1 Create `BulkCreateCoachesHttpRequest` record with `@NotEmpty @Size(max=100) List<@Valid CoachItemRequest> coaches` and inner `CoachItemRequest(@Positive int carNumber, @Positive int totalSeats)`
- [x] 7.2 Add `toBulkCommand(UUID trainId, BulkCreateCoachesHttpRequest)` and `toResponseList(List<CoachDto>)` methods to `CoachRequestMapper`
- [x] 7.3 Add `bulkCreate` endpoint to `CoachController`: `POST /{version}/trains/{trainId}/coaches:bulkCreate`, `@PreAuthorize("hasRole('ADMIN')")`, wire `BulkCreateCoachesUseCase`, return HTTP 201 on success
- [x] 7.4 Extend `coachErrorResponse` switch in `CoachController` to handle `CarNumbersAlreadyExist` (409, include `conflictingCarNumbers` in body) and `DuplicateCarNumbersInRequest` (422, include `duplicates` in body)

## 8. Infrastructure Layer — Web (Seats)

- [x] 8.1 Create `BulkCreateSeatsHttpRequest` record with `@NotEmpty @Size(max=100) List<@Valid SeatItemRequest> seats` and inner `SeatItemRequest(@NotBlank @Size(max=10) String seatNumber)`
- [x] 8.2 Add `toBulkCommand(UUID coachId, BulkCreateSeatsHttpRequest)` and `toResponseList(List<SeatDto>)` methods to `SeatRequestMapper`
- [x] 8.3 Add `bulkCreate` endpoint to `SeatController`: `POST /{version}/coaches/{coachId}/seats:bulkCreate`, `@PreAuthorize("hasRole('ADMIN')")`, wire `BulkCreateSeatsUseCase`, return HTTP 201 on success
- [x] 8.4 Extend `seatErrorResponse` switch in `SeatController` to handle `CoachNotFound` (404), `SeatNumbersAlreadyExist` (409, include `conflictingNumbers`), and `DuplicateSeatNumbersInRequest` (422, include `duplicates`)

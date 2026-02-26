## 1. Train Patch Update — Application Layer

- [x] 1.1 Create `UpdateTrainCommand.java` record in `train/application/command/` with fields: `TrainId trainId`, `JsonNullable<String> trainNumber`, `JsonNullable<String> name`, `JsonNullable<Integer> totalSeats`
- [x] 1.2 Create `UpdateTrainUseCase.java` in `train/application/usecase/` annotated `@Service @Transactional`: fetch by `trainId`, return `TrainError.TrainNotFound` if absent; if `trainNumber` is present and differs from current, check `trainRepository.existsByTrainNumber()` and return `TrainError.TrainNumberAlreadyExists` if taken; apply `JsonNullable` fields with `.isPresent()` guards; call `Train.reconstitute()`; save; return `Result.success(toDto(saved))`

## 2. Train Patch Update — Web Layer

- [x] 2.1 Create `UpdateTrainHttpRequest.java` record in `train/infrastructure/web/` with `JsonNullable<String> trainNumber`, `JsonNullable<String> name`, `JsonNullable<Integer> totalSeats`; add validation annotations (`@NotBlank`, `@Size`, `@Positive`) matching `CreateTrainHttpRequest`; add default no-arg constructor returning all `JsonNullable.undefined()`
- [x] 2.2 Add `toUpdateCommand(UUID id, UpdateTrainHttpRequest request)` method to `TrainRequestMapper.java`
- [x] 2.3 Add `@PatchMapping(value = "/{id}", version = "1.0")` endpoint to `TrainController.java` with `@PreAuthorize("hasRole('ADMIN')")`, `@Valid @RequestBody UpdateTrainHttpRequest`, delegating to `updateTrainUseCase.execute(mapper.toUpdateCommand(id, request))` and using `.fold()` to return `200 OK` or error response
- [x] 2.4 Add `UpdateTrainUseCase` to the constructor injection in `TrainController.java`

## 3. Seat Patch Update — Application Layer

- [x] 3.1 Create `UpdateSeatCommand.java` record in `train/application/command/` with fields: `SeatId seatId`, `JsonNullable<String> seatNumber`, `JsonNullable<SeatClass> seatClass`
- [x] 3.2 Create `UpdateSeatUseCase.java` in `train/application/usecase/` annotated `@Service @Transactional`: fetch by `seatId`, return `SeatError.SeatNotFound` if absent; if `seatNumber` is present and differs from current, check `seatRepository.existsByTrainIdAndSeatNumber(seat.getTrainId(), newSeatNumber)` and return `SeatError.SeatNumberAlreadyExists` if taken; apply `JsonNullable` fields; call `Seat.reconstitute()`; save; return `Result.success(toDto(saved))`

## 4. Seat Patch Update — Web Layer

- [x] 4.1 Create `UpdateSeatHttpRequest.java` record in `train/infrastructure/web/` with `JsonNullable<String> seatNumber`, `JsonNullable<SeatClass> seatClass`; add appropriate validation annotations; add default no-arg constructor returning all `JsonNullable.undefined()`
- [x] 4.2 Add `toUpdateCommand(UUID id, UpdateSeatHttpRequest request)` method to `SeatRequestMapper.java`
- [x] 4.3 Add `@PatchMapping(value = "/{id}", version = "1.0")` endpoint to `SeatController.java` with `@PreAuthorize("hasRole('ADMIN')")`, delegating to `updateSeatUseCase`, returning `200 OK` or error response via `.fold()`
- [x] 4.4 Add `UpdateSeatUseCase` to the constructor injection in `SeatController.java`

## 5. Route Patch Update — Application Layer

- [x] 5.1 Create `UpdateRouteCommand.java` record in `train/application/command/` with fields: `RouteId routeId`, `JsonNullable<Instant> departureTime`, `JsonNullable<Instant> arrivalTime`, `JsonNullable<BigDecimal> basePrice`, `JsonNullable<RouteStatus> status`
- [x] 5.2 Create `UpdateRouteUseCase.java` in `train/application/usecase/` annotated `@Service @Transactional`: fetch by `routeId`, return `RouteError.RouteNotFound` if absent; apply `JsonNullable` fields (no unique constraint checks needed); call `Route.reconstitute()`; save; return `Result.success(toDto(saved))`

## 6. Route Patch Update — Web Layer

- [x] 6.1 Create `UpdateRouteHttpRequest.java` record in `train/infrastructure/web/` with `JsonNullable<Instant> departureTime`, `JsonNullable<Instant> arrivalTime`, `JsonNullable<BigDecimal> basePrice`, `JsonNullable<RouteStatus> status`; add validation annotations (`@PositiveOrZero` on basePrice); add default no-arg constructor returning all `JsonNullable.undefined()`
- [x] 6.2 Add `toUpdateCommand(UUID id, UpdateRouteHttpRequest request)` method to `RouteRequestMapper.java`
- [x] 6.3 Add `@PatchMapping(value = "/{id}", version = "1.0")` endpoint to `RouteController.java` with `@PreAuthorize("hasRole('ADMIN')")`, delegating to `updateRouteUseCase`, returning `200 OK` or error response via `.fold()`
- [x] 6.4 Add `UpdateRouteUseCase` to the constructor injection in `RouteController.java`

## 7. Station Patch Update — Application Layer

- [x] 7.1 Create `UpdateStationCommand.java` record in `station/application/command/` with fields: `StationId stationId`, `JsonNullable<String> code`, `JsonNullable<String> name`, `JsonNullable<String> city`
- [x] 7.2 Create `UpdateStationUseCase.java` in `station/application/usecase/` annotated `@Service @Transactional`: fetch by `stationId`, return `StationError.StationNotFound` if absent; if `code` is present and differs from current, check `stationRepository.existsByCode()` and return `StationError.StationCodeAlreadyExists` if taken; apply `JsonNullable` fields; call `Station.reconstitute()`; save; return `Result.success(toDto(saved))`

## 8. Station Patch Update — Web Layer

- [x] 8.1 Create `UpdateStationHttpRequest.java` record in `station/infrastructure/web/` with `JsonNullable<String> code`, `JsonNullable<String> name`, `JsonNullable<String> city`; add validation annotations (`@NotBlank`, `@Size`) matching `CreateStationHttpRequest`; add default no-arg constructor returning all `JsonNullable.undefined()`
- [x] 8.2 Add `toUpdateCommand(UUID id, UpdateStationHttpRequest request)` method to `StationRequestMapper.java`
- [x] 8.3 Add `@PatchMapping(value = "/{id}", version = "1.0")` endpoint to `StationController.java` with `@PreAuthorize("hasRole('ADMIN')")`, delegating to `updateStationUseCase`, returning `200 OK` or error response via `.fold()`
- [x] 8.4 Add `UpdateStationUseCase` to the constructor injection in `StationController.java`

## 9. Repository — Uniqueness Query Methods

- [x] 9.1 Add `existsByTrainNumber(String trainNumber)` to `TrainRepository` port interface and `TrainRepositoryAdapter` / `TrainJpaRepository` if not already present
- [x] 9.2 Add `existsByTrainIdAndSeatNumber(UUID trainId, String seatNumber)` to `SeatRepository` port interface and `SeatRepositoryAdapter` / `SeatJpaRepository` if not already present
- [x] 9.3 Add `existsByCode(String code)` to `StationRepository` port interface and `StationRepositoryAdapter` / `StationJpaRepository` if not already present

## 10. Tests

- [x] 10.1 Write `UpdateTrainUseCaseTest.java` in `train/application/usecase/` with Mockito: test successful update, not-found, trainNumber conflict, same-trainNumber no-conflict scenarios
- [x] 10.2 Write `UpdateSeatUseCaseTest.java` covering: successful update, not-found, seatNumber conflict within same train, same-seatNumber no-conflict
- [x] 10.3 Write `UpdateRouteUseCaseTest.java` covering: successful partial update of each field, not-found
- [x] 10.4 Write `UpdateStationUseCaseTest.java` covering: successful update, not-found, code conflict, same-code no-conflict
- [x] 10.5 Write or extend `TrainControllerTest.java` (`@WebMvcTest`) to cover: 200 OK on valid PATCH, 404 on missing train, 409 on duplicate trainNumber, 403 on non-admin, 400 on validation failure
- [x] 10.6 Write or extend `SeatControllerTest.java` to cover: 200 OK on valid PATCH, 404 on missing seat, 409 on duplicate seatNumber, 403 on non-admin, 400 on validation failure
- [x] 10.7 Write or extend `RouteControllerTest.java` to cover: 200 OK on valid PATCH, 404 on missing route, 403 on non-admin, 400 on invalid status/price
- [x] 10.8 Write or extend `StationControllerTest.java` to cover: 200 OK on valid PATCH, 404 on missing station, 409 on duplicate code, 403 on non-admin, 400 on blank code

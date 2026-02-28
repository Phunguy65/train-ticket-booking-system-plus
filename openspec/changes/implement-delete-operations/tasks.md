# Tasks

## 1. Shared — Error Codes

- [x] 1.1 Add `TRAIN_NOT_FOUND`, `TRAIN_IN_USE`, `SEAT_IN_USE`, `STATION_IN_USE` to the shared `ErrorCode` enum

## 2. Train Module — Domain Layer

- [x] 2.1 Add `deletedAt` field (`Instant`) to `TrainEntity` with `@Column(name = "deleted_at")` and update `TrainEntityMapper` to map the field in both directions
- [x] 2.2 Add `deletedAt` field, `softDelete()` method (sets `deletedAt = Instant.now()`, registers `TrainDeleted` event), and `isDeleted()` predicate to the `Train` aggregate root
- [x] 2.3 Create `TrainDeleted` domain event in `train/domain/event/TrainDeleted.java` (past-tense naming, same pattern as `TrainCreated`)
- [x] 2.4 Add `TrainNotFound` and `TrainInUse` records to the `TrainError` sealed interface

## 3. Train Module — Repository Layer

- [x] 3.1 Add `softDeleteById(TrainId id, Instant deletedAt)` and `softDeleteByIds(List<TrainId> ids, Instant deletedAt)` methods to the `TrainRepository` domain interface
- [x] 3.2 Add `existsActiveRouteByTrainId(UUID trainId)` to `RouteJpaRepository` using `@Query` that checks `deleted_at IS NULL`
- [x] 3.3 Add `existsActiveByTrainId(TrainId trainId)` to the `RouteRepository` domain interface
- [x] 3.4 Implement the above in `RouteRepositoryAdapter`
- [x] 3.5 Add `@Modifying @Query` `softDeleteById` and `softDeleteByIds` to `TrainJpaRepository` (UPDATE … SET deleted_at WHERE deleted_at IS NULL)
- [x] 3.6 Implement `softDeleteById` and `softDeleteByIds` in `TrainRepositoryAdapter`
- [x] 3.7 Audit `TrainJpaRepository` `findAll` / `findBy*` queries and add `deleted_at IS NULL` filter where missing

## 4. Train Module — Application Layer (Single Delete)

- [x] 4.1 Create `SoftDeleteTrainCommand` record in `train/application/command/`
- [x] 4.2 Create `SoftDeleteTrainUseCase`: load aggregate → idempotency check → active-route guard via `RouteRepository.existsActiveByTrainId` → `train.softDelete()` → save → publish `TrainDeleted` event

## 5. Train Module — Application Layer (Bulk Delete)

- [x] 5.1 Create `BulkSoftDeleteTrainsCommand` record (holds `List<TrainId>`)
- [x] 5.2 Create `BulkSoftDeleteTrainsUseCase`: validate all IDs have no active routes (collect conflicting IDs) → if any conflicts return `Result.failure(TrainError.TrainInUse(conflictingIds))` → else batch UPDATE via `TrainRepository.softDeleteByIds` → publish `TrainDeleted` per ID

## 6. Train Module — Web Layer

- [x] 6.1 Create `BulkSoftDeleteTrainsHttpRequest` record with `@NotEmpty @Size(max = 100) List<@NotNull UUID> trainIds`
- [x] 6.2 Add `@DeleteMapping("/{id}")` endpoint to `TrainController` with `@PreAuthorize("hasRole('ADMIN')")` wired to `SoftDeleteTrainUseCase`
- [x] 6.3 Add `@DeleteMapping` (bulk) endpoint to `TrainController` wired to `BulkSoftDeleteTrainsUseCase`; handle `TrainInUse` as `422`
- [x] 6.4 Add error mapping for `TrainNotFound` → `404` and `TrainInUse` → `422` in `TrainController` error handler
- [x] 6.5 Register DELETE matchers in `SecurityConfig` for `/api/*/trains` and `/api/*/trains/**` requiring `ADMIN`

## 7. Seat Module — Domain Layer

- [x] 7.1 Add `deletedAt` field to `SeatEntity` with `@Column(name = "deleted_at")` and update `SeatEntityMapper`
- [x] 7.2 Add `deletedAt` field, `softDelete()` method (registers `SeatDeleted`), and `isDeleted()` to the `Seat` aggregate root
- [x] 7.3 Create `SeatDeleted` domain event in `train/domain/event/SeatDeleted.java`
- [x] 7.4 Add `SeatNotFound` (if missing) and `SeatInUse` records to the `SeatError` sealed interface

## 8. Seat Module — Repository Layer

- [x] 8.1 Add `softDeleteById(SeatId id, Instant deletedAt)` and `softDeleteByIds(List<SeatId> ids, Instant deletedAt)` to `SeatRepository` domain interface
- [x] 8.2 Add `existsActiveBySeatId(UUID seatId)` and `existsActiveByAnyOfSeatIds(List<UUID> seatIds)` to `RouteSeatAvailabilityJpaRepository` (status IN ('HELD', 'BOOKED'))
- [x] 8.3 Add corresponding methods to `RouteSeatAvailabilityRepository` domain interface and implement in its adapter
- [x] 8.4 Add `@Modifying @Query` soft-delete methods to `SeatJpaRepository`
- [x] 8.5 Implement `softDeleteById` and `softDeleteByIds` in `SeatRepositoryAdapter`
- [x] 8.6 Audit `SeatJpaRepository` `findBy*` queries and add `deleted_at IS NULL` filter where missing

## 9. Seat Module — Application Layer

- [x] 9.1 Create `SoftDeleteSeatCommand` record
- [x] 9.2 Create `SoftDeleteSeatUseCase`: load → idempotency → active-availability guard via `RouteSeatAvailabilityRepository` → `seat.softDelete()` → save → publish `SeatDeleted`
- [x] 9.3 Create `BulkSoftDeleteSeatsCommand` record (holds `List<SeatId>`)
- [x] 9.4 Create `BulkSoftDeleteSeatsUseCase`: validate all IDs have no HELD/BOOKED availability (collect conflicting IDs) → if any conflicts return failure → else batch UPDATE → publish events

## 10. Seat Module — Web Layer

- [x] 10.1 Create `BulkSoftDeleteSeatsHttpRequest` record with `@NotEmpty @Size(max = 100) List<@NotNull UUID> seatIds`
- [x] 10.2 Add `@DeleteMapping("/{id}")` to `SeatController` wired to `SoftDeleteSeatUseCase`
- [x] 10.3 Add `@DeleteMapping` (bulk) to `SeatController` wired to `BulkSoftDeleteSeatsUseCase`; handle `SeatInUse` as `422`
- [x] 10.4 Add error mapping for `SeatNotFound` → `404` and `SeatInUse` → `422`
- [x] 10.5 Register DELETE matchers in `SecurityConfig` for `/api/*/seats` and `/api/*/seats/**` requiring `ADMIN`

## 11. Cross-Module Port — RouteValidationPort

- [x] 11.1 Create `RouteValidationPort` interface in `train/application/port/RouteValidationPort.java` with method `hasActiveRoutesForStation(StationId stationId)`
- [x] 11.2 Create `RouteValidationPortAdapter` in `train/infrastructure/` that implements `RouteValidationPort` by delegating to `RouteRepository.existsActiveByStationId`
- [x] 11.3 Add `existsActiveByStationId(StationId stationId)` to `RouteRepository` domain interface and implement in `RouteRepositoryAdapter` with corresponding `@Query` in `RouteJpaRepository`
- [x] 11.4 Annotate `train/application/port/` package (or a suitable sub-package) with `@NamedInterface("validation")` in `package-info.java` to expose the port
- [x] 11.5 Update `train` module's `package-info.java` `allowedDependencies` to include `station::model` (needed because `RouteValidationPort` accepts `StationId`)
- [x] 11.6 Update `station` module's `package-info.java` `allowedDependencies` to include `train::validation`

## 12. Station Module — Domain Layer

- [x] 12.1 Add `deletedAt` field to `StationEntity` with `@Column(name = "deleted_at")` and update `StationEntityMapper`
- [x] 12.2 Add `deletedAt` field, `softDelete()` method (registers `StationDeleted`), and `isDeleted()` to the `Station` aggregate root
- [x] 12.3 Create `StationDeleted` domain event in `station/domain/event/StationDeleted.java`
- [x] 12.4 Add `StationNotFound` (if missing) and `StationInUse` records to the `StationError` sealed interface

## 13. Station Module — Repository Layer

- [x] 13.1 Add `softDeleteById(StationId id, Instant deletedAt)` and `softDeleteByIds(List<StationId> ids, Instant deletedAt)` to `StationRepository` domain interface
- [x] 13.2 Add `@Modifying @Query` soft-delete methods to `StationJpaRepository`
- [x] 13.3 Implement `softDeleteById` and `softDeleteByIds` in `StationRepositoryAdapter`
- [x] 13.4 Audit `StationJpaRepository` `findBy*` queries and add `deleted_at IS NULL` filter where missing

## 14. Station Module — Application Layer

- [x] 14.1 Create `SoftDeleteStationCommand` record
- [x] 14.2 Create `SoftDeleteStationUseCase`: load → idempotency → active-route guard via `RouteValidationPort.hasActiveRoutesForStation` → `station.softDelete()` → save → publish `StationDeleted`
- [x] 14.3 Create `BulkSoftDeleteStationsCommand` record (holds `List<StationId>`)
- [x] 14.4 Create `BulkSoftDeleteStationsUseCase`: validate all IDs via `RouteValidationPort` (collect conflicting IDs) → if any conflicts return failure → else batch UPDATE → publish events

## 15. Station Module — Web Layer

- [x] 15.1 Create `BulkSoftDeleteStationsHttpRequest` record with `@NotEmpty @Size(max = 100) List<@NotNull UUID> stationIds`
- [x] 15.2 Add `@DeleteMapping("/{id}")` to `StationController` wired to `SoftDeleteStationUseCase`
- [x] 15.3 Add `@DeleteMapping` (bulk) to `StationController` wired to `BulkSoftDeleteStationsUseCase`; handle `StationInUse` as `422`
- [x] 15.4 Add error mapping for `StationNotFound` → `404` and `StationInUse` → `422`
- [x] 15.5 Register DELETE matchers in `SecurityConfig` for `/api/*/stations` and `/api/*/stations/**` requiring `ADMIN`

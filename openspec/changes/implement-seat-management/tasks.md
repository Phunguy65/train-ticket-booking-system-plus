## 1. Database Migration

- [x] 1.1 Create `database/migrations/V4.0.0__implement_seat_management.sql` — drop `idx_seats_train_status` index, drop `seats.status` column, create `route_seat_availability` table with composite PK `(route_id, seat_id)`, `status` CHECK constraint, `version` column, FK constraints, and `idx_route_seat_status` index

## 2. Seat Domain Layer (train module)

- [x] 2.1 Create `SeatClass.java` enum (`ECONOMY`, `BUSINESS`, `FIRST_CLASS`) in `train/domain/model/`
- [x] 2.2 Create `SeatId.java` type-safe value object wrapping `UUID` in `train/domain/model/` (expose via `@NamedInterface("model")` in `package-info.java`)
- [x] 2.3 Create `Seat.java` aggregate with `create()` and `reconstitute()` factory methods in `train/domain/model/`
- [x] 2.4 Create `SeatError.java` sealed interface with `SeatNumberAlreadyExists` and `TrainNotFound` variants in `train/domain/errors/`
- [x] 2.5 Create `SeatRepository.java` interface with `save()`, `findByTrainId()`, and `findById()` in `train/domain/repository/`

## 3. RouteSeatAvailability Domain Layer (train module)

- [x] 3.1 Create `RouteSeatAvailabilityStatus.java` enum (`AVAILABLE`, `BOOKED`, `CANCELLED`) in `train/domain/model/`
- [x] 3.2 Create `RouteId.java` value object in `train/domain/model/` (mirrors `booking` module's `RouteId` — needed for cross-module event handling)
- [x] 3.3 Create `RouteSeatAvailability.java` entity with `book()`, `cancel()`, `release()` state-transition methods returning `Result<Void, RouteSeatAvailabilityError>` in `train/domain/model/`
- [x] 3.4 Create `RouteSeatAvailabilityError.java` sealed interface with `SeatNotAvailable` variant in `train/domain/errors/`
- [x] 3.5 Create `RouteSeatAvailabilityRepository.java` interface with `findAvailableByRouteId()`, `findByRouteIdAndSeatId()`, and `saveAll()` in `train/domain/repository/`

## 4. Seat Application Layer (train module)

- [x] 4.1 Create `CreateSeatCommand.java` record (`trainId`, `seatNumber`, `seatClass`) in `train/application/command/`
- [x] 4.2 Create `SeatDto.java` record (`id`, `trainId`, `seatNumber`, `seatClass`, `createdAt`) in `train/application/dto/`
- [x] 4.3 Create `CreateSeatUseCase.java` (`@Service @Transactional`) — validate train exists, check duplicate seat number, create and persist `Seat`, return `Result<SeatDto, SeatError>` in `train/application/usecase/`
- [x] 4.4 Create `GetSeatsByTrainUseCase.java` (`@Service`) — query seats by `trainId`, return `List<SeatDto>` in `train/application/usecase/`
- [x] 4.5 Create `GetAvailableSeatsForRouteUseCase.java` (`@Service`) — query `RouteSeatAvailabilityRepository` for `AVAILABLE` seats on a route, return `List<SeatDto>` in `train/application/usecase/`

## 5. RouteSeatAvailabilityPort (cross-module interface)

- [x] 5.1 Create `availability/` sub-package in `train/` with `@NamedInterface("availability")` in `package-info.java`
- [x] 5.2 Create `RouteSeatAvailabilityPort.java` interface with `reserveSeat(RouteId, SeatId): Result<Void, RouteSeatAvailabilityError>` in `train/availability/`
- [x] 5.3 Update `train/package-info.java` `@ApplicationModule` to declare `@NamedInterface("availability")` (handled via sub-package annotation)

## 6. Seat Persistence Layer (train module)

- [x] 6.1 Create `SeatEntity.java` JPA entity mapping to `seats` table (no `status` column) in `train/infrastructure/persistence/`
- [x] 6.2 Create `SeatJpaRepository.java` Spring Data JPA interface (package-private) in `train/infrastructure/persistence/`
- [x] 6.3 Create `SeatRepositoryAdapter.java` implementing `SeatRepository` domain port in `train/infrastructure/persistence/`
- [x] 6.4 Create `SeatEntityMapper.java` bidirectional mapper (`SeatEntity` ↔ `Seat`) in `train/infrastructure/persistence/`
- [x] 6.5 Create `RouteSeatAvailabilityEntity.java` JPA entity mapping to `route_seat_availability` table with `@Version` on `version` field in `train/infrastructure/persistence/`
- [x] 6.6 Create `RouteSeatAvailabilityJpaRepository.java` Spring Data JPA interface (package-private) in `train/infrastructure/persistence/`
- [x] 6.7 Create `RouteSeatAvailabilityRepositoryAdapter.java` implementing `RouteSeatAvailabilityRepository` domain port in `train/infrastructure/persistence/`
- [x] 6.8 Create `RouteSeatAvailabilityEntityMapper.java` bidirectional mapper in `train/infrastructure/persistence/`
- [x] 6.9 Create `RouteSeatAvailabilityPortAdapter.java` implementing `RouteSeatAvailabilityPort` (uses `RouteSeatAvailabilityRepository`) in `train/availability/`

## 7. Route-Created Event Listener (train module)

- [x] 7.1 Identify or create the `RouteCreated` domain event class — created in `train/domain/event/RouteCreated.java`
- [x] 7.2 Create `SeatAvailabilitySeeder.java` `@ApplicationModuleListener` in `train/application/`

## 8. Seat Web Layer (train module)

- [x] 8.1 Create `CreateSeatHttpRequest.java` HTTP request DTO (`seatNumber`, `seatClass`) in `train/infrastructure/web/`
- [x] 8.2 Create `SeatHttpResponse.java` HTTP response DTO (`id`, `trainId`, `seatNumber`, `seatClass`, `createdAt`) in `train/infrastructure/web/`
- [x] 8.3 Create `SeatRequestMapper.java` mapping `CreateSeatHttpRequest` → `CreateSeatCommand` and `SeatDto` → `SeatHttpResponse` in `train/infrastructure/web/`
- [x] 8.4 Add seat endpoints to `SeatController.java`: `POST /api/v1/trains/{trainId}/seats` (admin-only) and `GET /api/v1/trains/{trainId}/seats` in `train/infrastructure/web/`
- [x] 8.5 Add `GET /api/v1/routes/{routeId}/seats/available` endpoint in `train/infrastructure/web/`

## 9. Booking Module Integration

- [x] 9.1 Update `booking/package-info.java` to add `allowedDependencies = {"train::availability", "train::model"}` to `@ApplicationModule`
- [x] 9.2 Update `CreateBookingUseCase.java` to inject `RouteSeatAvailabilityPort` and call `reserveSeat(routeId, seatId)` within the booking transaction; return `Result<BookingDto, BookingError>` (409 Conflict on `SeatNotAvailable`)
- [x] 9.3 Update `GlobalExceptionHandler` to map `ObjectOptimisticLockingFailureException` to HTTP `409 Conflict`

## 10. Tests

- [x] 10.1 Create `SeatTest.java` — pure JUnit 5 unit tests for `Seat.create()`, `Seat.reconstitute()`, and `SeatClass` invariants in `train/domain/model/`
- [x] 10.2 Create `RouteSeatAvailabilityTest.java` — pure JUnit 5 unit tests for `book()`, `cancel()`, `release()` transitions and conflict cases in `train/domain/model/`
- [x] 10.3 Create `CreateSeatUseCaseTest.java` — Mockito unit tests for success, train-not-found, and duplicate-seat-number scenarios in `train/application/usecase/`
- [x] 10.4 Create `GetSeatsByTrainUseCaseTest.java` — Mockito unit tests in `train/application/usecase/`
- [x] 10.5 Create `SeatRepositoryAdapterTest.java` — `@DataJpaTest` persistence integration tests covering save and findByTrainId in `train/infrastructure/persistence/`
- [x] 10.6 Create `RouteSeatAvailabilityRepositoryAdapterTest.java` — `@DataJpaTest` persistence integration tests including optimistic lock conflict scenario in `train/infrastructure/persistence/`
- [x] 10.7 Create `SeatControllerTest.java` — `@WebMvcTest` REST tests for `POST` and `GET` seat endpoints in `train/infrastructure/web/`
- [x] 10.8 Update `CreateBookingUseCaseTest.java` — added test scenarios for seat-not-available failure path in `booking/application/usecase/`
- [x] 10.9 Update `TrainModuleTest.java` — added `@ApplicationModuleTest` tests verifying module boundaries including the `availability` named interface

# Tasks

## 1. Train Module — Optimistic Locking Migration (Prerequisite)

- [x] 1.1 Add `@Version Integer version` field to `RouteSeatAvailabilityEntity` and update `setVersion`/`getVersion` accessors
- [x] 1.2 Add `version` field to `RouteSeatAvailability` domain model; update `reconstitute()` factory to accept `version` parameter
- [x] 1.3 Update `RouteSeatAvailabilityEntityMapper.toDomain()` and `toEntity()` to pass `version` through
- [x] 1.4 Replace `findByRouteIdAndSeatIdsForUpdate` (pessimistic) with `findByRouteIdAndSeatIds` (plain query, no `@Lock`) in `RouteSeatAvailabilityJpaRepository`
- [x] 1.5 Update `RouteSeatAvailabilityRepositoryAdapter.findByRouteIdAndSeatIdsForUpdate()` to call the new plain query method; rename method to `findByRouteIdAndSeatIds`
- [x] 1.6 Update `RouteSeatAvailabilityPortAdapter.holdSeats()` and `confirmHeldSeats()` to remove pessimistic lock usage; rely on `@Version` for conflict detection
- [x] 1.7 Add `List<SeatId> findSeatIdsByBookingId(BookingId bookingId)` to `RouteSeatAvailabilityPort` interface
- [x] 1.8 Implement `findSeatIdsByBookingId` in `RouteSeatAvailabilityPortAdapter` via JPQL query on `route_seat_availability WHERE booking_id = ?`
- [x] 1.9 Add `findByBookingId` query method to `RouteSeatAvailabilityJpaRepository`
- [x] 1.10 Update `findAvailableByRouteId` query to include lazy expiry: treat `HELD` seats with expired booking `payment_deadline` as available (JOIN with `bookings` table)
- [x] 1.11 Update `ConcurrentSeatHoldTest` to validate `OptimisticLockException` / `409 Conflict` instead of pessimistic lock behavior

## 2. Booking Domain Layer

- [x] 2.1 Create `booking/domain/model/BookingId.java` — type-safe UUID wrapper with `of(UUID)` factory; null check
- [x] 2.2 Create `booking/domain/model/UserId.java` — type-safe UUID wrapper (booking module's own copy; does not conflict with user module's `UserId` due to module boundary)
- [x] 2.3 Create `booking/domain/model/RouteId.java` — type-safe UUID wrapper
- [x] 2.4 Create `booking/domain/model/BookingStatus.java` — enum: `HELD`, `CONFIRMED`, `CANCELLED`
- [x] 2.5 Create `booking/domain/model/Booking.java` — aggregate root extending `AggregateRoot<BookingId>` with fields: `bookingId`, `userId`, `routeId`, `passengerName`, `passengerEmail`, `passengerPhone`, `totalPrice` (Money), `currency`, `status`, `idempotencyKey`, `paymentDeadline`, `createdAt`; factory methods `create()` (registers `BookingCreated`) and `reconstitute()` (no events); domain methods `confirm()` and `cancel()` returning `Result<Void, BookingError>`
- [x] 2.6 Create `booking/domain/error/BookingError.java` — sealed interface with variants: `BookingNotFound`, `SeatNotAvailable`, `ActiveHoldExists`, `InvalidStatusTransition`, `Forbidden`; each implements `message()`
- [x] 2.7 Create `booking/domain/event/BookingCreated.java` — record with `bookingId`, `userId`, `routeId`
- [x] 2.8 Create `booking/domain/event/BookingConfirmed.java` — record with `bookingId`, `userId`, `routeId`
- [x] 2.9 Create `booking/domain/event/BookingCancelled.java` — record with `bookingId`, `userId`, `routeId`, `requiresRefund` (boolean)
- [x] 2.10 Create `booking/domain/repository/BookingRepository.java` — interface with: `save(Booking)`, `findById(BookingId)`, `findByIdempotencyKey(String)`, `findActiveHoldByUserAndRoute(UserId, RouteId)`, `findExpiredHeldBookings(Instant now)`, `saveAll(List<Booking>)`
- [x] 2.11 Write unit tests for `Booking` domain model in `BookingTest.java` covering all state transitions and edge cases

## 3. Booking Application Layer

- [x] 3.1 Create `booking/application/command/CreateBookingCommand.java` — record with `userId`, `routeId`, `seatIds` (List<SeatId>), `passengerName`, `passengerEmail`, `passengerPhone`, `idempotencyKey`
- [x] 3.2 Create `booking/application/command/CancelBookingCommand.java` — record with `bookingId`, `requestingUserId`
- [x] 3.3 Create `booking/application/dto/BookingDto.java` — record with `id`, `userId`, `routeId`, `passengerName`, `passengerEmail`, `passengerPhone`, `totalPrice`, `currency`, `status`, `paymentDeadline`, `createdAt`
- [x] 3.4 Create `booking/application/usecase/CreateBookingUseCase.java` — `@Service @Transactional`; steps: idempotency check → active hold check → `holdSeats()` → `Booking.create()` → `save()` → publish events → return `BookingDto`
- [x] 3.5 Create `booking/application/usecase/CancelBookingUseCase.java` — `@Service @Transactional`; steps: load booking → ownership check → `booking.cancel()` → status-aware seat release → `save()` → publish `BookingCancelled`
- [x] 3.6 Create `booking/application/usecase/ExpireHeldBookingsUseCase.java` — `@Service @Transactional`; steps: `findExpiredHeldBookings(now)` → for each: `booking.cancel()` → `releaseHeldSeats()` → `saveAll()` → publish events
- [x] 3.7 Write unit tests for `CreateBookingUseCase` with Mockito (idempotency, seat unavailable, active hold, success)
- [x] 3.8 Write unit tests for `CancelBookingUseCase` with Mockito (HELD cancel, CONFIRMED cancel, forbidden, not found, already cancelled)
- [x] 3.9 Write unit tests for `ExpireHeldBookingsUseCase` with Mockito (no expired, multiple expired, idempotency)

## 4. Booking Infrastructure — Persistence

- [x] 4.1 Create `booking/infrastructure/persistence/BookingEntity.java` — `@Entity @Table(name = "bookings")` with all columns from schema; `protected BookingEntity() {}`; no `@GeneratedValue`
- [x] 4.2 Create `booking/infrastructure/persistence/BookingJpaRepository.java` — `JpaRepository<BookingEntity, UUID>` with: `findByIdempotencyKey`, `findByUserIdAndRouteIdAndStatus`, `findByStatusAndPaymentDeadlineBefore`
- [x] 4.3 Create `booking/infrastructure/persistence/BookingEntityMapper.java` — `@Component`; `toDomain()` calls `Booking.reconstitute()`; `toEntity()` maps all fields
- [x] 4.4 Create `booking/infrastructure/persistence/BookingRepositoryAdapter.java` — `@Repository` implementing `BookingRepository`; delegates to `BookingJpaRepository` + `BookingEntityMapper`
- [x] 4.5 Write `@DataJpaTest` for `BookingRepositoryAdapter` covering `save`, `findById`, `findByIdempotencyKey`, `findActiveHoldByUserAndRoute`, `findExpiredHeldBookings`

## 5. Booking Infrastructure — Scheduler

- [x] 5.1 Create `booking/infrastructure/scheduler/BookingExpiryScheduler.java` — `@Component` with `@Scheduled(fixedDelay = 60_000)` invoking `expireHeldBookingsUseCase.execute()`; wrap in try-catch and log exceptions
- [x] 5.2 Add `@EnableScheduling` to the Spring Boot application main class

## 6. Booking Infrastructure — Web

- [x] 6.1 Create `booking/infrastructure/web/CreateBookingHttpRequest.java` — record with Bean Validation: `@NotNull routeId`, `@NotEmpty seatIds`, `@NotBlank passengerName`, `@Email passengerEmail`, `@NotBlank idempotencyKey`; optional `passengerPhone`
- [x] 6.2 Create `booking/infrastructure/web/BookingHttpResponse.java` — record mirroring `BookingDto` fields for HTTP response
- [x] 6.3 Create `booking/infrastructure/web/BookingRequestMapper.java` — `@Component`; maps `CreateBookingHttpRequest` → `CreateBookingCommand`; maps `BookingDto` → `BookingHttpResponse`
- [x] 6.4 Create `booking/infrastructure/web/BookingController.java` — `@RestController`; `POST /api/v1/bookings` (authenticated, returns 201); `POST /api/v1/bookings/{id}/cancel` (authenticated, returns 200); maps `BookingError` variants to HTTP status codes
- [x] 6.5 Write `@WebMvcTest` for `BookingController` covering: success 201, seat unavailable 409, active hold 409, invalid body 400, cancel success 200, cancel forbidden 403, cancel not found 404

## 7. Module Boundary & Integration

- [ ] 7.1 Create `booking/package-info.java` with `@ApplicationModule(allowedDependencies = {"train::port", "train::model", "user::model", "shared"})`
- [ ] 7.2 Verify Spring Modulith module structure compiles without circular dependency errors (`BookingModuleTest.java` with `@ApplicationModuleTest`)
- [ ] 7.3 Add `GlobalExceptionHandler` mapping for `OptimisticLockException` → `409 Conflict` with error code `SEAT_CONFLICT` (if not already present)
- [ ] 7.4 Run full test suite to confirm no regressions in train module after optimistic locking migration

# Tasks

## 1. Database Migrations

- [x] 1.1 Write `V5__add_held_seat_status.sql`: drop and recreate `chk_rsa_status` CHECK constraint on `route_seat_availability` to include `'HELD'`
- [x] 1.2 Write `V6__add_booking_seats_table.sql`: create `booking_seats(booking_id, seat_id, price_at_booking, seat_class_at_booking)` table with composite PK and FK constraints; migrate existing `bookings.seat_id` rows into `booking_seats` with `price_at_booking = bookings.total_price`; add index on `bookings(status, payment_deadline)`; then DROP `bookings.seat_id`
- [x] 1.3 Write `V7__add_unique_active_hold_index.sql`: create partial unique index `idx_one_active_hold_per_user_route ON bookings(user_id, route_id) WHERE status = 'HELD'`
- [x] 1.4 Write `V8__update_booking_status_constraint.sql`: update `bookings.status` CHECK constraint to `('HELD','CONFIRMED','CANCELLED')`; UPDATE existing `PENDING` rows to `HELD`

## 2. Train Module — Seat Availability Domain

- [x] 2.1 Add `HELD` to `RouteSeatAvailabilityStatus` enum
- [x] 2.2 Add `hold()` method to `RouteSeatAvailability`: `AVAILABLE → HELD`, returns `Result<Void, RouteSeatAvailabilityError>`
- [x] 2.3 Add `confirmHold()` method to `RouteSeatAvailability`: `HELD → BOOKED`, returns `Result<Void, RouteSeatAvailabilityError>`
- [x] 2.4 Add `expire()` method to `RouteSeatAvailability`: `HELD → AVAILABLE`, returns `Result<Void, RouteSeatAvailabilityError>`
- [x] 2.5 Update unit tests in `RouteSeatAvailabilityTest` to cover new `hold()`, `confirmHold()`, `expire()` state transitions

## 3. Train Module — Pessimistic Lock Repository

- [x] 3.1 Add `findByRouteIdAndSeatIdsForUpdate(UUID routeId, List<UUID> seatIds)` to `RouteSeatAvailabilityJpaRepository` with `@Lock(PESSIMISTIC_WRITE)` and `@QueryHint(javax.persistence.lock.timeout = 3000)` — query MUST include `ORDER BY e.id.seatId ASC`
- [x] 3.2 Extend `RouteSeatAvailabilityPort` interface: add `holdSeats(RouteId, List<SeatId>)`, `confirmHeldSeats(RouteId, List<SeatId>)`, `releaseHeldSeats(RouteId, List<SeatId>)`
- [x] 3.3 Implement the three new port methods in `RouteSeatAvailabilityPortAdapter`: sort seatIds ascending, call `findByRouteIdAndSeatIdsForUpdate`, validate all-or-nothing, call domain method per entity, save all
- [x] 3.4 Write `@DataJpaTest` integration tests for `RouteSeatAvailabilityPortAdapter` covering: successful batch hold, partial unavailability (all-or-nothing), lock timeout scenario using two concurrent threads

## 4. Booking Module — Domain Model

- [x] 4.1 Create `BookedSeat` value object in `booking/domain/model/`: fields `SeatId seatId`, `BigDecimal unitPrice`, `SeatClass seatClass`; no-mutation, constructed only via factory
- [x] 4.2 Refactor `Booking` aggregate: replace `SeatId seatId` with `List<BookedSeat> bookedSeats`; add `Instant paymentDeadline`; add `String paymentReference` (nullable)
- [x] 4.3 Replace `Booking.create()` with `Booking.createHold(userId, routeId, bookedSeats, totalPrice, currency, paymentDeadline, idempotencyKey)` — registers `SeatHoldCreated` event
- [x] 4.4 Update `Booking.confirm(paymentReference)`: guard `status == HELD` AND `paymentDeadline > Instant.now()`; transition to `CONFIRMED`; registers `BookingConfirmed`
- [x] 4.5 Add `Booking.expire()`: `HELD → CANCELLED`, registers `SeatHoldExpired` event
- [x] 4.6 Update `BookingStatus` enum: remove `PENDING`, ensure `HELD`, `CONFIRMED`, `CANCELLED` are present
- [x] 4.7 Add `SeatHoldCreated` domain event: fields `bookingId`, `userId`, `routeId`, `seatIds`, `expiresAt`
- [x] 4.8 Add `SeatHoldExpired` domain event: fields `bookingId`, `userId`, `routeId`, `seatIds`
- [x] 4.9 Update `BookingError` sealed interface: add `SeatsNotAvailable(List<UUID> seatIds)`, `SeatsLocked`, `ActiveHoldExists`, `HoldExpired`, `InvalidStatusTransition(BookingStatus current)`
- [x] 4.10 Write unit tests in `BookingTest` covering all new state transitions and error cases

## 5. Booking Module — Pricing Domain Service

- [x] 5.1 Add `getPriceMultiplier(): BigDecimal` to `SeatClass` enum: `ECONOMY=1.0`, `BUSINESS=1.5`, `FIRST_CLASS=2.0`
- [x] 5.2 Create `PricingService` domain service in `booking/application/`: method `calculatePrices(Route route, List<Seat> seats) → List<BookedSeatPrice>` using `BigDecimal` arithmetic
- [x] 5.3 Write unit tests for `PricingService` covering all three seat classes and mixed multi-seat scenarios

## 6. Booking Module — Repository Port

- [x] 6.1 Add `findActiveHoldByUserIdAndRouteId(UserId, RouteId): Optional<Booking>` to `BookingRepository` interface
- [x] 6.2 Add `findExpiredHolds(Instant now, int limit): List<Booking>` to `BookingRepository` interface
- [x] 6.3 Add `findByIdWithSeats(BookingId): Optional<Booking>` to `BookingRepository` interface (eager-loads `bookedSeats`)

## 7. Booking Module — Persistence Infrastructure

- [x] 7.1 Create `BookingSeatsEntity` JPA entity mapping `booking_seats` table
- [x] 7.2 Create `BookingSeatsJpaRepository` Spring Data JPA interface
- [x] 7.3 Update `BookingEntity`: remove `seatId` field; add `paymentDeadline`; add `@OneToMany` to `BookingSeatsEntity`
- [x] 7.4 Update `BookingEntityMapper.toDomain()` and `toEntity()` to handle `BookedSeat` list and `paymentDeadline`; use `Booking.reconstitute()` for loaded bookings
- [x] 7.5 Implement `findActiveHoldByUserIdAndRouteId` in `BookingRepositoryAdapter` using JPQL query
- [x] 7.6 Implement `findExpiredHolds` in `BookingRepositoryAdapter` using JPQL with `status = 'HELD' AND paymentDeadline < :now ORDER BY paymentDeadline ASC` with LIMIT
- [x] 7.7 Write `@DataJpaTest` for `BookingRepositoryAdapter` covering new query methods

## 8. Booking Module — Application Use Cases

- [x] 8.1 Create `CreateSeatHoldCommand` record: `userId`, `routeId`, `seatIds: List<UUID>`, `idempotencyKey`, `passengerName`, `passengerEmail`, `passengerPhone`
- [x] 8.2 Create `CreateSeatHoldUseCase` (@Service @Transactional): idempotency check → active hold check → load route + seats → calculate prices → sort seatIds ASC → call `seatAvailabilityPort.holdSeats()` → `Booking.createHold()` → save → publish events
- [x] 8.3 Create `ConfirmSeatHoldCommand` record: `bookingId`, `paymentReference`
- [x] 8.4 Create `ConfirmSeatHoldUseCase` (@Service @Transactional): find booking → validate HELD + not expired → `seatAvailabilityPort.confirmHeldSeats()` → `booking.confirm()` → save → publish events
- [x] 8.5 Create `CancelBookingCommand` record: `bookingId`
- [x] 8.6 Create `CancelBookingUseCase` (@Service @Transactional): find booking → determine seat release method based on current status → release seats via port → `booking.cancel()` → save → publish events
- [x] 8.7 Create `HoldDto` output record: `bookingId`, `status`, `routeId`, `seats: List<BookedSeatDto>`, `totalPrice`, `currency`, `expiresAt`
- [x] 8.8 Write Mockito unit tests for `CreateSeatHoldUseCase` covering: success, idempotency, active hold exists, seats unavailable, lock timeout
- [x] 8.9 Write Mockito unit tests for `ConfirmSeatHoldUseCase` covering: success, hold expired, invalid status
- [x] 8.10 Write Mockito unit tests for `CancelBookingUseCase` covering: cancel HELD, cancel CONFIRMED, cancel already CANCELLED

## 9. Booking Module — Expiry Scheduled Job

- [x] 9.1 Create `ExpireHoldsJob` class (`@Component`) with `@Scheduled(fixedDelay = 60_000)` method `expireStaleHolds()`
- [x] 9.2 Implement expiry loop: call `bookingRepository.findExpiredHolds(Instant.now(), 100)` → for each: start transaction, lock booking row (pessimistic), re-validate status == HELD, call `seatAvailabilityPort.releaseHeldSeats()`, call `booking.expire()`, save, publish `SeatHoldExpired`

## 10. Booking Module — Web Layer

- [x] 10.1 Create `CreateSeatHoldHttpRequest` record: `userId: UUID`, `routeId: UUID`, `seatIds: List<UUID>` (non-empty), `idempotencyKey: String`, `passengerName`, `passengerEmail`, `passengerPhone`
- [x] 10.2 Create `ConfirmSeatHoldHttpRequest` record: `paymentReference: String`
- [x] 10.3 Create `BookingHttpResponse` record: updated to include `seats: List<BookedSeatResponse>` and `expiresAt: Instant` (nullable)
- [x] 10.4 Update `BookingController`: add `POST /api/{version}/bookings/hold` → `CreateSeatHoldUseCase`; add `POST /api/{version}/bookings/{id}/confirm` → `ConfirmSeatHoldUseCase`; add `DELETE /api/{version}/bookings/{id}` → `CancelBookingUseCase`; update `GET /api/{version}/bookings/{id}` response to include new fields
- [x] 10.5 Update `BookingRequestMapper` to map new HTTP request types to commands
- [x] 10.6 Add `PessimisticLockException` / `LockTimeoutException` handler in `GlobalExceptionHandler` returning `409 Conflict`
- [x] 10.7 Write `@WebMvcTest` for `BookingController` covering all new endpoints: success cases, 409 conflict (active hold), 409 lock timeout, 404 not found

## 11. Integration & Cleanup

- [x] 11.1 Remove `CreateBookingUseCase`, `CreateBookingCommand`, `CreateBookingHttpRequest` and their tests (replaced by new use cases)
- [x] 11.2 Run `@ApplicationModuleTest` (Spring Modulith module test) to verify booking ↔ train module boundary is valid after changes
- [x] 11.3 Run full test suite (`./gradlew test`) and fix any failing tests
- [x] 11.4 Verify Flyway migrations apply cleanly on a fresh database (`./gradlew flywayMigrate` or equivalent)

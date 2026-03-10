# Tasks

## 1. Database Migration

- [x] 1.1 Create `database/migrations/V8.0.0__drop_seat_class.sql` with `ALTER TABLE seats DROP COLUMN seat_class` and `ALTER TABLE booking_seats DROP COLUMN seat_class_at_booking`

## 2. Domain Layer — Delete SeatClass & Simplify Models

- [x] 2.1 Delete `backend/src/main/java/io/github/phunguy65/ttbs/backend/train/domain/model/SeatClass.java`
- [x] 2.2 Remove `seatClass` field and its getter from `Seat.java`; remove the `seatClass` parameter from `Seat.of()` and `Seat.reconstitute()` factory methods
- [x] 2.3 Simplify `BookedSeat.java` — remove `seatClass` field; update `BookedSeat.of(seatId, unitPrice, seatClass)` to `BookedSeat.of(seatId, unitPrice)`

## 3. Application Layer — Pricing & Commands

- [x] 3.1 Refactor `PricingService.calculatePrices()` — replace `basePrice.multiply(seat.getSeatClass().getPriceMultiplier())` with `basePrice.setScale(2, RoundingMode.HALF_UP)`; update `BookedSeat.of()` call to remove `seatClass` argument
- [x] 3.2 Remove `seatClass` field from `CreateSeatCommand.java`
- [x] 3.3 Remove `seatClass` field (`JsonNullable<SeatClass>`) from `UpdateSeatCommand.java`
- [x] 3.4 Remove `seatClass` field from `SeatDto.java`
- [x] 3.5 Remove `seatClass` field from `HoldDto.BookedSeatDto` inner record

## 4. Application Layer — Use Cases

- [x] 4.1 Update `CreateSeatUseCase.java` — remove `seatClass` from `Seat` creation call
- [x] 4.2 Update `UpdateSeatUseCase.java` — remove `seatClass` patch logic
- [x] 4.3 Update `CreateSeatHoldUseCase.java` — remove any `seatClass` mapping to/from DTOs
- [x] 4.4 Update `ConfirmSeatHoldUseCase.java` — remove `seatClass` from `BookedSeatDto` mapping
- [x] 4.5 Update `CancelBookingUseCase.java` — remove `seatClass` from `BookedSeatDto` mapping
- [x] 4.6 Update `GetBookingUseCase.java` — remove `seatClass` from `BookedSeatDto` mapping

## 5. Persistence Layer

- [x] 5.1 Remove `seatClass` column field from `SeatEntity.java` (`@Column(name = "seat_class")`)
- [x] 5.2 Remove `seatClassAtBooking` column field from `BookingSeatsEntity.java`; update constructor to drop the `seatClassAtBooking` parameter
- [x] 5.3 Update `SeatEntityMapper.java` — remove `seatClass` mapping in `toDomain()` and `toEntity()`
- [x] 5.4 Update `BookingEntityMapper.java` — remove `seatClassAtBooking` mapping in both directions

## 6. Web / HTTP Layer

- [x] 6.1 Remove `seatClass` field from `CreateSeatHttpRequest.java`
- [x] 6.2 Remove `seatClass` field (`JsonNullable<SeatClass>`) from `UpdateSeatHttpRequest.java`
- [x] 6.3 Remove `seatClass` field from `SeatHttpResponse.java`
- [x] 6.4 Remove `seatClass` field from `BookingHttpResponse.BookedSeatResponse` inner record
- [x] 6.5 Update `SeatRequestMapper.java` — remove `seatClass` mapping to `CreateSeatCommand` and `UpdateSeatCommand`
- [x] 6.6 Update `BookingRequestMapper.java` (or equivalent mapper) — remove `seatClass` mapping to `BookedSeatResponse`

## 7. API Contract

- [x] 7.1 In `shared/api-contracts/openapi.yaml`, remove `seatClass` property (and its enum values) from the `Seat` schema
- [x] 7.2 In `shared/api-contracts/openapi.yaml`, remove `seatClass` property from any `BookedSeat` / booking response schema objects

## 8. Tests

- [x] 8.1 Update `PricingServiceTest.java` — replace three distinct multiplier-based price assertions with flat `basePrice` assertions for all seat types
- [x] 8.2 Update `CreateSeatHoldUseCaseTest.java` — remove `SeatClass` from seat fixture construction; update expected `unitPrice` to equal `route.basePrice`
- [x] 8.3 Update `ConfirmSeatHoldUseCaseTest.java` — remove `SeatClass` from fixtures and response assertions
- [x] 8.4 Update `CancelBookingUseCaseTest.java` — remove `SeatClass` references
- [x] 8.5 Update `GetBookingUseCaseTest.java` — remove `SeatClass` references
- [x] 8.6 Update `CreateSeatUseCaseTest.java` — remove `seatClass` from `CreateSeatCommand` construction
- [x] 8.7 Update `UpdateSeatUseCaseTest.java` — remove `seatClass` from `UpdateSeatCommand` construction
- [x] 8.8 Update `SeatControllerTest.java` — remove `seatClass` from JSON request bodies and response `jsonPath` assertions
- [x] 8.9 Update `BookingControllerTest.java` — remove `seatClass` from response `jsonPath` assertions
- [x] 8.10 Update `BookingRepositoryAdapterTest.java` — remove `seatClass` from `BookingSeatsEntity` fixture construction
- [x] 8.11 Update any remaining test files with `SeatClass.ECONOMY`, `SeatClass.BUSINESS`, or `SeatClass.FIRST_CLASS` references (audit with grep across test sources)
- [x] 8.12 Run `./gradlew test` and confirm all tests pass with zero compilation errors

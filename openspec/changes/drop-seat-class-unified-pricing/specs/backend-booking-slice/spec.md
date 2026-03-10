# MODIFIED Requirements

## Requirement: Booking domain model enforces booking lifecycle invariants

The `Booking` aggregate SHALL extend `AggregateRoot<BookingId>` from the shared kernel and enforce all booking business rules. Direct field mutation from outside the aggregate is PROHIBITED — all state changes SHALL go through domain methods.

Booking lifecycle states: `PENDING` → `CONFIRMED` → `CANCELLED`.

### Scenario: New booking starts in PENDING status

- **WHEN** `Booking.createHold(userId, routeId, bookedSeats, totalPrice, currency, paymentDeadline, idempotencyKey, passengerName, passengerEmail, passengerPhone)` factory method is called
- **THEN** the resulting booking SHALL have `status = PENDING` and a `BookingCreated` domain event SHALL be registered on the aggregate

### Scenario: Confirming a PENDING booking transitions to CONFIRMED

- **WHEN** `booking.confirm()` is called on a booking with status `PENDING`
- **THEN** the status SHALL change to `CONFIRMED` and a `BookingConfirmed` domain event SHALL be registered

### Scenario: Confirming a non-PENDING booking throws DomainException

- **WHEN** `booking.confirm()` is called on a booking with status `CONFIRMED` or `CANCELLED`
- **THEN** a `DomainException` SHALL be thrown with a descriptive message

### Scenario: Cancelling a booking that is not CONFIRMED throws DomainException

- **WHEN** `booking.cancel()` is called on a booking with status `CANCELLED`
- **THEN** a `DomainException` SHALL be thrown

## Requirement: PricingService computes a flat unit price per seat using route base price

The `PricingService` in `booking/application/service/` SHALL calculate `unitPrice = route.getBasePrice()` for every seat — no seat class multiplier SHALL be applied. All seats on the same route SHALL receive the same unit price.

### Scenario: All seats get the same unit price equal to route base price

- **WHEN** `PricingService.calculatePrices(route, seats)` is called with a route whose `base_price` is `100000` and a list of N seats
- **THEN** every returned `BookedSeat` SHALL have `unitPrice = 100000.00` regardless of seat number or position

### Scenario: Total price is base price multiplied by seat count

- **WHEN** `PricingService.calculateTotalPrice(bookedSeats)` is called with N booked seats each priced at the route base price
- **THEN** the result SHALL equal `route.basePrice × N`

## Requirement: BookedSeat value object carries seat ID and unit price only

The `BookedSeat` value object in `booking/domain/model/` SHALL contain exactly two fields: `SeatId seatId` and `BigDecimal unitPrice`. It SHALL NOT carry any seat class field.

### Scenario: BookedSeat has no seatClass field

- **WHEN** the `BookedSeat` class is examined
- **THEN** it SHALL NOT contain a `seatClass` field or any reference to `SeatClass`

### Scenario: BookedSeat factory method accepts seatId and unitPrice only

- **WHEN** `BookedSeat.of(seatId, unitPrice)` is called
- **THEN** a valid `BookedSeat` instance SHALL be returned with the provided values

## Requirement: BookingController exposes REST endpoints without seatClass in responses

The `BookingController` in `booking/infrastructure/web/` SHALL expose booking endpoints. Booking responses SHALL NOT include a `seatClass` field in any per-seat response object.

### Scenario: POST /api/v1.0/bookings/hold response contains no seatClass

- **WHEN** a `POST /api/v1.0/bookings/hold` request is processed successfully
- **THEN** the response body SHALL contain a `seats` array where each element has `seatId` and `unitPrice` fields but NO `seatClass` field

### Scenario: POST /api/v1.0/bookings/{id}/confirm response contains no seatClass

- **WHEN** a `POST /api/v1.0/bookings/{id}/confirm` request is processed successfully
- **THEN** the response body SHALL contain a `seats` array where each element has `seatId` and `unitPrice` fields but NO `seatClass` field

### Scenario: GET /api/v1.0/bookings/{id} response contains no seatClass

- **WHEN** a `GET /api/v1.0/bookings/{id}` request is processed successfully
- **THEN** the response body SHALL contain a `seats` array where each element has `seatId` and `unitPrice` fields but NO `seatClass` field

# REMOVED Requirements

## Requirement: SeatClass enum defines price multipliers

**Reason**: Pricing is unified to `route.basePrice`. Multiplier-based tiered pricing is removed.

**Migration**: Delete `SeatClass.java`. All references to `SeatClass.ECONOMY`, `SeatClass.BUSINESS`, `SeatClass.FIRST_CLASS`, and `getPriceMultiplier()` must be removed from production code and tests.

## Requirement: Seat domain aggregate carries a seatClass field

**Reason**: With no pricing differentiation and no active seat layout feature, `seatClass` on the `Seat` aggregate is dead weight.

**Migration**: Remove `SeatClass seatClass` field from `Seat.java`. Remove from `CreateSeatCommand`, `UpdateSeatCommand`, `SeatDto`, `CreateSeatHttpRequest`, `UpdateSeatHttpRequest`, `SeatHttpResponse`, `SeatEntityMapper`, `SeatEntity`.

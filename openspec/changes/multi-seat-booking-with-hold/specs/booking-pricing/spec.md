# ADDED Requirements

## Requirement: PricingService calculates per-seat price using route base price and seat class multiplier

A `PricingService` domain service SHALL compute the unit price for a seat as `route.basePrice.multiply(seatClass.getPriceMultiplier())` using `BigDecimal` arithmetic. It SHALL return a `List<BookedSeatPrice>` value object containing `seatId`, `unitPrice`, and `seatClass` for each requested seat. The `totalPrice` is the sum of all `unitPrice` values.

### Scenario: ECONOMY seat price equals base price

- **WHEN** `PricingService.calculate(route, seats)` is called with a route having `basePrice = 500000` and a seat with `seatClass = ECONOMY`
- **THEN** the returned `unitPrice` for that seat SHALL be `500000.00`

### Scenario: BUSINESS seat price is 1.5× base price

- **WHEN** `PricingService.calculate(route, seats)` is called with a route having `basePrice = 500000` and a seat with `seatClass = BUSINESS`
- **THEN** the returned `unitPrice` for that seat SHALL be `750000.00`

### Scenario: FIRST_CLASS seat price is 2.0× base price

- **WHEN** `PricingService.calculate(route, seats)` is called with a route having `basePrice = 500000` and a seat with `seatClass = FIRST_CLASS`
- **THEN** the returned `unitPrice` for that seat SHALL be `1000000.00`

### Scenario: Total price is the sum of all unit prices in the booking

- **WHEN** `PricingService.calculate(route, seats)` is called with seats of mixed classes
- **THEN** the returned `totalPrice` SHALL equal `Σ(unitPrice)` across all seats

## Requirement: SeatClass enum provides a price multiplier for each class

The `SeatClass` enum SHALL expose a `getPriceMultiplier()` method returning a `BigDecimal`. The multipliers SHALL be: `ECONOMY = 1.0`, `BUSINESS = 1.5`, `FIRST_CLASS = 2.0`.

### Scenario: Each SeatClass returns the correct multiplier

- **WHEN** `SeatClass.ECONOMY.getPriceMultiplier()` is called
- **THEN** it SHALL return `BigDecimal("1.0")`

### Scenario: BUSINESS multiplier is 1.5

- **WHEN** `SeatClass.BUSINESS.getPriceMultiplier()` is called
- **THEN** it SHALL return `BigDecimal("1.5")`

### Scenario: FIRST_CLASS multiplier is 2.0

- **WHEN** `SeatClass.FIRST_CLASS.getPriceMultiplier()` is called
- **THEN** it SHALL return `BigDecimal("2.0")`

## Requirement: Price is snapshotted at hold creation time and never recalculated

At the moment `CreateSeatHoldUseCase` successfully creates a hold, the `unitPrice` for each seat SHALL be stored in `booking_seats.price_at_booking` and `booking_seats.seat_class_at_booking`. The `bookings.total_price` SHALL be set to the sum of these values. When `ConfirmSeatHoldUseCase` confirms the booking, it SHALL use the stored `total_price` from the booking — it SHALL NOT recalculate using the current route base price.

### Scenario: Price snapshot is immutable after hold creation

- **WHEN** `route.basePrice` is updated after a hold is created
- **THEN** `ConfirmSeatHoldUseCase` SHALL use the original `price_at_booking` values from `booking_seats`, not the updated route price

### Scenario: booking_seats records store both unit price and seat class

- **WHEN** a hold is created for multiple seats
- **THEN** each row in `booking_seats` SHALL contain `price_at_booking` matching the calculated unit price and `seat_class_at_booking` matching the seat's class at the time of hold creation

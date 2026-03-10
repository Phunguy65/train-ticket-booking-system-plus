# Why

The current pricing model multiplies `route.basePrice` by a per-seat class multiplier (ECONOMY×1.0, BUSINESS×1.5, FIRST_CLASS×2.0), adding complexity to the domain with no current business justification. The system needs to be simplified to a single unified price per route — every seat on a given route costs the same (`route.basePrice`), eliminating the `SeatClass` concept entirely from pricing and persistence.

## What Changes

- **BREAKING**: Remove `seatClass` field from `POST /api/v1.0/trains/{trainId}/seats` request body
- **BREAKING**: Remove `seatClass` field from `PATCH /api/v1.0/seats/{id}` request body
- **BREAKING**: Remove `seatClass` field from all seat and booking API responses (`SeatHttpResponse`, `BookingHttpResponse.BookedSeatResponse`)
- **BREAKING**: Remove `seatClass` enum from OpenAPI contract (`shared/api-contracts/openapi.yaml`)
- Drop `seat_class` column from the `seats` database table (Flyway migration)
- Drop `seat_class_at_booking` column from the `booking_seats` database table (Flyway migration)
- Delete the `SeatClass` enum (`train/domain/model/SeatClass.java`) entirely
- Refactor `PricingService` to use `route.basePrice` directly — no multiplier applied
- Remove `seatClass` field from `Seat` domain aggregate and `BookedSeat` value object
- Remove `seatClass` from all DTOs, commands, mappers, use cases, and controllers

## Capabilities

### New Capabilities

- none

### Modified Capabilities

- `database-schema`: Drop `seats.seat_class` (VARCHAR NOT NULL) and `booking_seats.seat_class_at_booking` (VARCHAR NOT NULL) columns via a new Flyway migration
- `backend-booking-slice`: `PricingService.calculatePrices()` now uses `route.basePrice` as the flat unit price for all seats; `BookedSeat` value object no longer carries `seatClass`; booking API responses no longer include `seatClass` per seat

## Impact

- **Database**: 2 columns dropped across 2 tables (`seats`, `booking_seats`) — new Flyway migration `V8.0.0__drop_seat_class.sql` required
- **Backend — Domain**: `SeatClass.java` enum deleted; `Seat` aggregate and `BookedSeat` value object lose the `seatClass` field
- **Backend — Application**: `PricingService`, 6 use cases, `SeatDto`, `HoldDto`, `CreateSeatCommand`, `UpdateSeatCommand` all updated
- **Backend — Persistence**: `SeatEntity`, `BookingSeatsEntity`, `SeatEntityMapper`, `BookingEntityMapper` updated
- **Backend — Web**: `SeatController`, `BookingController`, `CreateSeatHttpRequest`, `UpdateSeatHttpRequest`, `SeatHttpResponse`, `BookingHttpResponse` updated
- **API Contract**: `openapi.yaml` Seat schema updated — breaking change for any client consuming `seatClass`
- **Tests**: ~13 test files with ~61 references to `SeatClass` enum updated
- **Frontend**: No impact — both frontends (`admin` Next.js, `customer` Kotlin Compose) are skeleton implementations with no seatClass usage

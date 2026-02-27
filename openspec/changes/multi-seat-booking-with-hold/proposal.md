# Why

The current booking system only supports single-seat bookings with hardcoded zero pricing, making it impossible for customers to reserve multiple seats in one transaction. To support group travel and real-world ticketing needs, the backend must be extended to allow multi-seat reservations with a two-phase hold-then-confirm flow and accurate price calculation.

## What Changes

- **BREAKING** — `bookings.seat_id` column is dropped; seat associations move to a new `booking_seats` join table
- **BREAKING** — `BookingStatus` transitions change: `PENDING` is replaced by `HELD`; confirmed bookings move to `CONFIRMED`
- **BREAKING** — `RouteSeatAvailabilityStatus` gains a new `HELD` state; the existing DB CHECK constraint is updated
- **New** — `POST /api/v1.0/bookings/hold` creates a multi-seat hold (15-minute window) using pessimistic locking
- **New** — `POST /api/v1.0/bookings/{id}/confirm` converts a held booking to confirmed after payment
- **New** — `DELETE /api/v1.0/bookings/{id}` (cancel) explicitly releases a held or confirmed booking
- **New** — Price is calculated at hold-creation time and snapshotted per seat in `booking_seats`; never recalculated at confirmation
- **New** — A scheduled job expires stale holds and releases their seats back to `AVAILABLE`
- **New** — Business rule: a user may not create a new hold while they already have an active hold on the same route

## Capabilities

### New Capabilities

- `multi-seat-hold`: Two-phase booking flow — create a 15-minute seat hold for one or more seats with pessimistic locking, then confirm after payment. Includes automatic expiry via scheduled job.
- `booking-pricing`: Price calculation at hold time using `route.basePrice × seatClass.multiplier`; snapshot stored in `booking_seats.price_at_booking` and summed into `bookings.total_price`.

### Modified Capabilities

- `backend-booking-slice`: Booking lifecycle changes from `PENDING → CONFIRMED → CANCELLED` to `HELD → CONFIRMED → CANCELLED`. The aggregate now carries a list of `BookedSeat` value objects instead of a single `SeatId`. Idempotency and domain event mechanics remain.
- `database-schema`: New `booking_seats` table; `bookings.seat_id` dropped; `route_seat_availability.status` CHECK constraint updated to include `HELD`.

## Impact

- **Backend — booking module**: `Booking` aggregate, `BookingStatus` enum, `CreateBookingUseCase` (replaced by `CreateSeatHoldUseCase` + `ConfirmSeatHoldUseCase`), `BookingRepository`, `BookingController`, all persistence/mapping classes
- **Backend — train module**: `RouteSeatAvailabilityStatus` enum, `RouteSeatAvailability` domain entity, `RouteSeatAvailabilityPort` interface and adapter, `RouteSeatAvailabilityJpaRepository`
- **Database**: New migration adds `booking_seats` table, updates seat status constraint, adds partial unique index on `bookings(user_id, route_id) WHERE status = 'HELD'`, drops `bookings.seat_id`
- **API contract**: Request body for hold creation uses `seatIds: List<UUID>` instead of `seatId: UUID`; response includes per-seat price breakdown and `expiresAt` timestamp
- **No frontend changes in scope** — API changes are additive (new endpoints); existing `GET /api/v1.0/bookings/{id}` is preserved

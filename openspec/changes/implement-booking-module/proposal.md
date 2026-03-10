# Why

The system has a fully designed database schema (`bookings`, `route_seat_availability`) and a cross-module port (`RouteSeatAvailabilityPort`) ready for booking operations, but the `booking` module is completely empty. Without this module, users cannot reserve seats, and the seat-hold/expiry lifecycle cannot function. The existing `backend-booking-slice` spec also predates the HELD-status flow and optimistic locking requirements — it needs to be superseded.

## What Changes

- Introduce the `booking` vertical slice module following the established Spring Modulith + DDD pattern
- Implement multi-seat booking in a single atomic transaction (one `Booking` aggregate → multiple seats held via `RouteSeatAvailabilityPort`)
- Implement seat hold with a 15-minute `payment_deadline`; expired holds are released by a background scheduler
- Implement booking cancellation with status-aware seat release (`HELD` → release, `CONFIRMED` → cancel + future refund trigger)
- **BREAKING** (spec update): Replace the existing `backend-booking-slice` spec — the old spec used `PENDING` status and single-seat booking; the new spec uses `HELD/CONFIRMED/CANCELLED` and multi-seat booking with optimistic locking
- Add `@Version` optimistic locking to `RouteSeatAvailabilityEntity` in the `train` module (prerequisite); remove `SELECT FOR UPDATE` from `holdSeats` / `confirmHeldSeats` paths

## Capabilities

### New Capabilities

- `booking-hold`: Multi-seat hold flow — create a `Booking` (status=`HELD`), atomically transition requested seats from `AVAILABLE` → `HELD` via optimistic locking, set `payment_deadline = now + 15 min`, enforce idempotency via `idempotency_key`
- `booking-cancel`: Cancel a booking — release `HELD` seats back to `AVAILABLE`, or mark `CONFIRMED` seats as `CANCELLED` and emit a `BookingCancelled` event carrying `requiresRefund=true` for the future payment module
- `booking-expiry`: Background scheduler (`@Scheduled`, 60 s interval) that finds expired `HELD` bookings and releases their seats, transitioning them back to `AVAILABLE`

### Modified Capabilities

- `backend-booking-slice`: Existing spec uses `PENDING` status, single-seat booking, and `DomainException` throws — all superseded by the new multi-seat, `HELD`-first, `Result`-monad design

## Impact

- **New module**: `backend/src/main/java/.../booking/` (domain, application, infrastructure layers)
- **train module change**: `RouteSeatAvailabilityEntity` gains `@Version Integer version`; `RouteSeatAvailabilityEntityMapper` and domain model updated; `findByRouteIdAndSeatIdsForUpdate` query replaced with plain `findByRouteIdAndSeatIds`; `ConcurrentSeatHoldTest` updated to validate `OptimisticLockException`
- **New REST endpoints**: `POST /api/v1/bookings` (hold), `POST /api/v1/bookings/{id}/cancel`
- **New scheduler**: `BookingExpiryScheduler` — requires `@EnableScheduling` on the application
- **No new migrations**: All required columns (`version`, `booking_id`, `price_at_booking`, `payment_deadline`, `idempotency_key`) already exist in `B1_0_0__baseline.sql`
- **Future dependency**: `BookingCancelled` event with `requiresRefund=true` is the integration point for the payment/refund module

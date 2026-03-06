# ADDED Requirements

## Requirement: ExpireHeldBookingsUseCase releases seats for expired holds

`ExpireHeldBookingsUseCase` SHALL be annotated `@Service` with `@Transactional` on `execute()`. It SHALL:
1. Query `BookingRepository.findExpiredHeldBookings(now)` — all bookings with `status = HELD` and `paymentDeadline < now`
2. For each expired booking:
   a. Call `booking.cancel()` (transitions `HELD → CANCELLED`, registers `BookingCancelled { requiresRefund = false }`)
   b. Call `RouteSeatAvailabilityPort.releaseHeldSeats(routeId, seatIds)`
   c. Persist the updated booking
3. Publish all `BookingCancelled` events

### Scenario: Expired HELD bookings are cancelled and seats released

- **WHEN** `expireHeldBookingsUseCase.execute()` is called and there are bookings with `status = HELD` and `paymentDeadline < now`
- **THEN** each such booking SHALL have `status` changed to `CANCELLED`, all associated seats SHALL transition from `HELD` to `AVAILABLE`, and `BookingCancelled { requiresRefund = false }` SHALL be published for each

### Scenario: No expired bookings results in no changes

- **WHEN** `expireHeldBookingsUseCase.execute()` is called and no bookings have expired
- **THEN** no bookings SHALL be modified and no events SHALL be published

### Scenario: Expiry is idempotent — already-cancelled bookings are not reprocessed

- **WHEN** `expireHeldBookingsUseCase.execute()` is called multiple times for the same expired booking
- **THEN** only the first call SHALL modify the booking; subsequent calls SHALL find no `HELD` bookings with that ID and skip it

## Requirement: BookingExpiryScheduler triggers expiry use case every 60 seconds

`BookingExpiryScheduler` in `booking/infrastructure/scheduler/` SHALL be annotated `@Component` and use `@Scheduled(fixedDelay = 60_000)` to invoke `ExpireHeldBookingsUseCase.execute()`. The application SHALL have `@EnableScheduling` enabled.

### Scenario: Scheduler invokes expiry use case at fixed interval

- **WHEN** the application is running and 60 seconds have elapsed since the last execution
- **THEN** `BookingExpiryScheduler` SHALL invoke `expireHeldBookingsUseCase.execute()` exactly once

### Scenario: Scheduler failure does not crash the application

- **WHEN** `expireHeldBookingsUseCase.execute()` throws an unexpected exception
- **THEN** the exception SHALL be caught and logged; the scheduler SHALL continue running and retry on the next interval

## Requirement: BookingRepository provides findExpiredHeldBookings query

`BookingRepository` SHALL include `List<Booking> findExpiredHeldBookings(Instant now)` returning all bookings with `status = HELD` and `paymentDeadline < now`.

### Scenario: findExpiredHeldBookings returns only expired HELD bookings

- **WHEN** `bookingRepository.findExpiredHeldBookings(Instant.now())` is called
- **THEN** only bookings with `status = HELD` AND `paymentDeadline` strictly before `now` SHALL be returned; active holds and non-HELD bookings SHALL be excluded

## Requirement: Seat availability query treats expired HELD seats as AVAILABLE (lazy expiry)

`RouteSeatAvailabilityRepository.findAvailableByRouteId()` SHALL treat seats as available if `status = AVAILABLE` OR (`status = HELD` AND the associated booking's `payment_deadline < NOW()`). This prevents users from seeing stale holds between scheduler runs.

### Scenario: Expired HELD seat appears as AVAILABLE in availability query

- **WHEN** a seat has `status = HELD` and its booking's `payment_deadline` is in the past
- **THEN** `findAvailableByRouteId()` SHALL include that seat in the results as if it were `AVAILABLE`

### Scenario: Active HELD seat does not appear in availability query

- **WHEN** a seat has `status = HELD` and its booking's `payment_deadline` is in the future
- **THEN** `findAvailableByRouteId()` SHALL NOT include that seat in the results

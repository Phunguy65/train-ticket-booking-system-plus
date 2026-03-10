# ADDED Requirements

## Requirement: CancelBookingUseCase performs status-aware seat release

`CancelBookingUseCase` SHALL be annotated `@Service` with `@Transactional` on `execute()`. It SHALL:
1. Load the booking by ID — return `BookingError.BookingNotFound` if absent
2. Verify the requesting user owns the booking — return `BookingError.Forbidden` if not
3. Call `booking.cancel()` — return failure if status transition is invalid
4. Based on `previousStatus`:
   - `HELD` → call `RouteSeatAvailabilityPort.releaseHeldSeats(routeId, seatIds)`
   - `CONFIRMED` → call `RouteSeatAvailabilityPort.cancelBookedSeats(routeId, seatIds)`
5. Persist the updated booking
6. Publish `BookingCancelled` domain event

### Scenario: Cancelling a HELD booking releases seats back to AVAILABLE

- **WHEN** `cancelBookingUseCase.execute(CancelBookingCommand)` is called for a booking with `status = HELD`
- **THEN** the booking `status` SHALL change to `CANCELLED`, all associated seats SHALL transition from `HELD` to `AVAILABLE` in `route_seat_availability`, and a `BookingCancelled` event with `requiresRefund = false` SHALL be published

### Scenario: Cancelling a CONFIRMED booking marks seats as CANCELLED

- **WHEN** `cancelBookingUseCase.execute(CancelBookingCommand)` is called for a booking with `status = CONFIRMED`
- **THEN** the booking `status` SHALL change to `CANCELLED`, all associated seats SHALL transition from `BOOKED` to `CANCELLED` in `route_seat_availability`, and a `BookingCancelled` event with `requiresRefund = true` SHALL be published

### Scenario: Cancelling an already-CANCELLED booking returns failure

- **WHEN** `cancelBookingUseCase.execute(CancelBookingCommand)` is called for a booking with `status = CANCELLED`
- **THEN** `Result.failure(BookingError.InvalidStatusTransition)` SHALL be returned and no seat status SHALL change

### Scenario: Cancelling a booking owned by a different user returns Forbidden

- **WHEN** `cancelBookingUseCase.execute(CancelBookingCommand)` is called with a `userId` that does not match `booking.userId`
- **THEN** `Result.failure(BookingError.Forbidden)` SHALL be returned

### Scenario: Cancelling a non-existent booking returns BookingNotFound

- **WHEN** `cancelBookingUseCase.execute(CancelBookingCommand)` is called with a `bookingId` that does not exist
- **THEN** `Result.failure(BookingError.BookingNotFound)` SHALL be returned

## Requirement: BookingCancelled domain event carries refund intent

`BookingCancelled` SHALL be a domain event record with fields: `bookingId`, `userId`, `routeId`, `requiresRefund` (boolean). It SHALL be published after the transaction commits via Spring Modulith's event publication mechanism.

### Scenario: BookingCancelled event is published after successful cancellation

- **WHEN** `CancelBookingUseCase` successfully cancels a booking
- **THEN** a `BookingCancelled` event SHALL be published with the correct `bookingId`, `userId`, `routeId`, and `requiresRefund` flag

### Scenario: BookingCancelled with requiresRefund=true is the payment module integration point

- **WHEN** a `CONFIRMED` booking is cancelled and `BookingCancelled { requiresRefund = true }` is published
- **THEN** any registered listener (e.g., future payment module) SHALL receive the event and MAY initiate a refund; the booking module itself SHALL NOT perform any payment operations

## Requirement: BookingController exposes POST /api/v1/bookings/{id}/cancel endpoint

`BookingController` SHALL expose `POST /api/v1/bookings/{id}/cancel`. Authentication is required. The authenticated user's ID SHALL be extracted from the security context and passed to the use case.

### Scenario: POST /api/v1/bookings/{id}/cancel for own HELD booking returns 200

- **WHEN** an authenticated user sends `POST /api/v1/bookings/{id}/cancel` for their own `HELD` booking
- **THEN** the controller SHALL return `200 OK` with a JSend success body

### Scenario: POST /api/v1/bookings/{id}/cancel for another user's booking returns 403

- **WHEN** an authenticated user sends `POST /api/v1/bookings/{id}/cancel` for a booking they do not own
- **THEN** the controller SHALL return `403 Forbidden`

### Scenario: POST /api/v1/bookings/{id}/cancel for unknown booking returns 404

- **WHEN** `POST /api/v1/bookings/{id}/cancel` is sent with a non-existent booking ID
- **THEN** the controller SHALL return `404 Not Found`

### Scenario: POST /api/v1/bookings/{id}/cancel for already-cancelled booking returns 409

- **WHEN** `POST /api/v1/bookings/{id}/cancel` is sent for a booking with `status = CANCELLED`
- **THEN** the controller SHALL return `409 Conflict`

## Requirement: BookingError sealed interface covers all booking failure cases

`BookingError` in `booking/domain/error/` SHALL be a sealed interface with variants: `BookingNotFound`, `SeatNotAvailable`, `ActiveHoldExists`, `InvalidStatusTransition`, `Forbidden`. Each variant SHALL implement `message()` returning a human-readable string.

### Scenario: Each BookingError variant provides a descriptive message

- **WHEN** `new BookingError.BookingNotFound().message()` is called
- **THEN** a non-null, non-empty string describing the error SHALL be returned

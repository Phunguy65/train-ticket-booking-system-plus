# ADDED Requirements

## Requirement: Held seats can be confirmed to booked on payment success

The `RouteSeatAvailabilityPort` SHALL expose a `confirmHeldSeats(bookingId)` method that transitions seats from HELD to BOOKED.

### Scenario: Seats confirmed after payment

- **WHEN** `confirmHeldSeats(bookingId)` is called
- **AND** the booking has seats in HELD status
- **THEN** all seats associated with the booking transition from HELD to BOOKED

### Scenario: Concurrent confirmation handled safely

- **WHEN** `confirmHeldSeats(bookingId)` is called concurrently
- **THEN** optimistic locking (`@Version`) prevents double-confirmation
- **THEN** the losing transaction throws `OptimisticLockException`

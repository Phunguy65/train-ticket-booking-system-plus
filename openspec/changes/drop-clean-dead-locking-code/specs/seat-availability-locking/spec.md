# ADDED Requirements

## Requirement: Seat availability uses pessimistic-only locking

The system SHALL protect `route_seat_availability` rows exclusively via pessimistic database locking (`SELECT … FOR UPDATE NOWAIT`). No optimistic locking (`@Version` / version column) SHALL be present on this table. All write operations — hold, confirm, cancel, release — MUST acquire a row-level exclusive lock before reading or mutating seat state.

### Scenario: Concurrent hold requests for the same seat are serialised

- **WHEN** two concurrent transactions both attempt to hold the same seat on the same route
- **THEN** exactly one transaction acquires the lock and succeeds
- **THEN** the other transaction is blocked until the first commits, then proceeds with the updated seat state (and may fail with a seat-unavailable error)

### Scenario: Lock timeout prevents indefinite blocking

- **WHEN** a transaction cannot acquire the seat lock within 3000 milliseconds
- **THEN** the system returns a lock-timeout error (HTTP 409 or appropriate domain error) without modifying any seat state

### Scenario: No version column exists on route_seat_availability

- **WHEN** the database schema is inspected after migration
- **THEN** the `route_seat_availability` table SHALL NOT contain a `version` column

## Requirement: Dead optimistic-locking exception handler is removed

The system SHALL NOT register a handler for `ObjectOptimisticLockingFailureException` in `GlobalExceptionHandler`. Because pessimistic locking prevents all concurrent modifications to seat availability rows, this exception class can never be thrown by the seat availability write path.

### Scenario: GlobalExceptionHandler has no optimistic lock handler

- **WHEN** the `GlobalExceptionHandler` class is inspected
- **THEN** no method handling `ObjectOptimisticLockingFailureException` SHALL exist

## Requirement: Concurrent hold test validates pessimistic locking

The system SHALL have an integration test that concurrently attempts to hold the same seat from two threads and asserts that pessimistic locking alone enforces mutual exclusion without any version column.

### Scenario: Concurrent hold — exactly one succeeds

- **WHEN** two threads simultaneously submit hold requests for the same seat on the same route
- **THEN** exactly one hold succeeds
- **THEN** the other receives a seat-unavailable or lock-timeout result
- **THEN** no data inconsistency (double-hold) exists in `route_seat_availability` after both threads complete

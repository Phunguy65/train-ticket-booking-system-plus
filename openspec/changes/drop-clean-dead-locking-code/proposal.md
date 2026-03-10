# Why

The `route_seat_availability` table carries a redundant `@Version` column (optimistic locking) that was introduced in V4.0.0 but became dead code when pessimistic locking (`SELECT FOR UPDATE`) was added in the multi-seat booking change. Because pessimistic locking fully serialises concurrent seat writes at the database level, the `@Version` check at commit time can never detect a conflict — the `ObjectOptimisticLockingFailureException` handler in `GlobalExceptionHandler` is therefore unreachable. Keeping both locking strategies creates misleading documentation, maintenance confusion, and unnecessary overhead on every UPDATE statement.

## What Changes

- **Remove** `@Version` annotation from `RouteSeatAvailabilityEntity` (stops JPA from appending `WHERE version = ?` on every UPDATE).
- **Remove** `version` field and its getter/setter from `RouteSeatAvailabilityEntity` and the domain model `RouteSeatAvailability`.
- **Remove** `version` field mapping from `RouteSeatAvailabilityEntityMapper`.
- **Remove** `handleOptimisticLock()` handler from `GlobalExceptionHandler` (dead code — now provably unreachable).
- **Add** a database migration to drop the `version` column from `route_seat_availability`.
- **Update** unit/integration tests that assert on `version` values (`RouteSeatAvailabilityTest`, `RouteSeatAvailabilityRepositoryAdapterTest`).
- **Add** a concurrent-hold integration test that proves pessimistic locking alone prevents double-booking (replaces the theoretical safety net that `@Version` used to provide).

## Capabilities

### New Capabilities

- `seat-availability-locking`: Formalises the single-strategy (pessimistic-only) concurrency contract for `route_seat_availability` — documents that `SELECT FOR UPDATE` with deadlock-safe ordering is the sole protection mechanism.

### Modified Capabilities

<!-- No existing spec-level behaviour changes — idempotency_key in bookings is unchanged and intentionally kept. -->

## Impact

- **Backend – persistence layer**: `RouteSeatAvailabilityEntity`, `RouteSeatAvailabilityEntityMapper`, `RouteSeatAvailability` domain model.
- **Backend – shared infrastructure**: `GlobalExceptionHandler` (remove dead `handleOptimisticLock` handler).
- **Database**: New Flyway migration to `DROP COLUMN version` from `route_seat_availability`.
- **Tests**: `RouteSeatAvailabilityTest`, `RouteSeatAvailabilityRepositoryAdapterTest`, `BookingControllerTest` — remove version assertions; add concurrency integration test.
- **No API contract changes** — version is an internal implementation detail never exposed to clients.
- **No impact on `bookings` table or `idempotency_key`** — idempotency key solves a different problem (client-retry deduplication) and remains essential.

# Tasks

## 1. Remove `version` from Domain Model

- [x] 1.1 Remove `version` field, constructor parameter, getter, and `create()` / `reconstitute()` usages from `RouteSeatAvailability.java`
- [x] 1.2 Remove `version` field, `@Version` annotation, getter, and setter from `RouteSeatAvailabilityEntity.java`
- [x] 1.3 Remove `version` read (`entity.getVersion()`) and write (`entity.setVersion(...)`) from `RouteSeatAvailabilityEntityMapper.java`

## 2. Remove Dead Exception Handler

- [x] 2.1 Remove `handleOptimisticLock(ObjectOptimisticLockingFailureException)` method from `GlobalExceptionHandler.java`
- [x] 2.2 Remove any now-unused `import` statements in `GlobalExceptionHandler.java` (e.g., `ObjectOptimisticLockingFailureException`)

## 3. Database Migration

- [x] 3.1 Add a new Flyway migration file (e.g., `V{next}__drop_seat_availability_version_column.sql`) with:
  ```sql
  ALTER TABLE route_seat_availability DROP COLUMN version;
  ```
- [x] 3.2 Verify the migration version number does not conflict with existing migrations in `database/migrations/`

## 4. Fix Broken Tests

- [x] 4.1 Remove `assertThat(availability.getVersion()).isEqualTo(0)` assertion from `RouteSeatAvailabilityTest.java`
- [x] 4.2 Remove `assertThat(availability.getVersion()).isEqualTo(3)` assertion from `RouteSeatAvailabilityTest.java`
- [x] 4.3 Remove any `version`-related setup or assertions in `RouteSeatAvailabilityRepositoryAdapterTest.java`
- [x] 4.4 Verify `BookingControllerTest.java` compiles and all existing locking-related tests still pass

## 5. Add Concurrent-Hold Integration Test

- [x] 5.1 Add a `@DataJpaTest` or `@SpringBootTest` integration test class (e.g., `ConcurrentSeatHoldTest.java`) under the `train` module's test infrastructure
- [x] 5.2 Implement a test that spawns two threads both calling `holdSeats()` for the same route + seat combination simultaneously
- [x] 5.3 Assert that exactly one thread's hold succeeds and the other receives a lock-timeout or seat-unavailable error
- [x] 5.4 Assert that `route_seat_availability` contains no double-hold inconsistency after both threads complete

## 6. Verify & Build

- [x] 6.1 Run `./gradlew :backend:compileJava` — confirm zero compilation errors
- [x] 6.2 Run `./gradlew :backend:test` — confirm all tests pass (including the new concurrency test)
- [x] 6.3 Run `./gradlew :backend:test --tests "*.RouteSeatAvailabilityTest"` to verify domain model tests pass without version assertions
- [ ] 6.4 Confirm Flyway migration applies cleanly in a local `docker compose up` environment

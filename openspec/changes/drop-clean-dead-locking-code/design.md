# Context

The `route_seat_availability` table was originally protected by JPA optimistic locking (`@Version`). When multi-seat booking was introduced, pessimistic locking (`SELECT … FOR UPDATE NOWAIT`) was layered on top to prevent partial-hold leaks across multiple seats. The `@Version` annotation was never removed, leaving both mechanisms active simultaneously.

**Current dual-locking state:**
```
Every write path (hold / confirm / cancel / release):
  1. SELECT … FOR UPDATE NOWAIT   ← acquires DB row lock, blocks all concurrent txs
  2. Domain logic executes
  3. UPDATE … WHERE version = X   ← always succeeds; lock already prevents any conflict
```

Because the pessimistic lock fully serialises all writers, the optimistic version check can never detect a conflict. The `ObjectOptimisticLockingFailureException` handler in `GlobalExceptionHandler` is provably unreachable. The `idempotency_key` column in `bookings` is intentionally **out of scope** — it solves a different problem (client-retry deduplication at the API layer) and is actively used.

## Goals / Non-Goals

**Goals:**

- Remove `@Version` annotation and the `version` field from `RouteSeatAvailabilityEntity` and the `RouteSeatAvailability` domain model.
- Remove the corresponding mapper code in `RouteSeatAvailabilityEntityMapper`.
- Remove the dead `handleOptimisticLock` exception handler from `GlobalExceptionHandler`.
- Add a Flyway migration to `DROP COLUMN version` from `route_seat_availability`.
- Update all tests that assert on `version` values.
- Add a concurrent-hold integration test that demonstrates pessimistic locking alone is sufficient.

**Non-Goals:**

- Changing any locking strategy for any other table.
- Touching `idempotency_key` in `bookings` — explicitly kept.
- Altering the `bookings` hold / confirm / cancel flow.
- Any frontend or API contract change.

## Decisions

### D1 — Remove `@Version` from entity (not just suppress it)

**Decision:** Delete the `@Version` field entirely rather than keeping it as an un-annotated column.

**Rationale:** Keeping the field without `@Version` would leave an orphaned column in the domain model with no clear purpose, creating future confusion. Full removal makes the intent unambiguous. The DB column is dropped via a separate migration step (see D3).

**Alternative considered:** Keep `@Version` as a "defence-in-depth" safety net.
- **Rejected:** Pessimistic locking (`FOR UPDATE NOWAIT`) already serialises all writers at the database level. A concurrent writer cannot modify the row between our read and commit — so the version check is mathematically guaranteed never to fire. Retaining dead safety nets that cannot trigger adds noise and misleads readers of the code.

### D2 — Remove `handleOptimisticLock` from `GlobalExceptionHandler`

**Decision:** Delete the handler entirely rather than documenting it as defensive code.

**Rationale:** Keeping dead exception handlers sends a false signal to future developers that `ObjectOptimisticLockingFailureException` is a real runtime possibility. Removing it makes the actual error-handling surface smaller and easier to reason about.

**Alternative considered:** Keep handler with a comment "defensive code — should not occur".
- **Rejected:** Defensive comments rot. The correct signal is absence of the handler.

### D3 — Flyway migration to DROP COLUMN

**Decision:** Add a new migration `V{next}__drop_seat_availability_version_column.sql` that runs `ALTER TABLE route_seat_availability DROP COLUMN version`.

**Rationale:** Keeping the column after removing the JPA annotation leaves dead schema that wastes storage on every row and confuses future engineers reading the DDL. Dropping it completes the cleanup.

**Alternative considered:** Leave the column, just remove the annotation.
- **Rejected:** The column has no remaining purpose once `@Version` is gone. Deferred schema cleanup tends to never happen.

### D4 — Add concurrent-hold integration test

**Decision:** Add a `@DataJpaTest` or `@SpringBootTest` integration test that spawns two threads both attempting to hold the same seat simultaneously, and asserts that exactly one succeeds and the other receives a lock-timeout or booking-conflict result.

**Rationale:** After removing `@Version`, the only concurrency guarantee is pessimistic locking. An executable test documents and continuously validates this guarantee, replacing the implicit safety net that `@Version` used to represent.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Flyway migration is irreversible on production without a restore | Run on staging first; the column has no business value, so rollback is only needed if migration itself fails (not a data concern) |
| A future code path added without pessimistic lock would silently lose the version guard | Enforce via code review + the new integration test (it would surface missing locks) |
| Tests referencing `version` field will fail to compile after removal | Identified and scoped in tasks — fix in same PR |

## Migration Plan

1. **Code change** — Remove `@Version`, `version` field, mapper code, exception handler in a single PR.
2. **Test update** — Fix or remove all version assertions; add concurrent-hold test.
3. **Schema migration** — Add Flyway `DROP COLUMN` migration file in the same PR (Flyway runs on application startup; CI will execute it against the test database automatically).
4. **Deploy** — Standard deploy. Migration runs automatically on first startup. No downtime needed (dropping a non-nullable integer column with no FK is near-instant in PostgreSQL).
5. **Rollback** — Re-add column + annotation + migration; restore from backup only if migration corrupts data (not a concern here).

## Open Questions

- None — the analysis is complete and the approach is unambiguous.

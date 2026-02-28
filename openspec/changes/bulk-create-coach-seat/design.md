# Context

The train module already has `BulkSoftDeleteCoachesUseCase` and `BulkSoftDeleteSeatsUseCase` as reference patterns. Single-create use cases (`CreateCoachUseCase`, `CreateSeatUseCase`) exist and follow validation-first → persist flow. Repository adapters use `save()` (single); `saveAll()` exists only on `RouteSeatAvailabilityRepositoryAdapter`. The JPA layer (`CoachJpaRepository`, `SeatJpaRepository`) inherits `saveAll()` from `JpaRepository<T, UUID>` — no JPA changes needed.

A minor pre-existing bug: `CreateSeatUseCase` returns `SeatError.TrainNotFound` when the coach is missing. This will be fixed as part of this change.

## Goals / Non-Goals

**Goals:**

- Atomic bulk-create endpoints for coaches and seats (fail-all semantics)
- Three-gate validation: parent exists → in-request duplicates → DB conflicts
- Efficient batch insert via `saveAll()` propagated through domain repo interface and adapter
- Descriptive error responses identifying the specific conflicting values
- Fix `SeatError.CoachNotFound` semantic bug in `CreateSeatUseCase`

**Non-Goals:**

- Partial-success / 207 Multi-Status responses
- Frontend / UI changes
- Async or queued bulk processing
- Changes to existing single-create endpoints

## Decisions

### 1. Endpoint URL: `:bulkCreate` custom-method suffix

**Decision:** `POST /{version}/trains/{trainId}/coaches:bulkCreate` and `POST /{version}/coaches/{coachId}/seats:bulkCreate`

**Rationale:** Follows Google AIP-131 custom method convention. Keeps the resource path (`/coaches`) clean and avoids collision with a hypothetical `/coaches/bulk` sub-resource. Spring MVC with `PathPatternParser` (Spring 6 default) handles the `:` suffix correctly.

**Alternatives considered:**
- `/coaches/bulk` — ambiguous; treats "bulk" as a sub-resource
- `/bulk-coaches` — non-RESTful, abandons resource hierarchy
- `?bulk=true` on the existing POST — conflates single and bulk semantics in one endpoint

---

### 2. Error semantics: Fail-all (atomic)

**Decision:** All validation gates must pass before any insert is attempted. A single failure rejects the entire request.

**Rationale:** Consistent with existing `BulkSoftDelete*UseCase` patterns in the codebase. Simpler transaction management (single `@Transactional` boundary). Clients get a clear contract: either all items are created, or none are.

**Alternatives considered:**
- Partial success (207) — no precedent in this codebase; complicates error reporting and client retry logic

---

### 3. Validation gate order

**Decision:** Gate 1 (parent exists) → Gate 2 (in-request duplicates) → Gate 3 (DB conflicts)

**Rationale:** Gate 2 must precede Gate 3. If two items in the same request share the same `carNumber`, a `findByTrainId()` check would see no conflict (carNumber not yet in DB), pass Gate 3, then crash on the DB unique constraint at insert time — producing an uncontrolled `DataIntegrityViolationException`. Detecting in-request duplicates first gives a clean domain error instead.

Gate 3 uses a **single batch query** (`findByTrainId()` / `findByCoachId()`) to load all existing records and compares in-memory with a `Set`, rather than calling `existsByTrainIdAndCarNumber()` per item (N queries). This avoids N+1 on large batches.

---

### 4. Persistence: `saveAll()` added to domain repository interfaces

**Decision:** Add `saveAll(List<T>) → List<T>` to `CoachRepository` and `SeatRepository` interfaces, implemented in their respective adapters by delegating to the JPA `saveAll()` (already inherited from `JpaRepository<T, UUID>`).

**Rationale:** Keeps the domain-repo contract explicit and testable. The JPA layer needs zero changes. Mirrors the existing `RouteSeatAvailabilityRepository.saveAll()` pattern exactly.

**Alternatives considered:**
- Loop `save()` per item — simpler but loses Hibernate batch-insert optimisation. With `spring.jpa.properties.hibernate.jdbc.batch_size=50` the batch insert consolidates round-trips automatically; `saveAll()` is the idiomatic way to enable it.

---

### 5. Request shape for seats: `List<{seatNumber: String}>` (Option B)

**Decision:** Seat bulk request wraps seat numbers in objects: `{ "seats": [{ "seatNumber": "1A" }, ...] }`, mirroring the coach request shape `{ "coaches": [{ "carNumber": 1, "totalSeats": 64 }, ...] }`.

**Rationale:** Uniform structure across both endpoints. Easier to extend if `Seat` gains additional fields (e.g., `seatType`) in the future without a breaking API change.

**Alternatives considered:**
- `{ "seatNumbers": ["1A", "1B"] }` — more compact but breaks symmetry with coaches and is not forward-compatible.

---

### 6. New error variants on sealed interfaces

**CoachError additions:**
- `CarNumbersAlreadyExist(List<Integer> conflictingCarNumbers)` — for Gate 3 failures
- `DuplicateCarNumbersInRequest(List<Integer> duplicates)` — for Gate 2 failures

**SeatError additions:**
- `SeatNumbersAlreadyExist(List<String> conflictingNumbers)` — Gate 3
- `DuplicateSeatNumbersInRequest(List<String> duplicates)` — Gate 2
- `CoachNotFound()` — replaces misuse of `TrainNotFound` in `CreateSeatUseCase`

**HTTP mapping:**
- `CarNumbersAlreadyExist` / `SeatNumbersAlreadyExist` → 409 Conflict, response includes the conflicting values list
- `DuplicateCarNumbersInRequest` / `DuplicateSeatNumbersInRequest` → 422 Unprocessable Entity
- `CoachNotFound` → 404 Not Found

---

### 7. Max items per request: 100

**Decision:** Both endpoints reject requests with more than 100 items (`@Size(max = 100)` on the list).

**Rationale:** Consistent with the 100-item cap enforced by `bulkDelete` in `CoachController`. Prevents accidental memory exhaustion from unbounded lists.

## Risks / Trade-offs

- **Race condition on uniqueness** — Two concurrent requests for the same `carNumber` could both pass Gate 3 (they both see the same DB state before either commits) and then one fails with a `DataIntegrityViolationException` at the JPA layer. Mitigation: the DB unique constraint (enforced at the schema level) acts as the final safety net. The `GlobalExceptionHandler` should catch `DataIntegrityViolationException` and return 409; if it doesn't already, the controller-level `coachErrorResponse` / `seatErrorResponse` methods can be extended to handle it. For an admin-only endpoint the probability of simultaneous conflict is low.

- **`findByTrainId()` loads all coaches** — For a train with many coaches, this loads all records into memory for the in-memory Set comparison. Given trains typically have ≤30 coaches and seats ≤100 per coach, this is acceptable. If scale requirements change, a targeted `findCarNumbersByTrainIdIn(Set<Integer>)` query could replace it.

- **No domain events on bulk create** — Consistent with the existing `CreateCoachUseCase` and `CreateSeatUseCase` (neither publish domain events). Coaches and seats are reference data; downstream systems react to bookings, not coach/seat creation. If event publishing is added later, the bulk use cases would need a per-item loop (same pattern as `BulkSoftDeleteCoachesUseCase`).

## Open Questions

*(none — all decisions resolved during exploration)*

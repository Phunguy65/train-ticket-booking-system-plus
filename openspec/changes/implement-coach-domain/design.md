# Context

The current seat hierarchy is `trains (1) → (N) seats`. Seats reference their parent train via `trainId`. The business domain requires an intermediate layer — a **Coach** (toa tàu) — so the hierarchy becomes `trains (1) → (N) coaches (1) → (N) seats`.

The database migration `V12_0_0__add_train_cars_table.sql` has already been written and introduces the `train_cars` table (physical name kept as-is). This change delivers the Java domain model and persistence infrastructure to match that schema, and establishes the term **Coach** (instead of TrainCar) as the canonical name in code.

Existing patterns in the `train` module are well-established and serve as the direct template:
- `Seat` / `SeatId` → `Coach` / `CoachId`
- `SeatEntity` → `CoachEntity`
- `SeatJpaRepository` → `CoachJpaRepository`
- `SeatEntityMapper` → `CoachEntityMapper`
- `SeatRepositoryAdapter` → `CoachRepositoryAdapter`
- `SeatRepository` (domain interface) → `CoachRepository`

## Goals / Non-Goals

**Goals:**

- Introduce `Coach` aggregate root (`Coach.java`, `CoachId.java`) in `train/domain/model/`
- Introduce `CoachRepository` interface in `train/domain/repository/`
- Introduce domain events `CoachCreated` and `CoachDeleted` in `train/domain/event/`
- Implement full persistence stack: `CoachEntity`, `CoachJpaRepository`, `CoachEntityMapper`, `CoachRepositoryAdapter`
- Update `Seat` domain: replace `trainId: TrainId` field with `coachId: CoachId`
- Update `Seat` persistence: replace `train_id` column mapping with `train_car_id`, update mapper and repository adapter
- Rename migration file from `V12_0_0__add_train_cars_table.sql` to `V12_0_0__add_coaches_table.sql` for consistency

**Non-Goals:**

- No new REST API endpoints or HTTP controllers for Coach
- No application use-case layer (no `CreateCoachUseCase`, etc.) — that is a follow-up change
- No changes to `Booking` or `Route` modules
- No changes to the physical DB table name (`train_cars` stays as-is)
- No `Coach` → `Train` bidirectional navigation in domain (Coach holds `trainId` reference only)

## Decisions

### Decision 1: Coach as AggregateRoot (not entity within Train aggregate)

**Choice:** `Coach extends AggregateRoot<CoachId>`

**Rationale:** Consistent with how `Seat` is modelled — as an independent aggregate root with a `trainId` reference, not an embedded entity inside `Train`. The `train_cars` schema has its own PK and lifecycle (soft delete), making it a natural aggregate boundary. Loading a Train would otherwise force eager loading of all its coaches and seats.

**Alternative considered:** Embed `List<Coach>` inside `Train` aggregate. Rejected because it creates a large aggregate boundary, performance issues on load, and breaks the existing pattern.

### Decision 2: Coach does NOT emit a domain event on `create()`

**Choice:** `Coach.create()` does not call `registerEvent(...)`. `Coach.softDelete()` emits `CoachDeleted`.

**Rationale:** Mirrors the `Seat` pattern — seats are administrative reference data that don't need event-driven side effects on creation. Coaches are created when configuring a train's physical composition; no downstream consumer needs to react at creation time yet.

**Alternative considered:** Emit `CoachCreated` event. Deferred — can be added when a consumer exists.

### Decision 3: Physical DB table name stays `train_cars`

**Choice:** `@Table(name = "train_cars")` in `CoachEntity.java`.

**Rationale:** The V12 migration is already written with `train_cars`. Renaming the table would require a new migration, breaking zero-downtime guarantees. The Java class name `CoachEntity` provides the clean naming while the DB column remains `train_cars`.

**Alternative considered:** Add a separate migration to rename the table to `coaches`. Unnecessary complexity — the table name is an infrastructure detail.

### Decision 4: `Seat.trainId` → `Seat.coachId` in domain model

**Choice:** Replace `TrainId trainId` with `CoachId coachId` in `Seat.java`. Update all factory methods, getters, and the repository interface (`findByTrainId` → `findByCoachId`).

**Rationale:** After V12 migration, seats no longer have a `train_id` column. The domain model must reflect this. `Seat.coachId` is the single FK that connects the seat to its physical location.

**Impact:** `SeatRepository.findByTrainId(TrainId)` is replaced by `findByCoachId(CoachId)`. Any existing callers (application use cases) must be updated.

### Decision 5: Migration file rename

**Choice:** Rename `V12_0_0__add_train_cars_table.sql` → `V12_0_0__add_coaches_table.sql`.

**Rationale:** The file has not been executed yet (it's the next pending migration). Renaming before execution keeps the naming consistent with `Coach` terminology in code without affecting the SQL content.

**Constraint:** If the migration has already run in any environment, this rename must NOT happen — Flyway tracks by checksum and filename. Confirm before renaming.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `SeatRepository.findByTrainId` callers break when signature changes to `findByCoachId` | Audit all call sites in `train/application/` before removing; update in the same PR |
| V12 migration filename rename breaks Flyway on environments where it already ran | Check `flyway_schema_history` table before renaming; keep old name if already applied |
| `@DataJpaTest` for `SeatRepositoryAdapterTest` requires Coach as FK dependency | Update test setup: create a `Coach` before creating `Seat` in `@BeforeEach` |
| ArchUnit tests may flag cross-module dependencies | No cross-module access added; Coach and Seat remain in same `train` module |

## Migration Plan

Execution is sequential within the same change:

```
Step 1  Add Coach domain files (new code, no breakage)
           Coach.java, CoachId.java, CoachRepository.java
           CoachCreated.java, CoachDeleted.java

Step 2  Add Coach persistence files (new code, no breakage)
           CoachEntity.java, CoachJpaRepository.java
           CoachEntityMapper.java, CoachRepositoryAdapter.java

Step 3  Rename migration file (if not yet applied)
           V12_0_0__add_train_cars_table.sql → V12_0_0__add_coaches_table.sql

Step 4  Update Seat domain (breaking within module)
           Seat.java: trainId → coachId (CoachId)
           SeatId.java: no change
           SeatRepository.java: findByTrainId → findByCoachId

Step 5  Update Seat persistence (follows domain change)
           SeatEntity.java: trainId column → trainCarId (maps to train_car_id)
           SeatEntityMapper.java: update mapping
           SeatRepositoryAdapter.java: update method signatures

Step 6  Fix all broken call sites in application layer
           Any use case calling seatRepository.findByTrainId(...)

Step 7  Update / add tests
           CoachRepositoryAdapterTest.java (new)
           SeatRepositoryAdapterTest.java (update BeforeEach to create Coach)
```

**Rollback:** Steps 1–2 are additive. Steps 4–6 are the breaking window. If rollback needed, revert Steps 4–6 together; the Coach infrastructure remains harmlessly unused.

## Open Questions

1. Should `CoachCreated` domain event be emitted in `Coach.create()` now, or deferred until there is a consumer? (Current decision: defer — follow Decision 2.)
2. Are there any existing seeds or test fixtures that create `Seat` with `trainId` that need updating outside the main source tree?
3. Is the V12 migration already applied in staging/production? Determines whether the file rename is safe.

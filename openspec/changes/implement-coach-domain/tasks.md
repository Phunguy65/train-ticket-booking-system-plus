# Tasks

## 1. Database Migration Rename

- [x] 1.1 Rename `database/migrations/V12_0_0__add_train_cars_table.sql` → `database/migrations/V12_0_0__add_coaches_table.sql` (only if the migration has not been applied in any environment; verify `flyway_schema_history` first)

## 2. Coach Domain Layer

- [x] 2.1 Create `CoachId.java` — immutable `record` wrapping `UUID`, static `of(UUID)` factory, null guard, `toString()` returning `value.toString()` in `train/domain/model/`
- [x] 2.2 Create `Coach.java` — extends `AggregateRoot<CoachId>`, fields: `id`, `trainId` (TrainId), `carNumber` (int), `totalSeats` (int), `createdAt` (Instant), `deletedAt` (Instant, mutable); static factory `create(id, trainId, carNumber, totalSeats)` and `reconstitute(id, trainId, carNumber, totalSeats, createdAt, deletedAt)`; `softDelete()` emits `CoachDeleted`; `isDeleted()` predicate in `train/domain/model/`
- [x] 2.3 Create `CoachDeleted.java` domain event — carries `CoachId`, static `of(CoachId)` factory in `train/domain/event/`
- [x] 2.4 Create `CoachRepository.java` domain interface in `train/domain/repository/` with methods: `save(Coach)`, `findById(CoachId)`, `findByTrainId(TrainId)`, `existsByTrainIdAndCarNumber(TrainId, int)`, `softDeleteById(CoachId, Instant)`

## 3. Coach Persistence Layer

- [x] 3.1 Create `CoachEntity.java` — `@Entity @Table(name = "train_cars")`, package-private, fields matching DB columns (`id` UUID, `trainId` UUID → `train_id`, `carNumber` Integer → `car_number`, `totalSeats` Integer → `total_seats`, `createdAt` Instant, `deletedAt` Instant), protected no-arg constructor, package-private getters/setters in `train/infrastructure/persistence/`
- [x] 3.2 Create `CoachJpaRepository.java` — package-private, extends `JpaRepository<CoachEntity, UUID>`; custom JPQL: `findActiveById` (filter `deletedAt IS NULL`), `findAllActiveByTrainId` (list by `trainId` where `deletedAt IS NULL`, ordered by `carNumber`), `existsByTrainIdAndCarNumberAndDeletedAtIsNull`, `@Modifying softDeleteById(@Param id, @Param deletedAt)` in `train/infrastructure/persistence/`
- [x] 3.3 Create `CoachEntityMapper.java` — `@Component`, package-private, `toDomain(CoachEntity)` calls `Coach.reconstitute(...)`, `toEntity(Coach)` creates new entity and unwraps value objects in `train/infrastructure/persistence/`
- [x] 3.4 Create `CoachRepositoryAdapter.java` — `@Repository`, package-private, implements `CoachRepository`, delegates to `CoachJpaRepository` + `CoachEntityMapper` in `train/infrastructure/persistence/`

## 4. Update Seat Domain

- [x] 4.1 Update `Seat.java` — replace `trainId: TrainId` with `coachId: CoachId`; update constructor, `create(id, coachId, seatNumber)`, `reconstitute(id, coachId, seatNumber, createdAt, deletedAt)`, and `getCoachId()` getter; remove `getTrainId()`
- [x] 4.2 Update `SeatRepository.java` — replace `findByTrainId(TrainId)` with `findByCoachId(CoachId)`; replace `existsByTrainIdAndSeatNumber(TrainId, String)` with `existsByCoachIdAndSeatNumber(CoachId, String)`

## 5. Update Seat Persistence Layer

- [x] 5.1 Update `SeatEntity.java` — replace `trainId` UUID field (`@Column(name = "train_id")`) with `trainCarId` UUID field (`@Column(name = "train_car_id")`)
- [x] 5.2 Update `SeatEntityMapper.java` — update `toDomain` to pass `CoachId.of(entity.getTrainCarId())` and `toEntity` to call `entity.setTrainCarId(seat.getCoachId().value())`
- [x] 5.3 Update `SeatRepositoryAdapter.java` — replace `findByTrainId` delegation with `findByTrainCarId`; replace `existsByTrainIdAndSeatNumber` with `existsByTrainCarIdAndSeatNumber`
- [x] 5.4 Update `SeatJpaRepository.java` — rename query method `findByTrainId(UUID)` → `findByTrainCarId(UUID)`; update `existsByTrainIdAndSeatNumber` → `existsByTrainCarIdAndSeatNumber`; update `softDeleteByIds` if it filters by `trainId`

## 6. Fix Application Layer Call Sites

- [x] 6.1 Audit all use cases in `train/application/usecase/` that call `seatRepository.findByTrainId(...)` or `seatRepository.existsByTrainIdAndSeatNumber(...)` and update them to use `coachId`-based signatures
- [x] 6.2 Update any DTOs or commands that carry `trainId` for seat-related operations to carry `coachId` instead (if applicable in this change scope)

## 7. Tests

- [x] 7.1 Create `CoachTest.java` — pure JUnit 5 unit tests: `create()` sets correct fields, `softDelete()` emits `CoachDeleted` event, `softDelete()` is idempotent, `reconstitute()` registers no events in `train/domain/model/`
- [x] 7.2 Create `CoachRepositoryAdapterTest.java` — `@DataJpaTest` + `@Import({CoachRepositoryAdapter.class, CoachEntityMapper.class, TrainRepositoryAdapter.class, TrainEntityMapper.class})`; setup creates a Train in `@BeforeEach`; tests: `save`, `findById`, `findByTrainId`, `existsByTrainIdAndCarNumber`, `softDeleteById` (active-only filter) in `train/infrastructure/persistence/`
- [x] 7.3 Update `SeatRepositoryAdapterTest.java` — change `@BeforeEach` to create a `Coach` (not just a Train) before creating seats; update all seat creation calls to use `coachId` instead of `trainId`
- [x] 7.4 Update `SeatTest.java` (if it exists) — update `create()` and `reconstitute()` calls to use `CoachId` instead of `TrainId`

# ADDED Requirements

## Requirement: CoachEntity JPA mapping

The system SHALL provide a `CoachEntity` JPA entity class in `train/infrastructure/persistence/` that maps to the `train_cars` table. The class SHALL be package-private with a protected no-arg constructor, use `@Entity @Table(name = "train_cars")`, and declare explicit `@Column(name = ...)` annotations for all fields: `id` (UUID), `trainId` (UUID, maps to `train_id`), `carNumber` (Integer, maps to `car_number`), `totalSeats` (Integer, maps to `total_seats`), `createdAt` (Instant), and `deletedAt` (Instant, nullable).

### Scenario: CoachEntity maps to train_cars table

- **WHEN** `CoachEntity.java` is examined
- **THEN** `@Table(name = "train_cars")` is present and each field has a `@Column(name = ...)` matching the DB column name

### Scenario: CoachEntity has no JPA annotations in domain

- **WHEN** `Coach.java` (domain model) is examined
- **THEN** it contains no Jakarta Persistence annotations

## Requirement: CoachJpaRepository

The system SHALL provide a `CoachJpaRepository` interface in `train/infrastructure/persistence/` that extends `JpaRepository<CoachEntity, UUID>`. It SHALL be package-private and declare custom JPQL queries for: `findActiveById` (filters `deletedAt IS NULL`), `findAllActiveByTrainId` (returns list filtered by `train_id` and `deletedAt IS NULL`), `existsByTrainIdAndCarNumber` (active-only uniqueness check), and `softDeleteById` (bulk update of `deletedAt`).

### Scenario: Active-only query filters soft-deleted coaches

- **WHEN** `coachJpaRepository.findActiveById(uuid)` is called for a soft-deleted coach
- **THEN** the result is `Optional.empty()`

### Scenario: findAllActiveByTrainId returns only active coaches

- **WHEN** a train has 3 coaches and 1 is soft-deleted
- **THEN** `coachJpaRepository.findAllActiveByTrainId(trainId)` returns 2 coaches

## Requirement: CoachEntityMapper

The system SHALL provide a `CoachEntityMapper` `@Component` in `train/infrastructure/persistence/` with two stateless mapping methods:
- `toDomain(CoachEntity)` → calls `Coach.reconstitute(...)` and unwraps value objects
- `toEntity(Coach)` → creates a new `CoachEntity` and sets all fields by unwrapping value objects (`.value()`)

### Scenario: toDomain uses reconstitute factory

- **WHEN** `coachEntityMapper.toDomain(entity)` is called
- **THEN** the returned `Coach` has all fields equal to the entity's fields and no domain events registered

### Scenario: toEntity unwraps CoachId and TrainId

- **WHEN** `coachEntityMapper.toEntity(coach)` is called
- **THEN** the returned `CoachEntity` has `id` equal to `coach.getId().value()` and `trainId` equal to `coach.getTrainId().value()`

## Requirement: CoachRepositoryAdapter

The system SHALL provide a `CoachRepositoryAdapter` class annotated with `@Repository` in `train/infrastructure/persistence/` that implements `CoachRepository` by delegating to `CoachJpaRepository` and `CoachEntityMapper`. It SHALL be package-private.

### Scenario: save persists and returns domain model

- **WHEN** `coachRepositoryAdapter.save(coach)` is called
- **THEN** the coach is persisted to `train_cars` and the returned value is a `Coach` domain instance with the same attributes

### Scenario: findById returns empty for deleted coach

- **WHEN** `coachRepositoryAdapter.findById(id)` is called for a soft-deleted coach
- **THEN** `Optional.empty()` is returned

### Scenario: softDeleteById sets deletedAt

- **WHEN** `coachRepositoryAdapter.softDeleteById(id, instant)` is called
- **THEN** the `train_cars` record for that id has `deleted_at` set to the given instant

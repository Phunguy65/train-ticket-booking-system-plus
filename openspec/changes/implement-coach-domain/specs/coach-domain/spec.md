# ADDED Requirements

## Requirement: Coach aggregate root

The system SHALL provide a `Coach` aggregate root in `train/domain/model/` that represents a physical train car (toa tàu) within a train. A Coach is identified by a `CoachId` (UUID wrapper), belongs to exactly one `Train` via `trainId`, has a positive integer `carNumber` (1-based position in the train), a positive integer `totalSeats` (declared seat capacity), and supports soft delete via a nullable `deletedAt` timestamp.

### Scenario: Create a new coach

- **WHEN** `Coach.create(id, trainId, carNumber, totalSeats)` is called with valid arguments
- **THEN** a `Coach` instance is returned with the given attributes, `createdAt` set to the current time, and `deletedAt` as `null`

### Scenario: Reconstitute coach from persistence

- **WHEN** `Coach.reconstitute(id, trainId, carNumber, totalSeats, createdAt, deletedAt)` is called
- **THEN** a `Coach` instance is returned with all fields set exactly as provided and no domain events registered

### Scenario: Soft-delete a coach

- **WHEN** `coach.softDelete()` is called on an active coach
- **THEN** `coach.isDeleted()` returns `true`, `coach.getDeletedAt()` is non-null, and a `CoachDeleted` domain event is registered

### Scenario: Soft-delete is idempotent

- **WHEN** `coach.softDelete()` is called on an already-deleted coach
- **THEN** the method returns immediately without changing `deletedAt` or adding additional domain events

## Requirement: CoachId value object

The system SHALL provide a `CoachId` record (immutable UUID wrapper) in `train/domain/model/` with a static `of(UUID)` factory method. `CoachId` SHALL reject null values with an `IllegalArgumentException`.

### Scenario: Create a valid CoachId

- **WHEN** `CoachId.of(uuid)` is called with a non-null UUID
- **THEN** a `CoachId` instance is returned with `value()` equal to the given UUID

### Scenario: Null UUID is rejected

- **WHEN** `CoachId.of(null)` is called
- **THEN** an `IllegalArgumentException` is thrown

## Requirement: CoachRepository domain interface

The system SHALL provide a `CoachRepository` interface in `train/domain/repository/` that uses only domain types (no JPA or Spring types). The interface SHALL include the following operations: `save`, `findById`, `findByTrainId`, `existsByTrainIdAndCarNumber`, and `softDeleteById`.

### Scenario: Repository interface uses domain types only

- **WHEN** `CoachRepository.java` is examined
- **THEN** it contains no imports from `jakarta.persistence`, `org.springframework.data.jpa`, or any `*Entity` class

### Scenario: Find coaches by train

- **WHEN** `coachRepository.findByTrainId(trainId)` is called
- **THEN** all non-deleted coaches belonging to that train are returned as domain `Coach` instances

## Requirement: Coach domain events

The system SHALL provide `CoachDeleted` domain event in `train/domain/event/`. The event SHALL carry the `CoachId` of the deleted coach.

### Scenario: CoachDeleted event is published on soft delete

- **WHEN** `coach.softDelete()` is called on an active coach
- **THEN** `coach.getDomainEvents()` contains exactly one `CoachDeleted` event with the coach's id

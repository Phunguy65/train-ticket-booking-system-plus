# Why

The seat hierarchy currently couples seats directly to a Train (`trains → seats`). To support real-world train composition — where a train consists of multiple physical cars (toa tàu), each with its own capacity — we need an intermediate `Coach` (toa tàu) layer. The database migration V12 has already added the `train_cars` table; this change delivers the corresponding domain model and persistence infrastructure, and renames the concept from `TrainCar` → `Coach` throughout the codebase for clarity.

## What Changes

- **Rename** all references from `TrainCar` / `train_car` to `Coach` / `coach` in new code (the DB table `train_cars` is kept as-is since V12 migration is already written).
- **New domain aggregate**: `Coach` (AggregateRoot) with `CoachId`, domain repository interface, and domain events.
- **New persistence layer**: `CoachEntity`, `CoachJpaRepository`, `CoachEntityMapper`, `CoachRepositoryAdapter` wired to the existing `train_cars` table.
- **Update `Seat` domain**: replace `trainId` field with `coachId` (`TrainCarId` → `CoachId`), reflecting the new hierarchy.
- **Update `Seat` persistence**: replace `train_id` FK column with `train_car_id` FK (already handled by V12 migration), update mapper and repository adapter accordingly.
- **Rename migration script**: Rename `V12_0_0__add_train_cars_table.sql` → `V12_0_0__add_coaches_table.sql` (or ensure internal naming is consistent). The SQL content itself keeps `train_cars` as the physical table name.

## Capabilities

### New Capabilities

- `coach-domain`: Domain aggregate `Coach` with `CoachId`, repository interface `CoachRepository`, domain events (`CoachCreated`, `CoachDeleted`), and business rules (car_number > 0, total_seats > 0, unique per train).
- `coach-persistence`: Infrastructure persistence layer for Coach — JPA entity mapping to `train_cars` table, Spring Data JPA repository, mapper, and repository adapter.

### Modified Capabilities

- `database-schema`: The `seats` table schema changes — `train_id` FK is dropped and replaced with `train_car_id` FK (V12 migration). The `train_cars` table is introduced as a new first-class entity in the schema.

## Impact

- **Backend domain layer** (`train/domain/model/`): New files `Coach.java`, `CoachId.java`; new `train/domain/repository/CoachRepository.java`; new `train/domain/event/CoachCreated.java`, `CoachDeleted.java`. `Seat.java` updated.
- **Backend infrastructure layer** (`train/infrastructure/persistence/`): New files `CoachEntity.java`, `CoachJpaRepository.java`, `CoachEntityMapper.java`, `CoachRepositoryAdapter.java`. `SeatEntity.java`, `SeatEntityMapper.java`, `SeatRepositoryAdapter.java` updated.
- **Database**: V12 migration already written. Renaming migration file if needed.
- **No API surface changes** in this change — no new REST endpoints; application/use-case layer is out of scope.
- **Booking module** (`booking/`): `BookedSeat` references `SeatId` only — not affected by this change.

# Tasks

## 1. Bulk Domain Events

- [x] 1.1 Create `StationsDeleted` record in `station/domain/event/` — fields: `List<StationId> stationIds`, `Instant occurredAt`, static factory `of(List<StationId>, Instant)`
- [x] 1.2 Create `RoutesDeleted` record in `train/domain/event/` — fields: `List<RouteId> routeIds`, `Instant occurredAt`, static factory `of(List<RouteId>, Instant)`
- [x] 1.3 Create `TrainsDeleted` record in `train/domain/event/` — fields: `List<TrainId> trainIds`, `Instant occurredAt`, static factory `of(List<TrainId>, Instant)`
- [x] 1.4 Create `CoachesDeleted` record in `train/domain/event/` — fields: `List<CoachId> coachIds`, `Instant occurredAt`, static factory `of(List<CoachId>, Instant)`
- [x] 1.5 Create `SeatsDeleted` record in `train/domain/event/` — fields: `List<SeatId> seatIds`, `Instant occurredAt`, static factory `of(List<SeatId>, Instant)`

## 2. Repository Methods — Route

- [x] 2.1 Add `findActiveIdsByStationId(StationId): List<RouteId>` to `RouteRepository` domain interface
- [x] 2.2 Add `findActiveIdsByStationIds(List<StationId>): List<RouteId>` to `RouteRepository` domain interface
- [x] 2.3 Add `findDistinctActiveTrainIdsByRouteIds(List<RouteId>): List<TrainId>` to `RouteRepository` domain interface
- [x] 2.4 Add `countActiveByTrainId(TrainId): long` to `RouteRepository` domain interface
- [x] 2.5 Add corresponding JPQL queries to `RouteJpaRepository` for all four methods above
- [x] 2.6 Implement all four methods in `RouteRepositoryAdapter`

## 3. Repository Methods — Coach, Seat, RSA

- [x] 3.1 Add `findActiveIdsByTrainIds(List<TrainId>): List<CoachId>` to `CoachRepository` domain interface, `CoachJpaRepository`, and `CoachRepositoryAdapter`
- [x] 3.2 Add `findActiveIdsByCoachIds(List<CoachId>): List<SeatId>` to `SeatRepository` domain interface, `SeatJpaRepository`, and `SeatRepositoryAdapter`
- [x] 3.3 Add `hardDeleteByRouteIds(List<RouteId>): void` to `RouteSeatAvailabilityRepository` domain interface, `RouteSeatAvailabilityJpaRepository` (native `@Modifying` DELETE query), and `RouteSeatAvailabilityRepositoryAdapter`
- [x] 3.4 Add `hardDeleteBySeatIds(List<SeatId>): void` to `RouteSeatAvailabilityRepository` domain interface, `RouteSeatAvailabilityJpaRepository`, and `RouteSeatAvailabilityRepositoryAdapter`

## 4. Update BulkSoftDelete Use Cases

- [x] 4.1 Update `BulkSoftDeleteStationsUseCase`: replace for-loop event emission with `if (affected > 0) eventPublisher.publishEvent(StationsDeleted.of(ids, now))`; remove `routeValidationPort.hasActiveRoutesForStation` guard
- [x] 4.2 Update `BulkSoftDeleteRoutesUseCase`: replace for-loop with single `RoutesDeleted` bulk event; remove existence-only guard (keep if needed for 404 response)
- [x] 4.3 Update `BulkSoftDeleteTrainsUseCase`: replace for-loop with single `TrainsDeleted` bulk event; remove `existsActiveByTrainId` guard
- [x] 4.4 Update `BulkSoftDeleteCoachesUseCase`: replace for-loop with single `CoachesDeleted` bulk event; remove seat-existence guard
- [x] 4.5 Update `BulkSoftDeleteSeatsUseCase`: replace for-loop with single `SeatsDeleted` bulk event; remove `existsActiveBySeatId` and `hasBookingHistoryForSeat` guards

## 5. Cascade Listeners

- [x] 5.1 Create `CascadeOnStationDeletedListener` in `train/application/listener/` — `@ApplicationModuleListener` on `StationDeleted`: find active route IDs by station ID → soft delete → publish `RoutesDeleted`
- [x] 5.2 Create `CascadeOnStationsDeletedListener` in `train/application/listener/` — `@ApplicationModuleListener` on `StationsDeleted`: find active route IDs by station IDs → soft delete → publish `RoutesDeleted`
- [x] 5.3 Create `CascadeOnRoutesDeletedListener` in `train/application/listener/` — `@ApplicationModuleListener` on `RoutesDeleted`: hard delete RSA by route IDs → find distinct train IDs → filter orphaned trains (countActiveByTrainId == 0) → soft delete orphaned trains → publish `TrainsDeleted`
- [x] 5.4 Create `CascadeOnTrainsDeletedListener` in `train/application/listener/` — `@ApplicationModuleListener` on `TrainsDeleted`: find active coach IDs by train IDs → soft delete → publish `CoachesDeleted`
- [x] 5.5 Create `CascadeOnCoachesDeletedListener` in `train/application/listener/` — `@ApplicationModuleListener` on `CoachesDeleted`: find active seat IDs by coach IDs → hard delete RSA by seat IDs → soft delete seats → publish `SeatsDeleted`

## 6. Update Single-Entity Delete Use Cases

- [x] 6.1 Update `SoftDeleteStationUseCase`: remove `routeValidationPort.hasActiveRoutesForStation` guard (cascade now handles children); keep existence and idempotency checks
- [x] 6.2 Update `SoftDeleteTrainUseCase`: remove `existsActiveByTrainId` guard
- [x] 6.3 Update `SoftDeleteCoachUseCase`: remove seat-existence guard

## 7. Tests

- [x] 7.1 Unit test `CascadeOnStationsDeletedListener` — mock repositories, verify route soft delete and `RoutesDeleted` event published
- [x] 7.2 Unit test `CascadeOnRoutesDeletedListener` — verify RSA hard delete, orphan train filter logic, and `TrainsDeleted` event; verify shared train is NOT deleted when it has remaining active routes
- [x] 7.3 Unit test `CascadeOnTrainsDeletedListener` — verify coach soft delete and `CoachesDeleted` event
- [x] 7.4 Unit test `CascadeOnCoachesDeletedListener` — verify RSA hard delete by seat IDs, seat soft delete, and `SeatsDeleted` event
- [x] 7.5 Unit test updated `BulkSoftDeleteStationsUseCase` — verify single `StationsDeleted` event emitted (not N individual events)
- [x] 7.6 Integration test (`@ApplicationModuleTest`) for the train module — verify full cascade chain fires correctly when `StationsDeleted` is published

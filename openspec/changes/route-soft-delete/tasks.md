# Tasks

## 1. Database Migration

- [x] 1.1 Create Flyway migration `V<next>__add_deleted_at_to_bookings.sql` — `ALTER TABLE bookings ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ`

## 2. Route Domain Layer

- [x] 2.1 Add `deletedAt` field (`private Instant deletedAt`) to `Route.java` aggregate with getter `getDeletedAt()` and helper `isDeleted()`
- [x] 2.2 Add `softDelete()` method to `Route.java` — idempotent: if `isDeleted()` return immediately; otherwise set `this.deletedAt = Instant.now()` and register `RouteDeleted` event
- [x] 2.3 Update `Route.reconstitute(...)` to accept the `deletedAt` parameter (nullable `Instant`) and pass it through to the private constructor
- [x] 2.4 Create `RouteDeleted.java` in `train/domain/event/` — `public record RouteDeleted(RouteId routeId, Instant occurredAt) implements DomainEvent` with static factory `of(RouteId)`
- [x] 2.5 Add `softDeleteById(RouteId id, Instant deletedAt)` and `softDeleteByIds(List<RouteId> ids, Instant deletedAt)` to `RouteRepository.java` domain port
- [x] 2.6 Add `existsById(RouteId id)` to `RouteRepository.java` — needed by bulk use case to validate all IDs before deletion

## 3. Route Application Layer

- [x] 3.1 Create `SoftDeleteRouteCommand.java` in `train/application/command/` — `public record SoftDeleteRouteCommand(RouteId routeId)`
- [x] 3.2 Create `BulkSoftDeleteRoutesCommand.java` in `train/application/command/` — `public record BulkSoftDeleteRoutesCommand(List<RouteId> routeIds)`
- [x] 3.3 Create `SoftDeleteRouteUseCase.java` in `train/application/usecase/` — `@Service @Transactional`, inject `RouteRepository` and `ApplicationEventPublisher`; logic: find by ID → return `RouteError.RouteNotFound` if absent → call `route.softDelete()` → `routeRepository.save(route)` → publish events → return `Result.success()`
- [x] 3.4 Create `BulkSoftDeleteRoutesUseCase.java` in `train/application/usecase/` — `@Service @Transactional`, inject `RouteRepository` and `ApplicationEventPublisher`; logic: for each ID check `existsById` → collect missing → if any missing return `Result.failure(RouteError.RouteNotFound(...))` → call `routeRepository.softDeleteByIds(ids, now)` → publish `RouteDeleted` for each ID → return `Result.success(affected)`
- [x] 3.5 Add `RouteNotFound` (with optional `List<UUID> invalidIds` for bulk) variant to `RouteError.java` sealed interface (or add a second subtype `RoutesNotFound` if the existing `RouteNotFound` must stay unary)

## 4. Route Infrastructure — Persistence

- [x] 4.1 Update `RouteEntityMapper.java` — uncomment / implement `entity.setDeletedAt(route.getDeletedAt())` in `toEntity()`, and set `deletedAt` in `toDomain()` call to `Route.reconstitute(...)`
- [x] 4.2 Add `findActiveById` JPQL query to `RouteJpaRepository.java`: `@Query("SELECT r FROM RouteEntity r WHERE r.id = :id AND r.deletedAt IS NULL")`
- [x] 4.3 Update `findAllWithFilter` query in `RouteJpaRepository.java` to add `AND r.deletedAt IS NULL` condition
- [x] 4.4 Add `existsActiveById` JPQL query to `RouteJpaRepository.java`: `@Query("SELECT COUNT(r) > 0 FROM RouteEntity r WHERE r.id = :id AND r.deletedAt IS NULL")`
- [x] 4.5 Add `@Modifying @Query` `softDeleteById` to `RouteJpaRepository.java`: `UPDATE RouteEntity r SET r.deletedAt = :deletedAt WHERE r.id = :id AND r.deletedAt IS NULL`
- [x] 4.6 Add `@Modifying @Query` `softDeleteByIds` to `RouteJpaRepository.java`: `UPDATE RouteEntity r SET r.deletedAt = :deletedAt WHERE r.id IN :ids AND r.deletedAt IS NULL`
- [x] 4.7 Implement `softDeleteById`, `softDeleteByIds`, and `existsById` in `RouteRepositoryAdapter.java` to delegate to `RouteJpaRepository`
- [x] 4.8 Update `RouteRepositoryAdapter.findById` to use `jpaRepository.findActiveById(id.value())` instead of the default `findById`

## 5. Route Infrastructure — Web

- [x] 5.1 Create `BulkSoftDeleteRoutesHttpRequest.java` in `train/infrastructure/web/` — `record BulkSoftDeleteRoutesHttpRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> routeIds)`
- [x] 5.2 Add `deleteById` endpoint to `RouteController.java`: `@DeleteMapping(value = "/{id}", version = "1.0") @PreAuthorize("hasRole('ADMIN')")` — inject and call `softDeleteRouteUseCase`, fold result to `ResponseEntity.ok(JsendResponse.success())` or `errorResponse(error)`
- [x] 5.3 Add `bulkDelete` endpoint to `RouteController.java`: `@PostMapping(value = ":bulkDelete", version = "1.0") @PreAuthorize("hasRole('ADMIN')")` — inject and call `bulkSoftDeleteRoutesUseCase` with mapped command, fold to `JsendResponse.success(Map.of("deletedCount", n))` or `errorResponse(error)`
- [x] 5.4 Register `RouteError.RouteNotFound` (and `RoutesNotFound`) in `RouteController.errorResponse()` to return `404` / `422` respectively

## 6. Booking Module — Cascade Soft-Delete

- [x] 6.1 Add `deleted_at` field mapping to `BookingEntity.java`: `@Column(name = "deleted_at") private Instant deletedAt` with getter/setter
- [x] 6.2 Add `softDeleteByRouteId(RouteId routeId, Instant deletedAt)` and `softDeleteByRouteIds(List<RouteId> routeIds, Instant deletedAt)` to `BookingRepository.java` domain port
- [x] 6.3 Add `@Modifying @Query` for `softDeleteByRouteId` to `BookingJpaRepository.java`: `UPDATE BookingEntity b SET b.deletedAt = :deletedAt WHERE b.routeId = :routeId AND b.deletedAt IS NULL`
- [x] 6.4 Add `@Modifying @Query` for `softDeleteByRouteIds` to `BookingJpaRepository.java`: `UPDATE BookingEntity b SET b.deletedAt = :deletedAt WHERE b.routeId IN :routeIds AND b.deletedAt IS NULL`
- [x] 6.5 Implement `softDeleteByRouteId` and `softDeleteByRouteIds` in `BookingRepositoryAdapter.java`
- [x] 6.6 Create `RouteDeletedEventListener.java` in `booking/infrastructure/` (or `booking/application/`) — `@Component` with `@TransactionalEventListener(phase = BEFORE_COMMIT)` handling `RouteDeleted`; call `bookingRepository.softDeleteByRouteId(event.routeId(), event.occurredAt())`

## 7. Tests

- [x] 7.1 Write unit test `RouteTest.java` — verify `softDelete()` sets `deletedAt`, registers `RouteDeleted` event; verify idempotency (second call no-ops)
- [x] 7.2 Write use case unit test `SoftDeleteRouteUseCaseTest.java` (`@ExtendWith(MockitoExtension)`) — happy path, route not found, already deleted (idempotent)
- [x] 7.3 Write use case unit test `BulkSoftDeleteRoutesUseCaseTest.java` — happy path (all valid IDs), one invalid ID fails all, publishes correct number of events
- [x] 7.4 Write controller test `RouteControllerTest.java` additions (`@WebMvcTest`) — `DELETE /{id}` 200, 404; `POST :bulkDelete` 200, 400 (empty/oversized array), 422 (invalid IDs), 401/403 auth checks
- [x] 7.5 Write persistence test `RouteRepositoryAdapterTest.java` additions (`@DataJpaTest`) — verify `findById` excludes deleted routes; verify `softDeleteById` and `softDeleteByIds` set `deleted_at`; verify `existsById` returns false for deleted routes
- [x] 7.6 Write `RouteDeletedEventListenerTest.java` — verify that publishing a `RouteDeleted` event causes `bookingRepository.softDeleteByRouteId` to be called

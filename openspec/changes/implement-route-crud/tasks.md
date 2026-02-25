## 1. Shared Infrastructure — ErrorCode

- [x] 1.1 Add `ROUTE_NOT_FOUND` constant to `shared/infrastructure/web/ErrorCode.java`

## 2. Domain Layer — Value Objects & Enums

- [x] 2.1 Create `StationId.java` in `train/domain/model/` — type-safe UUID value object, rejects null, follows `TrainId` / `SeatId` pattern
- [x] 2.2 Create `RouteStatus.java` in `train/domain/model/` — enum with `SCHEDULED` constant
- [x] 2.3 Create `RouteFilter.java` in `train/domain/model/` — record with four nullable fields: `originStationId`, `destinationStationId`, `departureDateFrom`, `departureDateTo`

## 3. Domain Layer — Aggregate, Errors & Repository Port

- [x] 3.1 Create `RouteError.java` in `train/domain/errors/` — sealed interface with `RouteNotFound` subtype implementing a `message()` method
- [x] 3.2 Create `Route.java` in `train/domain/model/` — `AggregateRoot<RouteId>` with fields `id`, `trainId`, `originStationId` (StationId), `destinationStationId` (StationId), `departureTime`, `arrivalTime`, `basePrice`, `status`, `createdAt`; factory method `create()` validates `arrivalTime > departureTime` and registers `RouteCreated` event; `reconstitute()` does not register events
- [x] 3.3 Create `RouteRepository.java` in `train/domain/repository/` — interface with `save(Route)`, `findById(RouteId)`, `findAll(page, size, sortField, SortDirection, RouteFilter)` using only domain types

## 4. Application Layer — Commands, DTOs & Use Cases

- [x] 4.1 Create `CreateRouteCommand.java` in `train/application/command/` — record with fields: `trainId` (UUID), `originStationId` (UUID), `destinationStationId` (UUID), `departureTime` (Instant), `arrivalTime` (Instant), `basePrice` (BigDecimal)
- [x] 4.2 Create `RouteDto.java` in `train/application/dto/` — record with fields: `id`, `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice`, `status` (RouteStatus), `createdAt`
- [x] 4.3 Create `CreateRouteUseCase.java` in `train/application/usecase/` — `@Service @Transactional`, accepts `CreateRouteCommand`, calls `Route.create()`, publishes domain events via `ApplicationEventPublisher`, saves via `RouteRepository`, returns `Result<RouteDto, RouteError>`
- [x] 4.4 Create `GetRouteByIdUseCase.java` in `train/application/usecase/` — `@Service`, accepts `RouteId`, returns `Result<RouteDto, RouteError>` (failure = `RouteNotFound`)
- [x] 4.5 Create `GetRoutesUseCase.java` in `train/application/usecase/` — `@Service`, accepts `page`, `size`, `sortField`, `SortDirection`, `RouteFilter`, returns `PageResult<RouteDto>`

## 5. Persistence Layer — JPA Adapter

- [x] 5.1 Create `RouteEntity.java` in `train/infrastructure/persistence/` — `@Entity @Table(name="routes")`, package-private, UUID fields matching DB schema (`id`, `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice`, `status`, `createdAt`)
- [x] 5.2 Create `RouteJpaRepository.java` in `train/infrastructure/persistence/` — package-private interface extending `JpaRepository<RouteEntity, UUID>`, with a `@Query` method supporting optional filtering by `originStationId`, `destinationStationId`, `departureDateFrom`, `departureDateTo` using `IS NULL OR` guards
- [x] 5.3 Create `RouteEntityMapper.java` in `train/infrastructure/persistence/` — `@Component`, package-private, `toDomain(RouteEntity)` using `Route.reconstitute()`, `toEntity(Route)` mapping domain fields to entity
- [x] 5.4 Create `RouteRepositoryAdapter.java` in `train/infrastructure/persistence/` — `@Repository`, package-private, implements `RouteRepository`; `findAll` builds `PageRequest` from sort params and delegates to the filter query, wraps result in `PageResult`

## 6. Web Layer — Controller & DTOs

- [x] 6.1 Add `ROUTE_NOT_FOUND` to `ErrorCode` enum (if not done in task 1.1 — verify)
- [x] 6.2 Create `CreateRouteHttpRequest.java` in `train/infrastructure/web/` — package-private record with `@NotNull` / `@NotBlank` validated fields: `trainId` (UUID), `originStationId` (UUID), `destinationStationId` (UUID), `departureTime` (Instant), `arrivalTime` (Instant), `basePrice` (BigDecimal)
- [x] 6.3 Create `RouteHttpResponse.java` in `train/infrastructure/web/` — package-private record with fields: `id`, `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice`, `status`, `createdAt`
- [x] 6.4 Create `RouteRequestMapper.java` in `train/infrastructure/web/` — `@Component`, package-private; `toCommand(CreateRouteHttpRequest)` → `CreateRouteCommand`; `toResponse(RouteDto)` → `RouteHttpResponse`
- [x] 6.5 Create `RouteController.java` in `train/infrastructure/web/` — `@RestController @RequestMapping("/{version}/routes")`, package-private; three endpoints:
  - `POST` (version=1.0) `@PreAuthorize("hasRole('ADMIN')")` → `201 Created` + Location header on success
  - `GET /{id}` (version=1.0) — public, `200 OK` or `404 Not Found`
  - `GET /` (version=1.0) — public, validates `page`/`size`/`sort`, parses optional filter params from `@RequestParam`, returns `SliceHttpResponse<RouteHttpResponse>`

## 7. Database Migration

- [x] 7.1 Create `database/migrations/V5.0.0__add_route_filter_index.sql` — adds composite index `idx_routes_origin_dest_departure ON routes (origin_station_id, destination_station_id, departure_time)` for filter query performance

## 8. Unit Tests — Domain Layer

- [x] 8.1 Create `RouteTest.java` in `train/domain/model/` (test) — pure JUnit 5; covers: `create()` sets `SCHEDULED` status and registers `RouteCreated` event; `reconstitute()` registers no events; `create()` throws when `arrivalTime <= departureTime`

## 9. Unit Tests — Application Layer

- [x] 9.1 Create `CreateRouteUseCaseTest.java` in `train/application/usecase/` (test) — `@ExtendWith(MockitoExtension)`, mocks `RouteRepository` and `ApplicationEventPublisher`; verifies success path returns `RouteDto` and event is published
- [x] 9.2 Create `GetRouteByIdUseCaseTest.java` in `train/application/usecase/` (test) — mocks `RouteRepository`; verifies success path and `RouteNotFound` failure path
- [x] 9.3 Create `GetRoutesUseCaseTest.java` in `train/application/usecase/` (test) — mocks `RouteRepository`; verifies paginated result and filter delegation

## 10. Integration Tests — Persistence Layer

- [x] 10.1 Create `RouteRepositoryAdapterTest.java` in `train/infrastructure/persistence/` (test) — `@DataJpaTest`; covers `save()`, `findById()` (found and not-found), `findAll()` with no filter, with `originStationId` filter, and with departure date range filter

## 11. Integration Tests — Web Layer

- [x] 11.1 Create `RouteControllerTest.java` in `train/infrastructure/web/` (test) — `@WebMvcTest(RouteController.class)`; covers:
  - `POST` valid request by ADMIN → 201
  - `POST` missing field → 400
  - `POST` by CUSTOMER → 403
  - `GET /{id}` found → 200
  - `GET /{id}` not found → 404
  - `GET /` paginated → 200 with slice response
  - `GET /` invalid page → 400
  - `GET /` invalid sort → 400

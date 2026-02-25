## 1. Shared Module Updates

- [x] 1.1 Add `STATION_NOT_FOUND` and `STATION_CODE_ALREADY_EXISTS` to the `ErrorCode` enum in `shared/infrastructure/web/ErrorCode.java`

## 2. Module Scaffold & StationId Migration

- [x] 2.1 Create `station/package-info.java` with `@ApplicationModule` annotation
- [x] 2.2 Move `StationId.java` from `train/domain/model/` to `station/domain/model/` (update package declaration)
- [x] 2.3 Create `station/domain/model/package-info.java` with `@NamedInterface("model")` to expose `StationId` to other modules
- [x] 2.4 Update `train/package-info.java` to declare `allowedDependencies = {"station::model"}` for cross-module access
- [x] 2.5 Update all imports of `StationId` in the `train` module (`Route.java`, `RouteEntity.java`, and any other references) to use the new `station.domain.model` package

## 3. Station Domain Layer

- [x] 3.1 Create `station/domain/model/Station.java` — aggregate root with `create()` factory method (registers `StationCreated` event) and `reconstitute()` method (no events); fields: `id`, `code`, `name`, `city`, `createdAt`
- [x] 3.2 Create `station/domain/event/StationCreated.java` — domain event record with `id`, `code`, `name`, `city`
- [x] 3.3 Create `station/domain/repository/StationRepository.java` — repository port interface with methods: `save(Station)`, `findById(StationId)`, `findAll(page, size, sortField, direction)`, `existsByCode(String)`
- [x] 3.4 Create `station/domain/errors/StationError.java` — sealed interface with `StationNotFound` and `StationCodeAlreadyExists` typed error records, each implementing `message()`

## 4. Station Application Layer

- [x] 4.1 Create `station/application/command/CreateStationCommand.java` — input record with fields: `code`, `name`, `city`
- [x] 4.2 Create `station/application/dto/StationDto.java` — output record with fields: `id` (UUID), `code`, `name`, `city`, `createdAt`
- [x] 4.3 Create `station/application/usecase/CreateStationUseCase.java` — `@Service @Transactional`; checks `existsByCode`, calls `Station.create()`, publishes domain events, returns `Result<StationDto, StationError>`
- [x] 4.4 Create `station/application/usecase/GetStationByIdUseCase.java` — `@Service @Transactional(readOnly=true)`; finds station by ID, returns `Result<StationDto, StationError.StationNotFound>`
- [x] 4.5 Create `station/application/usecase/GetStationsUseCase.java` — `@Service @Transactional(readOnly=true)`; returns `PageResult<StationDto>` with pagination and sorting

## 5. Station Persistence Layer

- [x] 5.1 Create `station/infrastructure/persistence/StationEntity.java` — `@Entity @Table(name="stations")`; fields: `id` (UUID), `code`, `name`, `city`, `createdAt`; package-private
- [x] 5.2 Create `station/infrastructure/persistence/StationJpaRepository.java` — `extends JpaRepository<StationEntity, UUID>` with `existsByCode(String)` and `findAll(Pageable)` methods; package-private
- [x] 5.3 Create `station/infrastructure/persistence/StationEntityMapper.java` — `@Component`; maps `Station` ↔ `StationEntity` using `reconstitute()` for domain model construction
- [x] 5.4 Create `station/infrastructure/persistence/StationRepositoryAdapter.java` — `@Repository`; implements `StationRepository` port using `StationJpaRepository` and `StationEntityMapper`; package-private

## 6. Station Web Layer

- [x] 6.1 Create `station/infrastructure/web/CreateStationHttpRequest.java` — request record with Jakarta Validation: `@NotBlank @Size(max=10)` on `code`, `@NotBlank @Size(max=255)` on `name`, `@NotBlank @Size(max=100)` on `city`
- [x] 6.2 Create `station/infrastructure/web/StationHttpResponse.java` — response record mirroring `StationDto` fields (`id`, `code`, `name`, `city`, `createdAt`)
- [x] 6.3 Create `station/infrastructure/web/StationRequestMapper.java` — `@Component`; maps `CreateStationHttpRequest` → `CreateStationCommand` and `StationDto` → `StationHttpResponse`
- [x] 6.4 Create `station/infrastructure/web/StationController.java` — `@RestController @RequestMapping("/{version}/stations")`; implement three endpoints:
  - `POST /` with `@PreAuthorize("hasRole('ADMIN')")`, `@Valid` body, returns 201 + Location header on success / 409 on duplicate
  - `GET /{id}` returns 200 on found / 404 on not found
  - `GET /` with `page`, `size`, `sort` query params, validates ranges, whitelists sort fields, returns `SliceHttpResponse`

## 7. Security Configuration

- [x] 7.1 Update `SecurityConfig.java` to add rules for station endpoints:
  - `POST /api/*/stations/**` → `hasRole("ADMIN")`
  - `GET /api/*/stations/**` → `authenticated()`

## 8. Tests

- [x] 8.1 Create `station/domain/model/StationTest.java` — pure JUnit 5 unit tests verifying `Station.create()` sets fields correctly and registers `StationCreated` event; `reconstitute()` does not register events
- [x] 8.2 Create `station/application/usecase/CreateStationUseCaseTest.java` — `@ExtendWith(MockitoExtension)`; test success path, duplicate code path (returns `StationCodeAlreadyExists` error), event publication
- [x] 8.3 Create `station/application/usecase/GetStationByIdUseCaseTest.java` — `@ExtendWith(MockitoExtension)`; test found and not-found paths
- [x] 8.4 Create `station/application/usecase/GetStationsUseCaseTest.java` — `@ExtendWith(MockitoExtension)`; test pagination result mapping
- [x] 8.5 Create `station/infrastructure/persistence/StationRepositoryAdapterTest.java` — `@DataJpaTest`; verify save, findById, existsByCode, and paginated findAll against an in-memory DB
- [x] 8.6 Create `station/infrastructure/web/StationControllerTest.java` — `@WebMvcTest`; test all three endpoints including validation errors, 409 conflict, 404 not found, and 403/401 access control scenarios
- [x] 8.7 Create `station/StationModuleTest.java` — `@ApplicationModuleTest`; verify Spring Modulith module boundaries and that the module loads correctly with all dependencies

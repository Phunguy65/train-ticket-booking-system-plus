## 1. Module Scaffold & Boundaries

- [x] 1.1 Create `train/package-info.java` with `@ApplicationModule` annotation
- [x] 1.2 Create `train/domain/model/package-info.java` with `@NamedInterface("model")` annotation

## 2. Domain Layer

- [x] 2.1 Create `TrainId.java` — `record TrainId(UUID value)` with null guard and `of(UUID)` factory, following `UserId` / `BookingId` pattern
- [x] 2.2 Create `Train.java` — aggregate root extending `AggregateRoot<TrainId>` with fields `trainId`, `trainNumber`, `name`, `totalSeats`, `createdAt`; `create()` factory registers `TrainCreated` event; `reconstitute()` factory does not
- [x] 2.3 Create `TrainCreated.java` — immutable `record` implementing `DomainEvent` with `TrainId` and `trainNumber`
- [x] 2.4 Create `TrainRepository.java` — port interface with `save(Train)`, `findById(TrainId)`, `findAll(int page, int size, String sortField, SortDirection direction)`, `existsByTrainNumber(String)`
- [x] 2.5 Create `TrainError.java` — `sealed interface` with `record TrainNumberAlreadyExists(String trainNumber)` and `record TrainNotFound()` variants; each implements `message()`

## 3. Application Layer

- [x] 3.1 Create `CreateTrainCommand.java` — `record` with `trainNumber`, `name`, `totalSeats`
- [x] 3.2 Create `TrainDto.java` — `record` with `id(UUID)`, `trainNumber`, `name`, `totalSeats`, `createdAt(Instant)`
- [x] 3.3 Create `CreateTrainUseCase.java` — `@Service @Transactional`; checks `existsByTrainNumber` → returns `Result.failure(TrainNumberAlreadyExists)` if duplicate; otherwise creates aggregate, saves, returns `Result.success(TrainDto)`
- [x] 3.4 Create `GetTrainByIdUseCase.java` — `@Service`; calls `findById(TrainId)` → returns `Result.success(TrainDto)` or `Result.failure(TrainNotFound)`
- [x] 3.5 Create `GetTrainsUseCase.java` — `@Service`; calls `findAll(page, size, sort, direction)` → returns `PageResult<TrainDto>`

## 4. Persistence Infrastructure

- [x] 4.1 Create `TrainEntity.java` — `@Entity @Table(name = "trains")` with `UUID id`, `String trainNumber`, `String name`, `int totalSeats`, `Instant createdAt`; no `@GeneratedValue` (DB default `uuidv7()`)
- [x] 4.2 Create `TrainJpaRepository.java` — package-private `interface` extending `JpaRepository<TrainEntity, UUID>` with `boolean existsByTrainNumber(String trainNumber)`; add `Page<TrainEntity> findAll(Pageable pageable)` via `JpaRepository` default
- [x] 4.3 Create `TrainEntityMapper.java` — `@Component` with `toDomain(TrainEntity) → Train` (uses `Train.reconstitute()`) and `toEntity(Train) → TrainEntity`
- [x] 4.4 Create `TrainRepositoryAdapter.java` — `@Repository` implementing `TrainRepository`; delegates `save`, `findById`, `findAll` (using `PageRequest`), and `existsByTrainNumber` to `TrainJpaRepository` via `TrainEntityMapper`

## 5. Web Infrastructure

- [x] 5.1 Create `CreateTrainHttpRequest.java` — `record` with `@NotBlank @Size(max=20) String trainNumber`, `@NotBlank @Size(max=255) String name`, `@Positive int totalSeats`
- [x] 5.2 Create `TrainHttpResponse.java` — `record` with `UUID id`, `String trainNumber`, `String name`, `int totalSeats`, `Instant createdAt`
- [x] 5.3 Create `TrainRequestMapper.java` — `@Component` mapping `CreateTrainHttpRequest → CreateTrainCommand` and `TrainDto → TrainHttpResponse`
- [x] 5.4 Create `TrainController.java` — `@RestController @RequestMapping("/{version}/trains")`; implement `create` (POST, version 1.0, `@PreAuthorize("hasRole('ADMIN')")`), `list` (GET, version 1.0, authenticated), `getById` (GET `/{id}`, version 1.0, authenticated); all return `ResponseEntity<JsendResponse<?>>`
- [x] 5.5 Add `requestMatchers` rules to `SecurityConfig`: `POST /api/*/trains/**` → `hasRole("ADMIN")`; `GET /api/*/trains/**` → `authenticated()`

## 6. Tests — Domain

- [x] 6.1 Create `TrainTest.java` — pure JUnit 5; assert `Train.create()` registers exactly one `TrainCreated` event; assert `Train.reconstitute()` registers no events

## 7. Tests — Application Use Cases

- [x] 7.1 Create `CreateTrainUseCaseTest.java` — `@ExtendWith(MockitoExtension)`; test success path (returns `TrainDto`); test duplicate `trainNumber` path (returns `TrainNumberAlreadyExists`); verify `trainRepository.save()` called only on success
- [x] 7.2 Create `GetTrainByIdUseCaseTest.java` — test found path returns `TrainDto`; test not-found path returns `TrainNotFound`
- [x] 7.3 Create `GetTrainsUseCaseTest.java` — test returns `PageResult<TrainDto>` with correct page metadata

## 8. Tests — Persistence

- [x] 8.1 Create `TrainRepositoryAdapterTest.java` — `@DataJpaTest @Import({TrainRepositoryAdapter.class, TrainEntityMapper.class})`; test `save` persists and returns domain model; test `findById` on existing and missing ID; test `existsByTrainNumber` true/false; test `findAll` pagination and sort order

## 9. Tests — Web Layer

- [x] 9.1 Create `TrainControllerTest.java` — `@WebMvcTest(TrainController.class) @Import({TrainRequestMapper.class, GlobalExceptionHandler.class, WebConfig.class})`; test POST 201 with valid body; test POST 409 on duplicate; test POST 400 on invalid body; test POST 403 for non-ADMIN; test GET list 200 with JSend success envelope; test GET by ID 200; test GET by ID 404

## 10. Tests — Module Integration

- [x] 10.1 Create `TrainModuleTest.java` — `@ApplicationModuleTest`; assert module structure is valid; assert `TrainCreated` event is published after `CreateTrainUseCase.execute()`

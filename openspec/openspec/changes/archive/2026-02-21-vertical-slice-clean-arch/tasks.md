## 1. Package Structure & Spring Modulith Setup

- [x] 1.1 Create top-level bounded context packages: `booking/`, `payment/`, `train/`, `user/` under `io.github.phunguy65.ttbs.backend`
- [x] 1.2 Create `package-info.java` for `booking/` module with `@ApplicationModule` annotation
- [x] 1.3 Create sub-package structure in `booking/`: `domain/model/`, `domain/event/`, `domain/repository/`, `application/usecase/`, `application/dto/`, `infrastructure/persistence/`, `infrastructure/web/`
- [x] 1.4 Verify Spring Modulith can detect `booking` as a valid module (run existing module structure test or create one)

## 2. Booking Domain Layer (Pure Java, No Framework Dependencies)

- [x] 2.1 Create `BookingId` as Java record wrapping `UUID` with `generate()` factory method delegating to `IdGenerator`
- [x] 2.2 Create `UserId`, `RouteId`, `SeatId` as Java record types (same pattern as `BookingId`)
- [x] 2.3 Create `BookingStatus` enum with states: `PENDING`, `CONFIRMED`, `CANCELLED`
- [x] 2.4 Create `BookingCreated`, `BookingConfirmed`, and `BookingCancelled` domain events implementing `DomainEvent` interface from shared kernel
- [x] 2.5 Create `Booking` aggregate class extending `AggregateRoot<BookingId>` with: private constructor, `create()` static factory method (registers `BookingCreated` event), `confirm()` method (validates PENDING status, registers `BookingConfirmed`), `cancel()` method (validates non-CANCELLED), `reconstitute()` static factory method (no events registered)
- [x] 2.6 Create `BookingRepository` interface in `domain/repository/` with methods: `save(Booking): Booking`, `findById(BookingId): Optional<Booking>`, `findByIdempotencyKey(String): Optional<Booking>`
- [x] 2.7 Write unit tests for `Booking` aggregate: test `create()` registers `BookingCreated`, test `confirm()` from PENDING succeeds, test `confirm()` from CONFIRMED throws `DomainException`, test `cancel()` from CANCELLED throws `DomainException`, test `reconstitute()` produces no domain events

## 3. Booking Application Layer (Use Cases & DTOs)

- [x] 3.1 Create `CreateBookingCommand` record/data class with fields: `userId: UUID`, `routeId: UUID`, `seatId: UUID`, `idempotencyKey: String`
- [x] 3.2 Create `BookingDto` record/data class with fields: `id: UUID`, `userId: UUID`, `routeId: UUID`, `seatId: UUID`, `status: String`, `totalPrice: BigDecimal`, `currency: String`
- [x] 3.3 Create `CreateBookingUseCase` `@Service` class with `@Transactional execute(CreateBookingCommand): BookingDto` method — creates `Booking` domain object via `Booking.create()`, persists via `BookingRepository`, publishes domain events, returns `BookingDto`
- [x] 3.4 Create `GetBookingUseCase` `@Service` class with `execute(UUID): BookingDto` method — retrieves from `BookingRepository`, throws `BookingNotFoundException` if not found, returns `BookingDto`
- [x] 3.5 Create `BookingNotFoundException` extending `DomainException` from shared kernel (HTTP 404)
- [x] 3.6 Write unit tests for `CreateBookingUseCase`: mock `BookingRepository`, verify `Booking.create()` is called, verify `save()` is called, verify returned DTO has correct fields
- [x] 3.7 Write unit test for `GetBookingUseCase`: verify returns DTO on found, verify throws `BookingNotFoundException` on not found

## 4. Booking Infrastructure — Persistence Layer

- [x] 4.1 Create `BookingEntity` JPA class in `infrastructure/persistence/` annotated with `@Entity @Table(name = "bookings")` — fields matching DB schema: `id: UUID` (`@Id`), `userId: UUID`, `routeId: UUID`, `seatId: UUID`, `totalPrice: BigDecimal`, `currency: String`, `status: BookingStatus` (`@Enumerated(STRING)`), `idempotencyKey: String` — include protected no-args constructor
- [x] 4.2 Create `BookingJpaRepository` extending `JpaRepository<BookingEntity, UUID>` with `findByIdempotencyKey(key: String): Optional<BookingEntity>` custom query method
- [x] 4.3 Create `BookingEntityMapper` in `infrastructure/persistence/` with `toDomain(BookingEntity): Booking` (uses `Booking.reconstitute()`) and `toEntity(Booking): BookingEntity` methods
- [x] 4.4 Create `BookingRepositoryAdapter` implementing domain `BookingRepository` interface — delegates `save()` to `BookingJpaRepository` via mapper, delegates `findById()` via mapper
- [x] 4.5 Write `@DataJpaTest` integration test for `BookingRepositoryAdapter`: test `save()` persists correctly, test `findById()` retrieves correct domain model, test round-trip (save → findById) preserves all fields
- [ ] 4.6 Verify `BookingEntity` validates against existing `bookings` Flyway migration schema (run with `spring.jpa.hibernate.ddl-auto=validate`) — requires running PostgreSQL instance

## 5. Booking Infrastructure — Web Layer

- [x] 5.1 Create `CreateBookingHttpRequest` record with fields: `userId: UUID`, `routeId: UUID`, `seatId: UUID`, `idempotencyKey: String`
- [x] 5.2 Create `BookingHttpResponse` record wrapping `BookingDto` fields for HTTP responses
- [x] 5.3 Create `BookingRequestMapper` in `infrastructure/web/` with `toCommand(CreateBookingHttpRequest): CreateBookingCommand` and `toResponse(BookingDto): BookingHttpResponse` methods
- [x] 5.4 Create `BookingController` `@RestController` with `@RequestMapping("/api/bookings")` — implement `POST /` calling `CreateBookingUseCase` (returns 201 Created), implement `GET /{id}` calling `GetBookingUseCase` (returns 200 OK or 404 Not Found)
- [x] 5.5 Create global `@ControllerAdvice` exception handler in `shared/infrastructure/web/` mapping `DomainException` → 404/422 based on `getHttpStatus()`
- [x] 5.6 Write `@WebMvcTest(BookingController)` tests: test `POST /api/bookings` with valid request returns 201, test `GET /api/bookings/{id}` with existing ID returns 200, test `GET /api/bookings/{id}` with unknown ID returns 404

## 6. Spring Modulith Event Integration

- [x] 6.1 Domain events (`BookingCreated`, `BookingConfirmed`, `BookingCancelled`) published via `ApplicationEventPublisher` in `CreateBookingUseCase` after transaction save
- [x] 6.2 Verify Spring Modulith publishes `BookingCreated` event after `CreateBookingUseCase` transaction commits (write `@ApplicationModuleTest` that asserts event publication)
- [x] 6.3 Write `@ApplicationModuleTest` for the `booking` module to verify module boundary: no illegal dependencies on other module internals, module is self-contained

## 7. Cleanup & Documentation

- [x] 7.1 Remove the wrongly-placed files in `openspec/changes/vertical-slice-clean-arch/` (the ones at the root `openspec/changes/` path, not `openspec/openspec/changes/`)
- [x] 7.2 Create `backend/ARCHITECTURE.md` documenting the vertical slice conventions, package structure rules, and mapping patterns for team reference
- [x] 7.3 Add ArchUnit dependency to `backend/build.gradle.kts` and create `ArchitectureTest` class verifying: no `@Entity` in domain packages, no Spring/JPA imports in domain packages, application layer only depends on domain and shared, use cases annotated with `@Transactional`
- [x] 7.4 Run full backend test suite and verify all tests pass — 30 tests, 0 failures

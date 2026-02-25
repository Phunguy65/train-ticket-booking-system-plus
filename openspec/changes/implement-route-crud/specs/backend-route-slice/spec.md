## ADDED Requirements

### Requirement: Route domain aggregate exists in the train module

The `train` module SHALL contain a `Route` aggregate root in `train/domain/model/Route.java` extending `AggregateRoot<RouteId>`. The aggregate SHALL expose two factory methods: `create()` (for new routes, registers `RouteCreated` event) and `reconstitute()` (for rehydration from persistence, no event). All fields SHALL be immutable after creation.

#### Scenario: Route.create() produces a valid aggregate and registers RouteCreated event

- **WHEN** `Route.create(id, trainId, originStationId, destinationStationId, departureTime, arrivalTime, basePrice)` is called
- **THEN** the returned `Route` instance SHALL have `status = SCHEDULED`, a non-null `createdAt`, and exactly one pending domain event of type `RouteCreated` containing the same `routeId` and `trainId`

#### Scenario: Route.reconstitute() does not register any domain events

- **WHEN** `Route.reconstitute(id, trainId, originStationId, destinationStationId, departureTime, arrivalTime, basePrice, status, createdAt)` is called
- **THEN** the returned `Route` instance SHALL have zero pending domain events

#### Scenario: Route requires arrival after departure

- **WHEN** `Route.create()` is called with `arrivalTime` not after `departureTime`
- **THEN** an `IllegalArgumentException` SHALL be thrown

### Requirement: StationId value object wraps UUID references to stations

The `train` module SHALL contain a `StationId` record in `train/domain/model/StationId.java` following the same pattern as `TrainId`, `SeatId`, and `RouteId`. It SHALL reject `null` values.

#### Scenario: StationId rejects null

- **WHEN** `new StationId(null)` is called
- **THEN** an `IllegalArgumentException` SHALL be thrown

#### Scenario: StationId.of() creates a valid value object

- **WHEN** `StationId.of(uuid)` is called with a non-null UUID
- **THEN** a `StationId` wrapping that UUID SHALL be returned

### Requirement: RouteStatus enum defines lifecycle values

The `train` module SHALL contain a `RouteStatus` enum in `train/domain/model/RouteStatus.java` with at least the value `SCHEDULED`.

#### Scenario: RouteStatus has SCHEDULED value

- **WHEN** `RouteStatus.SCHEDULED` is referenced
- **THEN** it SHALL compile and be a valid enum constant

### Requirement: RouteError sealed interface defines typed domain errors

The `train` module SHALL contain a `RouteError` sealed interface in `train/domain/errors/RouteError.java`. It SHALL declare at minimum a `RouteNotFound` error subtype.

#### Scenario: RouteNotFound error carries a message

- **WHEN** `new RouteError.RouteNotFound()` is instantiated
- **THEN** calling `.message()` SHALL return a non-blank string

### Requirement: RouteFilter record encapsulates optional list filter criteria

The `train` module SHALL contain a `RouteFilter` record in `train/domain/model/RouteFilter.java` with four nullable fields: `originStationId` (UUID), `destinationStationId` (UUID), `departureDateFrom` (Instant), `departureDateTo` (Instant). All fields SHALL accept `null` to indicate "no filter on this axis."

#### Scenario: RouteFilter allows all-null construction

- **WHEN** `new RouteFilter(null, null, null, null)` is called
- **THEN** a valid `RouteFilter` with all null fields SHALL be created

### Requirement: RouteRepository domain port defines persistence operations

The `train` module SHALL contain a `RouteRepository` interface in `train/domain/repository/RouteRepository.java` with no JPA or Spring framework types. It SHALL declare:

- `Route save(Route route)`
- `Optional<Route> findById(RouteId id)`
- `PageResult<Route> findAll(int page, int size, String sortField, SortDirection direction, RouteFilter filter)`

#### Scenario: RouteRepository methods use domain types only

- **WHEN** examining the `RouteRepository` interface
- **THEN** all method parameters and return types SHALL be domain types (`Route`, `RouteId`, `Optional<Route>`, `PageResult<Route>`, `RouteFilter`, `SortDirection`) — never JPA entity types or framework-specific types

### Requirement: CreateRouteUseCase creates a route and publishes RouteCreated

The `train` module SHALL contain `CreateRouteUseCase` in `train/application/usecase/CreateRouteUseCase.java` annotated `@Service @Transactional`. It SHALL accept a `CreateRouteCommand` and return `Result<RouteDto, RouteError>`. On success, the saved `Route`'s domain events SHALL be published via `ApplicationEventPublisher` so `SeatAvailabilitySeeder` is triggered.

#### Scenario: Successful route creation returns RouteDto and triggers SeatAvailabilitySeeder

- **WHEN** `CreateRouteUseCase.execute(command)` is called with valid data
- **THEN** a `Route` SHALL be saved, a `RouteCreated` event SHALL be published, and `Result.success(routeDto)` SHALL be returned

#### Scenario: RouteDto contains all expected fields

- **WHEN** a `RouteDto` is returned by `CreateRouteUseCase`
- **THEN** it SHALL contain `id`, `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice`, `status`, and `createdAt`

### Requirement: GetRouteByIdUseCase returns a route or a RouteNotFound error

The `train` module SHALL contain `GetRouteByIdUseCase` in `train/application/usecase/GetRouteByIdUseCase.java` annotated `@Service`. It SHALL accept a `RouteId` and return `Result<RouteDto, RouteError>`.

#### Scenario: Existing route is found

- **WHEN** `GetRouteByIdUseCase.execute(routeId)` is called for an existing route
- **THEN** `Result.success(routeDto)` SHALL be returned

#### Scenario: Non-existent route returns RouteNotFound

- **WHEN** `GetRouteByIdUseCase.execute(routeId)` is called for a non-existent ID
- **THEN** `Result.failure(RouteError.RouteNotFound)` SHALL be returned

### Requirement: GetRoutesUseCase returns a paginated and filtered list of routes

The `train` module SHALL contain `GetRoutesUseCase` in `train/application/usecase/GetRoutesUseCase.java` annotated `@Service`. It SHALL accept `page`, `size`, `sortField`, `SortDirection`, and `RouteFilter` and return `PageResult<RouteDto>`.

#### Scenario: List with no filter returns all routes paginated

- **WHEN** `GetRoutesUseCase.execute(0, 20, "createdAt", DESC, emptyFilter)` is called
- **THEN** a `PageResult<RouteDto>` with up to 20 items and correct `hasNext`/`hasPrevious` flags SHALL be returned

#### Scenario: Filter by originStationId narrows results

- **WHEN** `GetRoutesUseCase.execute(...)` is called with a `RouteFilter` containing a non-null `originStationId`
- **THEN** only routes with that `originStationId` SHALL appear in the result

#### Scenario: Filter by departure date range narrows results

- **WHEN** `GetRoutesUseCase.execute(...)` is called with `departureDateFrom` and `departureDateTo` set
- **THEN** only routes whose `departure_time` falls within that range (inclusive) SHALL be returned

### Requirement: RouteRepositoryAdapter implements RouteRepository using JPA

The `train` module SHALL contain `RouteRepositoryAdapter` in `train/infrastructure/persistence/RouteRepositoryAdapter.java` implementing `RouteRepository`. It SHALL be package-private. It SHALL use `RouteJpaRepository` and `RouteEntityMapper` internally. Filter logic SHALL be implemented via JPQL `@Query` with `IS NULL OR` null-coalescing guards.

#### Scenario: save() persists and returns domain model

- **WHEN** `routeRepositoryAdapter.save(route)` is called
- **THEN** the route SHALL be persisted to the `routes` table and a domain `Route` SHALL be returned

#### Scenario: findById() returns empty Optional for unknown ID

- **WHEN** `routeRepositoryAdapter.findById(unknownId)` is called
- **THEN** `Optional.empty()` SHALL be returned

#### Scenario: findAll() with originStationId filter returns only matching routes

- **WHEN** `routeRepositoryAdapter.findAll(0, 20, "createdAt", DESC, filter)` is called with a specific `originStationId`
- **THEN** only routes with that origin station SHALL be in the returned slice

### Requirement: POST /routes endpoint creates a route (ADMIN only)

The `train` module SHALL expose `POST /{version}/routes` in `RouteController`. It SHALL require `ADMIN` role via `@PreAuthorize("hasRole('ADMIN')")`. On success it SHALL return `201 Created` with `Location` header and `JsendResponse.success(routeResponse)`. On validation failure it SHALL return `400 Bad Request`.

#### Scenario: Admin creates a valid route — 201 Created

- **WHEN** a `POST /1.0/routes` request is sent with valid `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice` fields and `ADMIN` JWT
- **THEN** the response SHALL be `201 Created` with `Location: /1.0/routes/{newId}` and `{"status":"success","data":{...routeFields...}}`

#### Scenario: Missing required field — 400 Bad Request

- **WHEN** a `POST /1.0/routes` request is sent without `departureTime`
- **THEN** the response SHALL be `400 Bad Request` with `{"status":"fail","data":{"code":"VALIDATION_ERROR",...}}`

#### Scenario: Non-admin user is rejected — 403 Forbidden

- **WHEN** a `POST /1.0/routes` request is sent with a `CUSTOMER` JWT
- **THEN** the response SHALL be `403 Forbidden`

### Requirement: GET /routes/{id} endpoint retrieves a single route (public)

The `train` module SHALL expose `GET /{version}/routes/{id}` in `RouteController`. No authentication is required. On success it SHALL return `200 OK` with `JsendResponse.success(routeResponse)`. On not-found it SHALL return `404 Not Found` with `{"status":"fail","data":{"code":"ROUTE_NOT_FOUND",...}}`.

#### Scenario: Existing route found — 200 OK

- **WHEN** `GET /1.0/routes/{existingId}` is requested
- **THEN** the response SHALL be `200 OK` with the route's fields in `data`

#### Scenario: Unknown route ID — 404 Not Found

- **WHEN** `GET /1.0/routes/{unknownId}` is requested
- **THEN** the response SHALL be `404 Not Found` with `{"status":"fail","data":{"code":"ROUTE_NOT_FOUND"}}`

### Requirement: GET /routes endpoint returns paginated and filtered list (public)

The `train` module SHALL expose `GET /{version}/routes` in `RouteController`. No authentication is required. It SHALL accept `page` (default 0), `size` (default 20, max 100), `sort` (default `createdAt,desc`), and optional filter params `originStationId`, `destinationStationId`, `departureDateFrom`, `departureDateTo`. It SHALL return `200 OK` with `JsendResponse.success(SliceHttpResponse<RouteHttpResponse>)`.

#### Scenario: List returns slice response with pagination metadata

- **WHEN** `GET /1.0/routes?page=0&size=5` is requested
- **THEN** the response SHALL be `200 OK` with `data.content` (array), `data.page`, `data.size`, `data.hasNext`, `data.hasPrevious`

#### Scenario: Filter by originStationId narrows list response

- **WHEN** `GET /1.0/routes?originStationId={uuid}` is requested
- **THEN** all items in `data.content` SHALL have `originStationId` equal to the requested UUID

#### Scenario: Invalid page parameter — 400 Bad Request

- **WHEN** `GET /1.0/routes?page=-1` is requested
- **THEN** the response SHALL be `400 Bad Request` with `{"status":"fail","data":{"code":"VALIDATION_ERROR"}}`

#### Scenario: Invalid sort field — 400 Bad Request

- **WHEN** `GET /1.0/routes?sort=unknown,asc` is requested
- **THEN** the response SHALL be `400 Bad Request` with `{"status":"fail","data":{"code":"VALIDATION_ERROR"}}`

### Requirement: ROUTE_NOT_FOUND error code is defined in ErrorCode enum

The shared `ErrorCode` enum in `shared/infrastructure/web/ErrorCode.java` SHALL include a `ROUTE_NOT_FOUND` constant.

#### Scenario: ROUTE_NOT_FOUND is a valid ErrorCode

- **WHEN** `ErrorCode.ROUTE_NOT_FOUND` is referenced in controller code
- **THEN** it SHALL compile without errors

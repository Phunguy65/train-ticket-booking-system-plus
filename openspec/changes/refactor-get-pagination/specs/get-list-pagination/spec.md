# ADDED Requirements

## Requirement: Shared PageRequest web DTO

The system SHALL provide a shared `PageRequest` record in
`shared.infrastructure.web.request` with fields `page` (default 0, min 0) and
`size` (default 20, min 1, max 100), bound via `@ModelAttribute @Valid` in all
GET list controllers.

### Scenario: Valid pagination params accepted

- **WHEN** a client sends `GET /api/v1/stations?page=1&size=10`
- **THEN** the system returns `200 OK` with page 1, size 10 of results

### Scenario: Default values applied when params omitted

- **WHEN** a client sends `GET /api/v1/stations` with no query params
- **THEN** the system returns `200 OK` with page 0, size 20 of results

### Scenario: Invalid page rejected

- **WHEN** a client sends `GET /api/v1/stations?page=-1`
- **THEN** the system returns `400 Bad Request`

### Scenario: Invalid size rejected

- **WHEN** a client sends `GET /api/v1/stations?size=0` or `?size=101`
- **THEN** the system returns `400 Bad Request`

## Requirement: Per-module Query objects in application/query/

The system SHALL define one Query record per list use-case in
`{module}.application.query`, containing `page`, `size`, and any
resource-specific context (e.g. `trainId` for coaches). Controllers SHALL map
`PageRequest` to the appropriate Query object before invoking the use case.

### Scenario: Query object carries pagination context to use case

- **WHEN** `StationController` receives a valid `PageRequest`
- **THEN** it constructs a `GetStationsQuery(page, size)` and passes it to
  `GetStationsUseCase`

### Scenario: Coach query carries trainId

- **WHEN** `CoachController` receives
  `GET /trains/{trainId}/coaches?page=0&size=20`
- **THEN** it constructs `GetCoachesQuery(page, size, trainId)` and passes it to
  `GetCoachesByTrainUseCase`

## Requirement: Fixed default ordering per resource, controlled by application layer

Each list use-case SHALL construct a fixed `Sort` internally and pass it to the
repository port. No sort or orderBy parameter SHALL be accepted from HTTP
clients. Tie-break SHALL always be `id ASC`.

Default orders:

| Resource       | Primary order       |
| -------------- | ------------------- |
| Station        | `code ASC`          |
| User           | `createdAt DESC`    |
| Train          | `trainNumber ASC`   |
| Route          | `departureTime ASC` |
| Coach          | `carNumber ASC`     |
| Seat           | `seatNumber ASC`    |
| AvailableSeats | `seatNumber ASC`    |

### Scenario: Stations returned in code order

- **WHEN** a client sends `GET /api/v1/stations`
- **THEN** results are ordered by `code ASC`, tie-broken by `id ASC`

### Scenario: Routes returned by departure time

- **WHEN** a client sends `GET /api/v1/routes`
- **THEN** results are ordered by `departureTime ASC`, tie-broken by `id ASC`

### Scenario: Sort param ignored / not accepted

- **WHEN** a client sends `GET /api/v1/stations?sort=name,asc`
- **THEN** the system ignores the `sort` param and returns results in the fixed
  default order

## Requirement: All GET list endpoints return SliceHttpResponse

Every GET list endpoint SHALL return `ResponseEntity<JsendResponse<?>>` wrapping
a `SliceHttpResponse<T>`. Endpoints that previously returned `List<T>` (coaches,
seats, available-seats) SHALL be updated to return `SliceHttpResponse<T>`.

### Scenario: Coach list returns paginated response

- **WHEN** a client sends `GET /api/v1/trains/{trainId}/coaches`
- **THEN** the system returns `200 OK` with a JSend success envelope containing
  `SliceHttpResponse<CoachResponse>` with `content`, `page`, `size`, `hasNext`,
  `hasPrevious` fields

### Scenario: Seat list returns paginated response

- **WHEN** a client sends `GET /api/v1/trains/{trainId}/seats`
- **THEN** the system returns `200 OK` with a JSend success envelope containing
  `SliceHttpResponse<SeatResponse>`

### Scenario: Available seats returns paginated response

- **WHEN** a client sends `GET /api/v1/routes/{routeId}/seats/available`
- **THEN** the system returns `200 OK` with a JSend success envelope containing
  `SliceHttpResponse<SeatResponse>`

## Requirement: Route list endpoint drops filter params

`GET /api/v1/routes` SHALL NOT accept `originStationId`, `destinationStationId`,
`departureDateFrom`, or `departureDateTo` query parameters. The `RouteFilter`
domain record SHALL be deleted.

### Scenario: Route list returns all active routes paginated

- **WHEN** a client sends `GET /api/v1/routes?page=0&size=20`
- **THEN** the system returns `200 OK` with a paginated list of all active
  routes ordered by `departureTime ASC`

### Scenario: Filter params no longer accepted

- **WHEN** a client sends `GET /api/v1/routes?originStationId=<uuid>`
- **THEN** the param is ignored (Spring MVC does not bind unknown
  `@ModelAttribute` fields to the `PageRequest` record)

## Requirement: Repository ports accept Sort from application layer

All `findAll` repository port methods SHALL accept
`org.springframework.data.domain.Sort` as a parameter instead of
`String sortField` and `SortDirection direction`. Infrastructure adapters SHALL
build `org.springframework.data.domain.PageRequest` from `(page, size, sort)`.

### Scenario: Adapter builds correct Pageable

- **WHEN** `GetStationsUseCase` calls
  `stationRepository.findAll(0, 20, Sort.by("code").ascending().and(Sort.by("id").ascending()))`
- **THEN** the adapter constructs `PageRequest.of(0, 20, sort)` and passes it to
  the JPA repository

## Requirement: SeatRepository supports paginated findAll by TrainId and by RouteId

`SeatRepository` SHALL expose two new paginated methods:
`findAll(int page, int size, Sort sort, TrainId trainId)` and
`findAllAvailable(int page, int size, Sort sort, RouteId routeId)`. The infra
adapter for `findAll(trainId)` SHALL perform a join from Seat to Coach to filter
by trainId in a single query.

### Scenario: Seats by train returned paginated

- **WHEN** `GetSeatsByTrainUseCase` calls
  `seatRepository.findAll(page, size, sort, trainId)`
- **THEN** the adapter returns a `PageResult<Seat>` containing only seats
  belonging to coaches of that train

### Scenario: Available seats by route returned paginated

- **WHEN** `GetAvailableSeatsForRouteUseCase` calls
  `seatRepository.findAllAvailable(page, size, sort, routeId)`
- **THEN** the adapter returns a `PageResult<Seat>` containing only seats with
  AVAILABLE status for that route

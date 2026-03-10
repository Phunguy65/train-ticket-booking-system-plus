## ADDED Requirements

### Requirement: RouteSeatAvailability domain model tracks per-route seat status

The `RouteSeatAvailability` entity SHALL reside in `train/domain/model/` and SHALL hold `RouteId`, `SeatId`, `RouteSeatAvailabilityStatus`, and a `version` integer for optimistic locking. It SHALL NOT extend `AggregateRoot` — it is an entity managed within the seat management context.

`RouteSeatAvailabilityStatus` SHALL be an enum with values: `AVAILABLE`, `BOOKED`, `CANCELLED`.

State transitions allowed:
- `AVAILABLE → BOOKED` (via `book()`)
- `BOOKED → CANCELLED` (via `cancel()`)
- `CANCELLED → AVAILABLE` (via `release()`)

#### Scenario: Booking an AVAILABLE seat transitions status to BOOKED

- **WHEN** `routeSeatAvailability.book()` is called on an entity with status `AVAILABLE`
- **THEN** the status SHALL change to `BOOKED`

#### Scenario: Booking a non-AVAILABLE seat returns failure

- **WHEN** `routeSeatAvailability.book()` is called on an entity with status `BOOKED`
- **THEN** the method SHALL return `Result.failure(RouteSeatAvailabilityError.SeatNotAvailable)`

#### Scenario: Cancelling a BOOKED seat transitions status to CANCELLED

- **WHEN** `routeSeatAvailability.cancel()` is called on an entity with status `BOOKED`
- **THEN** the status SHALL change to `CANCELLED`

#### Scenario: Releasing a CANCELLED seat transitions status back to AVAILABLE

- **WHEN** `routeSeatAvailability.release()` is called on an entity with status `CANCELLED`
- **THEN** the status SHALL change to `AVAILABLE`

### Requirement: RouteSeatAvailabilityRepository defines domain-facing persistence contract

The `RouteSeatAvailabilityRepository` interface in `train/domain/repository/` SHALL define persistence operations using domain types only.

#### Scenario: Repository finds available seats for a route

- **WHEN** `routeSeatAvailabilityRepository.findAvailableByRouteId(routeId)` is called
- **THEN** it SHALL return a list of `RouteSeatAvailability` entities where status is `AVAILABLE`

#### Scenario: Repository finds a specific seat availability by route and seat

- **WHEN** `routeSeatAvailabilityRepository.findByRouteIdAndSeatId(routeId, seatId)` is called
- **THEN** it SHALL return an `Optional<RouteSeatAvailability>` for the matching entry

#### Scenario: Repository bulk-saves a list of availability records

- **WHEN** `routeSeatAvailabilityRepository.saveAll(list)` is called with a list of new `RouteSeatAvailability` entities
- **THEN** all records SHALL be persisted

### Requirement: Availability is pre-populated when a route is created

When a `RouteCreated` domain event is published by the route management flow, a Spring Modulith `@ApplicationModuleListener` in the `train` module SHALL seed `route_seat_availability` rows for every seat belonging to the route's train, all with status `AVAILABLE`.

#### Scenario: RouteCreated event seeds availability for all train seats

- **WHEN** a `RouteCreated` event is received with a `trainId` and `routeId`
- **THEN** one `RouteSeatAvailability` record with status `AVAILABLE` SHALL be inserted for every seat belonging to that train

#### Scenario: Listener is idempotent — duplicate RouteCreated does not double-insert

- **WHEN** a `RouteCreated` event is received for a `routeId` that already has availability records
- **THEN** no duplicate records SHALL be inserted (use `INSERT … ON CONFLICT DO NOTHING` or check-before-insert)

### Requirement: GetAvailableSeatsForRouteUseCase returns available seats

The `GetAvailableSeatsForRouteUseCase` in `train/application/usecase/` SHALL return a list of seat DTOs where availability status is `AVAILABLE` for a given route.

#### Scenario: Returns seats with AVAILABLE status for the route

- **WHEN** `getAvailableSeatsForRouteUseCase.execute(routeId)` is called
- **THEN** a list of `SeatDto` objects for all seats with `AVAILABLE` status on that route SHALL be returned

#### Scenario: Returns empty list when all seats are booked

- **WHEN** `getAvailableSeatsForRouteUseCase.execute(routeId)` is called and all seats have status `BOOKED`
- **THEN** an empty list SHALL be returned

### Requirement: RouteSeatAvailabilityPort exposes cross-module booking validation

The `train` module SHALL expose a `@NamedInterface("availability")` package containing the `RouteSeatAvailabilityPort` interface. This port SHALL provide a single method for the `booking` module to atomically validate and reserve a seat for a route. The `booking` module SHALL declare `allowedDependencies = {"train::availability"}`.

#### Scenario: Port reserves an AVAILABLE seat atomically

- **WHEN** `routeSeatAvailabilityPort.reserveSeat(routeId, seatId)` is called and the seat is `AVAILABLE`
- **THEN** the seat status SHALL transition to `BOOKED`, the `version` SHALL increment, and the method SHALL return success

#### Scenario: Port returns conflict when seat is already BOOKED

- **WHEN** `routeSeatAvailabilityPort.reserveSeat(routeId, seatId)` is called and the seat is `BOOKED`
- **THEN** the method SHALL return `Result.failure(RouteSeatAvailabilityError.SeatNotAvailable)` without modifying any state

#### Scenario: Concurrent reservation — only one request succeeds

- **WHEN** two concurrent calls to `routeSeatAvailabilityPort.reserveSeat(routeId, seatId)` are made for the same seat
- **THEN** exactly one SHALL succeed and the other SHALL receive a conflict result due to optimistic lock version mismatch

### Requirement: GET /api/v1/routes/{routeId}/seats/available exposes available seats

The `SeatController` (or a dedicated `RouteSeatAvailabilityController`) SHALL expose `GET /api/v1/routes/{routeId}/seats/available` returning available seats for a given route.

#### Scenario: Returns available seats for a valid route

- **WHEN** a `GET /api/v1/routes/{routeId}/seats/available` request is received with a valid `routeId`
- **THEN** the controller SHALL return `200 OK` with an array of available seat objects

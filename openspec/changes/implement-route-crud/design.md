## Context

The `train` module already contains `RouteId`, `RouteCreated` (domain event), `RouteSeatAvailabilitySeeder` (listener), and the `routes` table fully migrated to UUID. What is missing is the `Route` aggregate itself, its repository port, all application use cases, and the REST controller. The `routes` table schema includes `train_id`, `origin_station_id`, `destination_station_id`, `departure_time`, `arrival_time`, `base_price`, and `status`. A `stations` table also exists (UUID PK, `code`, `name`, `city`) but has no corresponding Java domain classes yet.

This change adds the complete Route vertical slice to the `train` module, following the identical layer structure already used by `Train` and `Seat`.

## Goals / Non-Goals

**Goals:**

- Implement `Route` as a full `AggregateRoot<RouteId>` in the domain layer with `create()` + `reconstitute()` factory methods.
- Introduce `StationId` value object (type-safe UUID wrapper) to reference origin/destination stations without pulling in a full `Station` aggregate.
- Expose three REST endpoints: `POST /routes`, `GET /routes/{id}`, `GET /routes` (paginated + sorted + filtered).
- Support filtering on the list endpoint by `originStationId`, `destinationStationId`, and `departureDateFrom`/`departureDateTo`.
- `Route.create()` publishes `RouteCreated` domain event so `SeatAvailabilitySeeder` seeds availability rows automatically (no changes needed to the seeder).
- Write tests at every layer: domain unit tests, use-case Mockito tests, `@DataJpaTest` repository tests, `@WebMvcTest` controller tests.

**Non-Goals:**

- Station domain aggregate (CRUD for stations) — stations are referenced only as `StationId` UUIDs.
- Route status transitions (SCHEDULED → CANCELLED / COMPLETED) — `SCHEDULED` is the only status created in this change.
- Pagination `total` count — consistent with `Train` and `Seat`, the list response uses `SliceHttpResponse` (no `COUNT(*)` query).
- Price calculation or fare rules — `base_price` is a plain `BigDecimal` supplied by the caller.

## Decisions

### Decision 1 — Route lives in the `train` module (not a separate module)

`Route` is tightly coupled to `Train` and `RouteSeatAvailability` within the same bounded context. Introducing a new `route` module would force cross-module dependencies without architectural benefit at this stage. All new files are placed under `train/`.

### Decision 2 — `StationId` as a value object (no `Station` aggregate)

The proposal confirmed: station references are held as `StationId` (UUID wrapper). The domain model does not need to load or validate a `Station` entity when creating a route; referential integrity is enforced by the DB foreign key constraint (`origin_station_id → stations.id`). This keeps `Route` simple and avoids an unscoped `Station` vertical slice.

`StationId` follows the same pattern as `TrainId`, `SeatId`, `RouteId`:

```java
public record StationId(UUID value) { … }
```

### Decision 3 — Filtering via `RouteFilter` record in `RouteRepository`

`GetRoutesUseCase` needs to pass optional filter criteria (originStationId, destinationStationId, departureDateFrom, departureDateTo) alongside pagination. Rather than adding many parameters to `findAll`, a dedicated `RouteFilter` record (in `domain/model/`) is introduced:

```java
public record RouteFilter(
    UUID originStationId,       // nullable
    UUID destinationStationId,  // nullable
    Instant departureDateFrom,  // nullable
    Instant departureDateTo     // nullable
) {}
```

`RouteRepository.findAll(int page, int size, String sortField, SortDirection direction, RouteFilter filter)` accepts this record. The persistence adapter builds a JPA `Specification` (or a `@Query` with `IS NULL OR` guards) from it.

### Decision 4 — `GetRouteByIdUseCase` returns `Result<RouteDto, RouteError>`

Consistent with `GetTrainByIdUseCase`, which also uses `Result` (not `Optional`), so the controller can `.fold()` and return the appropriate HTTP status in one expression.

### Decision 5 — `POST /routes` requires `ADMIN` role

Consistent with `POST /trains`. The `GET` endpoints are unauthenticated (public), consistent with `GET /trains` and `GET /trains/{id}`.

### Decision 6 — DB migration adds composite index for filter queries

A new migration `V5.0.0__add_route_filter_index.sql` adds:

```sql
CREATE INDEX idx_routes_origin_dest_departure
    ON routes (origin_station_id, destination_station_id, departure_time);
```

This index supports the most common query pattern (search by origin+destination+date range) without needing to change the existing schema.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|---|---|---|
| JPA `Specification` adds complexity to the persistence adapter | Low | Use a simple `@Query` with `IS NULL OR …` guards for the four filter fields; switch to `Specification` only if more filter axes are needed later. |
| `RouteCreated` event already exists — new `Route.create()` must publish it correctly | Low | The event record signature `RouteCreated(routeId, trainId, occurredAt)` is already defined; `Route.create()` just calls `registerEvent(RouteCreated.of(id, trainId))`. |
| `StationId` not exported via `@NamedInterface` — `booking` module cannot use it | Acceptable | Booking currently references stations only via raw UUID. If needed later, `StationId` can be added to the `train::model` named interface. |
| Filter date range with no upper/lower bound could return a huge result set | Low | `size` is capped at 100 (same as `TrainController`); callers must paginate. |

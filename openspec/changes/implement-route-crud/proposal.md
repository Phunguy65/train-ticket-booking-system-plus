# Why

The `routes` table and its domain infrastructure (schema, `RouteId` value object, `RouteCreated` event, `RouteSeatAvailabilitySeeder`) are already in place, but the Route aggregate has no REST API surface — there is no way for clients to create routes or query them. Bookings depend on valid Route IDs, so until Route CRUD is exposed, all booking flows require manually-inserted database records. This change delivers the missing Create, Get-by-ID, and Get-list endpoints for routes so that admin workflows and booking clients can function end-to-end.

## What Changes

- **New** `Route` domain aggregate (`Route.java`) implementing `AggregateRoot<RouteId>` with fields: `id`, `trainId`, `originStationId`, `destinationStationId`, `departureTime`, `arrivalTime`, `basePrice`, `status`, `createdAt`. Factory method `create()` publishes `RouteCreated` event (already consumed by `SeatAvailabilitySeeder`).
- **New** `RouteStatus` enum: `SCHEDULED` (only status needed for now; future statuses out of scope).
- **New** `StationId` value object (mirrors pattern of `TrainId`, `SeatId`, etc.).
- **New** `RouteError` sealed interface: `RouteNotFound`.
- **New** `RouteRepository` domain port: `save`, `findById`, `findAll` (paginated, sortable, filterable).
- **New** application layer: `CreateRouteCommand`, `RouteDto`, `CreateRouteUseCase`, `GetRouteByIdUseCase`, `GetRoutesUseCase`.
- **New** persistence adapter: `RouteEntity`, `RouteJpaRepository`, `RouteRepositoryAdapter`, `RouteEntityMapper`.
- **New** REST web layer: `RouteController` (`POST /routes`, `GET /routes/{id}`, `GET /routes`), `CreateRouteHttpRequest`, `RouteHttpResponse`, `RouteRequestMapper`.
- **New** database migration for any missing indexes (existing `routes` table schema is already fully migrated to UUID).
- **New** tests for each layer (unit, persistence integration, web integration).

## Capabilities

### New Capabilities

- `backend-route-slice`: Full vertical-slice CRUD for the Route aggregate — domain model, application use cases, persistence adapter, and REST endpoints (`POST /routes`, `GET /routes/{id}`, `GET /routes`) with pagination, sorting, and filtering support.

### Modified Capabilities

<!-- No existing spec-level requirements change — only new surface is added -->

## Impact

- **Backend `train` module**: All new files live inside `train/` (Route belongs to the train bounded context alongside Train and Seat).
- **`SeatAvailabilitySeeder`**: No code changes — it already listens for `RouteCreated`; the new `Route.create()` factory will publish that event.
- **Booking module**: No changes — it references routes only by UUID; the new API makes those UUIDs discoverable.
- **Database**: No schema changes needed (routes table is fully migrated); a new migration may add a composite index on `(origin_station_id, destination_station_id, departure_time)` for list-filter performance.
- **OpenAPI spec** (`shared/api-contracts/openapi.yaml`): Three new path entries to be documented.

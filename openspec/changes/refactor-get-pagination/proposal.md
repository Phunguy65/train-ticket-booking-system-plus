# Why

GET list endpoints across all modules accept individual `@RequestParam` for
pagination (and in RouteController, 4 extra filter params), duplicate
validation/sort-parsing logic in every controller, and three sub-resource
endpoints (coaches, seats, available-seats) return raw `List<T>` with no
pagination at all. Standardising these now removes the duplication and makes
every list endpoint consistent before the API surface grows further.

## What Changes

- **NEW** `shared.infrastructure.web.request.PageRequest` — shared web DTO
  (`page`, `size`) bound via `@ModelAttribute`; replaces individual
  `@RequestParam` in every list controller
- **NEW** `application/query/` package per module — one Query record per list
  use-case (e.g. `GetStationsQuery`, `GetRoutesQuery`);
  `PageRequest.toQuery(...)` maps to it
- **BREAKING** All GET list endpoints drop `sort` / `orderBy` query params —
  order is now fixed per resource, controlled by the application layer
- **BREAKING** `GET /routes` drops `originStationId`, `destinationStationId`,
  `departureDateFrom`, `departureDateTo` filter params
- **BREAKING** `GET /trains/{trainId}/coaches`, `GET /trains/{trainId}/seats`,
  `GET /routes/{routeId}/seats/available` now accept `page`/`size` and return
  `SliceHttpResponse<T>` instead of `List<T>`
- Repository port signatures updated: `sortField` + `SortDirection` removed;
  UseCase passes a Spring Data `Sort` built internally
- `RouteFilter` domain record deleted (no longer needed)
- `SortDirection` shared domain enum deleted (no longer referenced)

## Capabilities

### New Capabilities

- `get-list-pagination`: Unified pagination contract for all GET list endpoints
  — shared `PageRequest` web DTO, per-module Query objects in
  `application/query/`, fixed default ordering per resource, and consistent
  `SliceHttpResponse<T>` return shape

### Modified Capabilities

- `user-get`: `GET /users` list endpoint now uses `PageRequest` DTO; `sort`
  param removed; order fixed to `createdAt DESC, id ASC`

## Impact

- **Controllers** (7): `StationController`, `UserController`, `TrainController`,
  `RouteController`, `CoachController`, `SeatController` — param signatures
  change
- **Use cases** (7): all `Get*UseCase` list variants — signature changes from
  primitives to Query object
- **Domain repository ports** (5): `StationRepository`, `UserRepository`,
  `TrainRepository`, `RouteRepository`, `CoachRepository`, `SeatRepository` —
  `findAll` signatures updated; `CoachRepository` and `SeatRepository` gain new
  paginated `findAll` methods
- **Infrastructure adapters + JPA repos** (6): updated to match new port
  signatures; `CoachJpaRepository` and `SeatJpaRepository` gain `Pageable`-based
  queries
- **Deleted**: `RouteFilter.java`, `SortDirection.java`
- **No changes** to auth, booking, payment modules

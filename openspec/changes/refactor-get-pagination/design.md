# Context

All GET list endpoints in the backend currently accept individual
`@RequestParam` for `page`, `size`, and `sort`. Four controllers duplicate the
same validation and sort-string-parsing logic. Three sub-resource endpoints
(`coaches`, `seats`, `available-seats`) return raw `List<T>` with no pagination.
`RouteController` additionally accepts four filter params as flat
`@RequestParam`.

The codebase already has a solid pagination foundation: `PageResult<T>`
(domain), `SliceHttpResponse<T>` (web), and the pattern is consistently applied
in Station/User/Train/Route. The gap is (a) no shared request object, (b) sort
is client-controlled with a whitelist, and (c) Coach/Seat have no pagination at
all.

## Goals / Non-Goals

**Goals:**

- Single shared `PageRequest` web DTO (`page`, `size`) bound via
  `@ModelAttribute @Valid` — eliminates duplicated param declarations
- Per-module Query records in `application/query/` — decouples web DTO from
  use-case signature
- Fixed default ordering per resource, decided in the UseCase — no sort param
  exposed to HTTP clients
- All GET list endpoints return `SliceHttpResponse<T>`
- Repository ports accept `org.springframework.data.domain.Sort` passed from
  UseCase — infra adapters build `PageRequest` from `(page, size, sort)`

**Non-Goals:**

- Total-count pagination (`Page<T>`) — slice-based pagination is the established
  pattern, not changing
- Filter/search params — removed from Route, not added elsewhere
- Cursor-based pagination
- Changes to auth, booking, or payment modules

## Decisions

### D1 — Shared `PageRequest` in `shared.infrastructure.web.request`

`PageRequest` is a Java record with `@Min`/`@Max` Bean Validation annotations.
Spring MVC binds query params automatically via `@ModelAttribute`. Default
values are set via field initializers (`page = 0`, `size = 20`).

Considered: keeping individual `@RequestParam` with `defaultValue` — rejected
because it requires repeating the same three annotations in every controller and
cannot be validated centrally.

### D2 — Query records in `application/query/`, not `application/command/`

CQRS convention: commands mutate state, queries read it. Existing `command/`
package holds write operations. A separate `query/` package makes the
distinction explicit and aligns with the `backend-vertical-slice-structure` spec
requirement that application-layer files be "use case class or application-level
DTO (command, query, result)".

### D3 — UseCase controls Sort, not the HTTP client

Each UseCase constructs a `org.springframework.data.domain.Sort` internally and
passes it to the repository port. The port signature becomes
`findAll(int page, int size, Sort sort)`. The infra adapter builds
`PageRequest.of(page, size, sort)`.

This means sort order is a code-level decision, not a runtime one. Changing
order requires a code change + deploy — acceptable for admin/reference data
lists.

Default orders chosen by domain semantics:

| Resource       | Primary             | Tie-break |
| -------------- | ------------------- | --------- |
| Station        | `code ASC`          | `id ASC`  |
| User           | `createdAt DESC`    | `id ASC`  |
| Train          | `trainNumber ASC`   | `id ASC`  |
| Route          | `departureTime ASC` | `id ASC`  |
| Coach          | `carNumber ASC`     | `id ASC`  |
| Seat           | `seatNumber ASC`    | `id ASC`  |
| AvailableSeats | `seatNumber ASC`    | `id ASC`  |

Considered: passing `Sort` as a param from controller — rejected because it
re-introduces client-controlled ordering through the back door.

### D4 — Repository port accepts `Sort` (Spring Data type)

`Sort` is a Spring Data type. Allowing it in the domain port is a pragmatic
trade-off: the domain port is already in `shared.domain` which is framework-free
by convention, but `Sort` is a value object with no side effects and is already
used in the infra layer. The alternative — a custom `OrderBy` domain type — adds
indirection with no benefit given the project's ArchUnit rules only prohibit
JPA/Jackson annotations in domain, not Spring Data value types.

Considered: custom `List<OrderClause>` domain type — rejected as
over-engineering for a project that already uses Spring Data `Slice` in infra
adapters.

### D5 — `SeatRepository.findAll` accepts `TrainId`, infra joins Coach→Seat

`Seat` domain model only has `coachId`. The endpoint is
`GET /trains/{trainId}/seats`. Rather than resolving coachIds in the UseCase
(N+1 risk), the repository port accepts `TrainId` and the JPA adapter performs a
single join query.

### D6 — `RouteFilter` and `SortDirection` deleted

`RouteFilter` is only used in `RouteRepository.findAll` which is being
refactored. `SortDirection` is only used in the four `findAll` port signatures
being replaced by `Sort`. Both become dead code after this change.

## Risks / Trade-offs

- **Breaking API change** — Clients using `sort`, `originStationId`,
  `destinationStationId`, `departureDateFrom`, `departureDateTo` on Route will
  break. Mitigation: document in changelog; frontend (Next.js) is in the same
  repo and will be updated in the same PR.
- **Fixed sort order** — Clients cannot reorder results. Mitigation: orders are
  chosen to match the most natural UI presentation; can be revisited by adding
  an `orderBy` param later if needed.
- **`Sort` in domain port** — Minor architectural impurity. Mitigation: ArchUnit
  rules do not flag this; can be replaced with a custom type in a future cleanup
  if needed.

## Migration Plan

1. Create shared `PageRequest` and per-module Query records (no behaviour change
   yet)
2. Update use cases to accept Query objects and build `Sort` internally
3. Update repository ports and infra adapters
4. Update controllers to use `@ModelAttribute PageRequest`
5. Add pagination to Coach/Seat use cases, ports, adapters, JPA repos
6. Delete `RouteFilter`, `SortDirection`
7. Build passes (`./gradlew build`)

Rollback: revert the PR. No DB migrations involved.

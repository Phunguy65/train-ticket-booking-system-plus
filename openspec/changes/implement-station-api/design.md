## Context

The backend is a Spring Boot application following Hexagonal Architecture organized as vertical slices (Spring Modulith). The `stations` table already exists in PostgreSQL (created in migrations V1 and V2), and `StationId` is currently defined as a value object inside the `train` module. No Station domain aggregate or API layer exists yet.

The three new endpoints (`POST /api/v1/stations`, `GET /api/v1/stations/{id}`, `GET /api/v1/stations`) will serve as reference-data management for the rest of the system. Routes and schedules reference stations by foreign key, so stations must exist before any route can be created.

## Goals / Non-Goals

**Goals:**

- Introduce a `station` vertical slice module following the same Hexagonal/Clean Architecture patterns already used by `train`, `booking`, and other modules
- Expose three REST endpoints: create station, get by ID, paginated list
- Relocate `StationId` to the `station` module and expose it via a `@NamedInterface` for cross-module use
- Register typed error codes in the shared `ErrorCode` enum
- Secure the create endpoint to ADMIN role; list/get endpoints are accessible by authenticated users

**Non-Goals:**

- Station update or delete endpoints (out of scope for V1)
- Multilingual station names (not in current schema)
- Geospatial coordinate fields (schema has no coordinates column yet)
- Caching layer for station list (deferred to a future performance change)
- Full-text search or fuzzy name matching

## Decisions

### Decision 1: New `station` Spring Modulith module (not extending `train`)

**Chosen**: Create `station/` as an independent top-level module.

**Rationale**: `StationId` was placed inside `train` as a convenience reference. Stations are a distinct bounded context — they are shared reference data, not a sub-concept of trains. Keeping stations inside `train` would violate module boundaries as routes and other future modules also need station references.

**Alternative considered**: Keep station code inside `train` module under a sub-package. Rejected because Spring Modulith would require all other modules to depend on `train` just to access station data, creating unwanted coupling.

---

### Decision 2: Move `StationId` from `train.domain.model` to `station.domain.model`

**Chosen**: Move `StationId` to `station.domain.model` and expose it with `@NamedInterface("model")`. Update `train` module to declare `allowedDependencies = {"station::model"}`.

**Rationale**: The value object belongs to the module that owns the aggregate. Cross-module access is controlled through Spring Modulith's Named Interface mechanism, which is already used elsewhere in the codebase.

**Alternative considered**: Duplicate `StationId` in both modules. Rejected — duplication breaks type safety and creates inconsistency across module boundaries.

---

### Decision 3: Slice-based pagination (no total count) for the station list

**Chosen**: Use `Slice`-based pagination (`PageResult<StationDto>`) consistent with other list endpoints (`GetTrainsUseCase`).

**Rationale**: Station lists do not require a total count for UI display (infinite scroll / "load more" pattern). Avoiding `COUNT(*)` on every page request improves performance as data grows. This matches the existing `SliceHttpResponse` pattern already present in the shared web layer.

**Alternative considered**: `Page`-based pagination with total count. Deferred — can be added later if product requires it, with no breaking API change needed (just add `totalElements` field).

---

### Decision 4: Result monad for error propagation in use cases

**Chosen**: Use `Result<StationDto, StationError>` (shared `Result<T,E>` type) instead of throwing exceptions.

**Rationale**: Consistent with all existing use cases in the codebase. Typed errors via sealed interfaces (`StationError`) enable exhaustive pattern matching at the controller level, preventing unhandled error cases at compile time.

---

### Decision 5: POST `/stations` — Admin-only, GET endpoints — authenticated users

**Chosen**: `POST` requires `ADMIN` role; `GET` endpoints require authentication (not public).

**Rationale**: Station data is reference data managed by operators. Listing stations is needed by authenticated users when creating bookings or browsing routes, but it should not be exposed publicly without authentication to prevent data scraping.

## Risks / Trade-offs

- **`StationId` migration in `train` module** → If `Route` or other domain models reference `StationId` by direct import, those imports must be updated. Mitigation: scope is small — only `Route.java` and `RouteEntity.java` reference `StationId`; update in the same PR.

- **No station update/delete endpoints** → Operators cannot correct station data after creation without a future change. Mitigation: document limitation; future `update-station` change can add PATCH endpoint without breaking changes.

- **Schema already exists but no Flyway migration for new fields** → If future requirements add columns (coordinates, timezone), a new migration will be needed. Mitigation: additive migrations are safe; this is not a risk for V1.

## ADDED Requirements

### Requirement: PageResult domain abstraction
The system SHALL provide a `PageResult<T>` record in `shared/domain/` that represents a page of sliced results without total count. `PageResult<T>` SHALL be a pure Java type with zero dependencies on Spring, JPA, or any external framework, consistent with existing shared domain types (`Result<T,E>`, `Money`, `UserId`).

`PageResult<T>` SHALL contain: `items` (list of results), `pageNumber` (0-indexed), `pageSize` (requested size), `hasNext` (more data exists), `hasPrevious` (not on first page).

#### Scenario: PageResult created from Slice
- **WHEN** a `Slice<X>` from Spring Data is mapped using `PageResult.fromSlice(slice, mapper)`
- **THEN** the resulting `PageResult<T>` SHALL have `hasNext` equal to `slice.hasNext()`
- **AND** `hasPrevious` SHALL be `true` when `pageNumber > 0`
- **AND** `items` SHALL contain exactly the mapped elements (not the extra probe element)

#### Scenario: PageResult for first page with more data
- **WHEN** `pageNumber` is `0` and backend detects more rows exist
- **THEN** `hasPrevious` SHALL be `false`
- **AND** `hasNext` SHALL be `true`

#### Scenario: PageResult for empty result set
- **WHEN** no records exist matching the query
- **THEN** `items` SHALL be an empty list
- **AND** both `hasNext` and `hasPrevious` SHALL be `false`

### Requirement: SortDirection domain enum
The system SHALL provide a `SortDirection` enum in `shared/domain/` with values `ASC` and `DESC`, usable in domain repository port signatures without any Spring dependency.

#### Scenario: SortDirection used in repository port
- **WHEN** a domain repository port defines a method signature with `SortDirection`
- **THEN** the interface SHALL compile with zero Spring or JPA imports

### Requirement: SliceHttpResponse web envelope
The system SHALL provide a generic `SliceHttpResponse<T>` record in `shared/infrastructure/web/` that serializes to JSON with fields: `content` (list), `page` (0-indexed number), `size` (page size), `hasNext` (boolean), `hasPrevious` (boolean).

All slice-based list endpoints throughout the system SHALL wrap their response in `SliceHttpResponse<T>`, then in `JsendResponse.success(...)`.

#### Scenario: SliceHttpResponse JSON structure
- **WHEN** a controller returns `JsendResponse.success(new SliceHttpResponse<>(content, page, size, hasNext, hasPrevious))`
- **THEN** the JSON response SHALL be:
  ```json
  {
    "status": "success",
    "data": {
      "content": [...],
      "page": 0,
      "size": 20,
      "hasNext": true,
      "hasPrevious": false
    }
  }
  ```

#### Scenario: SliceHttpResponse reusable across modules
- **WHEN** a future module (e.g., booking, route) adds a list endpoint
- **THEN** it SHALL use the same `SliceHttpResponse<T>` from `shared/infrastructure/web/`
- **AND** the JSON envelope structure SHALL be identical

### Requirement: No COUNT query for slice-based pagination
The system SHALL use Spring Data's `Slice<T>` (not `Page<T>`) at the infrastructure layer for all list endpoints based on `PageResult<T>`. The generated SQL SHALL NOT include a `COUNT(*)` query.

#### Scenario: SQL generated for Slice query
- **WHEN** `UserRepositoryAdapter.findAll(page=0, size=20, ...)` is called
- **THEN** exactly ONE SQL SELECT statement SHALL be issued to the database
- **AND** it SHALL use `LIMIT 21` (size + 1) to detect `hasNext`
- **AND** NO `SELECT COUNT(*)` statement SHALL be issued

#### Scenario: hasNext detection via extra row
- **WHEN** the database returns `size + 1` rows for a page query
- **THEN** `PageResult.hasNext` SHALL be `true`
- **AND** the `items` list SHALL contain exactly `size` elements (extra row is discarded)

### Requirement: Sort field whitelist for list endpoints
All slice-based list endpoints SHALL validate the sort field against an explicit whitelist. Requests with non-whitelisted sort fields SHALL be rejected.

#### Scenario: Valid sort field accepted
- **WHEN** a request includes `?sort=createdAt,desc` and `createdAt` is in the whitelist
- **THEN** the system returns `200 OK` with results sorted by `createdAt DESC`

#### Scenario: Invalid sort field rejected
- **WHEN** a request includes `?sort=passwordHash,asc`
- **THEN** the system returns `400 Bad Request` with JSend fail envelope
- **AND** the error message SHALL indicate the field is not allowed

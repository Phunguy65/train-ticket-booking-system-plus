# MODIFIED Requirements

## Requirement: List users with pagination

The system SHALL allow an authenticated caller to retrieve a paginated list of
users via `GET /api/v1/users` using the shared `PageRequest` DTO (`page`,
`size`). The `sort` query parameter SHALL NOT be accepted. Results SHALL be
ordered by `createdAt DESC`, tie-broken by `id ASC`.

### Scenario: Users returned in default order

- **WHEN** an authenticated request is made to `GET /api/v1/users`
- **THEN** the system returns `200 OK` with a JSend success envelope containing
  `SliceHttpResponse<UserResponse>` ordered by `createdAt DESC, id ASC`

### Scenario: Pagination params applied

- **WHEN** an authenticated request is made to
  `GET /api/v1/users?page=1&size=10`
- **THEN** the system returns page 1 with up to 10 users

### Scenario: Invalid pagination params rejected

- **WHEN** an authenticated request is made to `GET /api/v1/users?page=-1` or
  `?size=0`
- **THEN** the system returns `400 Bad Request`

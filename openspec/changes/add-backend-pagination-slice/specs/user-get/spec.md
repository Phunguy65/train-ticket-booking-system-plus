## ADDED Requirements

### Requirement: List all users (admin)
The system SHALL expose `GET /api/v1/users` that returns a paginated slice of all users. This endpoint SHALL require `ROLE_ADMIN` authentication. The endpoint SHALL accept query parameters: `page` (default `0`), `size` (default `20`, max `100`), `sort` (default `createdAt,desc`).

The response SHALL use `SliceHttpResponse<UserListHttpResponse>` wrapped in `JsendResponse.success(...)`.

#### Scenario: Admin retrieves first page of users
- **WHEN** an authenticated admin makes `GET /api/v1/users?page=0&size=20&sort=createdAt,desc`
- **THEN** the system returns `200 OK` with JSend success envelope
- **AND** `data.content` SHALL be an array of user objects with fields: `id`, `email`, `fullName`, `role`, `createdAt`
- **AND** `data.page` SHALL be `0`, `data.size` SHALL be `20`
- **AND** `data.hasNext` SHALL be `true` if more than 20 users exist
- **AND** no `passwordHash` or sensitive field SHALL appear in any item

#### Scenario: Admin retrieves subsequent page
- **WHEN** an authenticated admin makes `GET /api/v1/users?page=1&size=20`
- **THEN** the system returns `200 OK` with `data.hasPrevious` as `true`
- **AND** `data.page` SHALL be `1`

#### Scenario: Empty user database
- **WHEN** no users exist and admin makes `GET /api/v1/users?page=0&size=20`
- **THEN** the system returns `200 OK` with `data.content` as an empty array
- **AND** `data.hasNext` SHALL be `false` and `data.hasPrevious` SHALL be `false`

#### Scenario: Non-admin user attempts to list users
- **WHEN** a request with `ROLE_USER` JWT is made to `GET /api/v1/users`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Unauthenticated request to list users
- **WHEN** a request without a valid JWT is made to `GET /api/v1/users`
- **THEN** the system returns `401 Unauthorized`

### Requirement: Pagination parameter validation for user list
The system SHALL validate pagination parameters on `GET /api/v1/users` and reject invalid requests before hitting the database.

#### Scenario: Negative page number rejected
- **WHEN** `GET /api/v1/users?page=-1` is requested
- **THEN** the system returns `400 Bad Request` with JSend fail envelope
- **AND** the error SHALL indicate `page must be >= 0`

#### Scenario: Size exceeding maximum rejected
- **WHEN** `GET /api/v1/users?size=200` is requested
- **THEN** the system returns `400 Bad Request`
- **AND** the error SHALL indicate `size must be between 1 and 100`

#### Scenario: Zero size rejected
- **WHEN** `GET /api/v1/users?size=0` is requested
- **THEN** the system returns `400 Bad Request`

#### Scenario: Valid sort field accepted
- **WHEN** `GET /api/v1/users?sort=email,asc` is requested by an admin
- **THEN** the system returns `200 OK` with results sorted by email ascending

#### Scenario: Disallowed sort field rejected
- **WHEN** `GET /api/v1/users?sort=passwordHash,asc` is requested
- **THEN** the system returns `400 Bad Request`
- **AND** the error SHALL indicate the sort field is not allowed

## MODIFIED Requirements

### Requirement: Separation of user resource from auth controller
The system SHALL expose user profile endpoints under a dedicated `UserController` at `/api/v1/users`, separate from `AuthController` at `/api/v1/auth`.

#### Scenario: User endpoints mapped to correct controller
- **WHEN** the application starts
- **THEN** `GET /api/v1/users/{id}`, `GET /api/v1/users/me`, and `GET /api/v1/users` are handled by `UserController`
- **AND** `AuthController` handles only `/api/v1/auth/**` endpoints

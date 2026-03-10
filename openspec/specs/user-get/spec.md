# Capability: User Get

## Purpose

Provides endpoints for authenticated callers to retrieve user profile information — either by UUID or via a `/me` shortcut for the currently authenticated user. User profile endpoints are served by a dedicated `UserController` separate from authentication concerns.

## Requirements

### Requirement: Retrieve user by ID

The system SHALL allow an authenticated caller to retrieve a user's profile by
their UUID via `GET /api/v1/users/{id}`.

#### Scenario: User found by ID

- **WHEN** an authenticated request is made to `GET /api/v1/users/{id}` with a
  valid UUID that corresponds to an existing user
- **THEN** the system returns `200 OK` with a JSend success envelope containing
  the user's `id`, `email`, `fullName`, `phone`, `role`, and `createdAt` fields
- **AND** the response MUST NOT include `passwordHash` or any other sensitive
  credential field

#### Scenario: User not found by ID

- **WHEN** an authenticated request is made to `GET /api/v1/users/{id}` with a
  valid UUID that does not correspond to any existing user
- **THEN** the system returns `404 Not Found` with a JSend fail envelope and
  error code `USER_NOT_FOUND`

#### Scenario: Invalid UUID format in path

- **WHEN** an authenticated request is made to `GET /api/v1/users/{id}` where
  `{id}` is not a valid UUID string
- **THEN** the system returns `400 Bad Request`

### Requirement: Retrieve own profile via /me

The system SHALL allow an authenticated user to retrieve their own profile via
`GET /api/v1/users/me` without specifying a UUID in the path.

#### Scenario: Authenticated user retrieves own profile

- **WHEN** an authenticated request is made to `GET /api/v1/users/me`
- **THEN** the system returns `200 OK` with the same user profile shape as
  `GET /api/v1/users/{id}` — reflecting the identity encoded in the JWT

#### Scenario: Unauthenticated request to /me

- **WHEN** a request is made to `GET /api/v1/users/me` without a valid JWT
- **THEN** the system returns `401 Unauthorized` (enforced by existing Spring
  Security `anyRequest().authenticated()` rule)

### Requirement: Separation of user resource from auth controller

The system SHALL expose user profile endpoints under a dedicated
`UserController` at `/api/v1/users`, separate from `AuthController` at
`/api/v1/auth`.

#### Scenario: User endpoints mapped to correct controller

- **WHEN** the application starts
- **THEN** `GET /api/v1/users/{id}` and `GET /api/v1/users/me` are handled by
  `UserController`
- **AND** `AuthController` handles only `/api/v1/auth/**` endpoints

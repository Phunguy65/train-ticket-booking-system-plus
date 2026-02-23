## ADDED Requirements

### Requirement: Admin can create a CUSTOMER account via POST /api/v1/users

The system SHALL expose a `POST /api/v1/users` endpoint that allows an
authenticated caller to create a new `CUSTOMER` user account. The system SHALL
auto-generate a cryptographically secure temporary password and return it
exactly once in the `201 Created` response. The caller SHALL NOT supply a
password in the request body.

#### Scenario: Successful user creation returns 201 with temporary password

- **WHEN** an authenticated caller sends `POST /api/v1/users` with a valid
  request body containing a unique email, full name, and optional phone
- **THEN** the system creates the user with role `CUSTOMER`, returns HTTP `201
  Created`, and the response body contains the standard user fields (`id`,
  `email`, `fullName`, `phone`, `role`, `createdAt`) plus a non-null
  `temporaryPassword` field

#### Scenario: Email already in use returns 409 Conflict

- **WHEN** an authenticated caller sends `POST /api/v1/users` with an email
  address that already exists in the system
- **THEN** the system returns HTTP `409 Conflict` with JSend fail body and
  error code `USER_EMAIL_ALREADY_EXISTS`

#### Scenario: Missing or invalid email returns 400 Bad Request

- **WHEN** an authenticated caller sends `POST /api/v1/users` with a blank,
  missing, or malformed email field
- **THEN** the system returns HTTP `400 Bad Request` with JSend fail body,
  error code `VALIDATION_ERROR`, and a field-level violation for `email`

#### Scenario: Missing full name returns 400 Bad Request

- **WHEN** an authenticated caller sends `POST /api/v1/users` with a blank or
  missing `fullName` field
- **THEN** the system returns HTTP `400 Bad Request` with JSend fail body,
  error code `VALIDATION_ERROR`, and a field-level violation for `fullName`

#### Scenario: Unauthenticated request returns 401 Unauthorized

- **WHEN** a caller sends `POST /api/v1/users` without a valid JWT in the
  `Authorization` header
- **THEN** the system returns HTTP `401 Unauthorized`

### Requirement: Created user account has CUSTOMER role regardless of request content

The system SHALL always assign `UserRole.CUSTOMER` to accounts created via
`POST /api/v1/users`. The request body SHALL NOT contain a role field. No
caller-controlled role escalation SHALL be possible through this endpoint.

#### Scenario: Created user role is always CUSTOMER

- **WHEN** a user account is successfully created via `POST /api/v1/users`
- **THEN** the `role` field in the response is `"CUSTOMER"` and the persisted
  `UserEntity.role` is `CUSTOMER`

### Requirement: System-generated temporary password is cryptographically secure

The system SHALL generate the temporary password using `UUID.randomUUID()`
(122-bit entropy) within `CreateUserUseCase`. The raw temporary password SHALL
be returned in the `201 Created` response exactly once and SHALL NOT be
persisted in plain text. Only the BCrypt hash (12 rounds) SHALL be stored in
`UserEntity.passwordHash`.

#### Scenario: Temporary password is BCrypt-hashed before persistence

- **WHEN** a user is successfully created
- **THEN** the value stored in `UserEntity.passwordHash` starts with `$2a$12$`
  and does NOT equal the plain-text `temporaryPassword` from the response

#### Scenario: Temporary password is present in the creation response only

- **WHEN** a user is successfully created via `POST /api/v1/users`
- **THEN** the `201 Created` response body contains a non-blank
  `temporaryPassword` field
- **WHEN** the same user is subsequently retrieved via `GET /api/v1/users/{id}`
- **THEN** the response body does NOT contain a `temporaryPassword` field

### Requirement: User creation publishes a UserRegistered domain event

The system SHALL publish a `UserRegistered` domain event after successfully
persisting the new user. This event SHALL carry the new user's `UserId` and
normalized email.

#### Scenario: UserRegistered event is published on successful creation

- **WHEN** a user is successfully created via `POST /api/v1/users`
- **THEN** a `UserRegistered` event is published with the correct `userId` and
  lowercase-trimmed `email`

## ADDED Requirements

### Requirement: Partial update of own profile (self)
Any authenticated user SHALL be able to send a `PATCH /api/{version}/users/me` request with a JSON body containing any subset of `fullName`, `email`, and `phone`. Only fields present in the body SHALL be updated; absent fields SHALL be left unchanged. The response SHALL be `200 OK` with the full updated user representation in JSend success format.

#### Scenario: User updates fullName only
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{ "fullName": "New Name" }`
- **THEN** the system updates only `fullName` and returns `200 OK` with the updated user (email and phone unchanged)

#### Scenario: User removes phone number
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{ "phone": null }`
- **THEN** the system sets `phone` to `null` and returns `200 OK` with the updated user

#### Scenario: User sends empty body
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{}`
- **THEN** no fields are changed and the system returns `200 OK` with the current user (idempotent)

#### Scenario: User provides blank fullName
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{ "fullName": "" }`
- **THEN** the system returns `400 Bad Request` with JSend fail containing a `REQUIRED` violation for `fullName`

#### Scenario: User provides invalid email format
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{ "email": "not-an-email" }`
- **THEN** the system returns `400 Bad Request` with JSend fail containing an `INVALID_FORMAT` violation for `email`

#### Scenario: User provides email already taken by another user
- **WHEN** an authenticated user sends `PATCH /api/1.0/users/me` with body `{ "email": "taken@example.com" }` where that email belongs to a different user
- **THEN** the system returns `409 Conflict` with JSend fail and error code `USER_EMAIL_ALREADY_EXISTS`

#### Scenario: Unauthenticated request
- **WHEN** an unauthenticated client sends `PATCH /api/1.0/users/me`
- **THEN** the system returns `401 Unauthorized`

---

### Requirement: Partial update of any user by admin
An admin user SHALL be able to send a `PATCH /api/{version}/users/{id}` request with a JSON body containing any subset of `fullName`, `email`, and `phone` to update the profile of any existing user. Only fields present in the body SHALL be updated. The response SHALL be `200 OK` with the full updated user representation in JSend success format.

#### Scenario: Admin updates email of another user
- **WHEN** an admin sends `PATCH /api/1.0/users/{id}` with body `{ "email": "new@example.com" }` and `{id}` refers to an existing user
- **THEN** the system updates the target user's `email` and returns `200 OK` with the updated user

#### Scenario: Admin updates multiple fields at once
- **WHEN** an admin sends `PATCH /api/1.0/users/{id}` with body `{ "fullName": "A", "phone": "0901234567" }`
- **THEN** the system updates both fields and returns `200 OK` with the updated user

#### Scenario: Admin targets non-existent user
- **WHEN** an admin sends `PATCH /api/1.0/users/{id}` where `{id}` does not correspond to any user
- **THEN** the system returns `404 Not Found` with JSend fail and error code `USER_NOT_FOUND`

#### Scenario: Non-admin tries to use admin PATCH endpoint
- **WHEN** a customer (non-admin) sends `PATCH /api/1.0/users/{id}`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Admin provides null fullName
- **WHEN** an admin sends `PATCH /api/1.0/users/{id}` with body `{ "fullName": null }`
- **THEN** the system returns `400 Bad Request` with JSend fail containing a `REQUIRED` violation for `fullName`

---

### Requirement: Request fields excluded from PATCH
The PATCH request body SHALL NOT accept `password`, `role`, `id`, `createdAt`, or `updatedAt`. These fields SHALL be silently ignored if sent (Jackson `@JsonIgnoreProperties`) or excluded from the DTO entirely.

#### Scenario: Client sends password in PATCH body
- **WHEN** a client sends `PATCH /api/1.0/users/me` with body `{ "password": "newpass" }`
- **THEN** the system ignores the `password` field, applies no password change, and responds as if it was not sent

---

### Requirement: Jackson configuration for JsonNullable
The application SHALL register a `JsonNullableModule` Spring bean so that Jackson can correctly deserialize `JsonNullable<T>` fields: absent JSON keys map to `JsonNullable.undefined()` and explicit `null` values map to `JsonNullable.of(null)`.

#### Scenario: Module registered at startup
- **WHEN** the Spring application context starts
- **THEN** a `JsonNullableModule` bean is present in the context and the `ObjectMapper` can deserialize `UpdateUserHttpRequest` fields correctly

# ADDED Requirements

## Requirement: Self-delete own account

An authenticated user SHALL be able to soft-delete their own account via `DELETE /api/v1/users/me`. The account is not physically removed; `deleted_at` is set to the current timestamp.

### Scenario: User successfully deletes own account

- **WHEN** an authenticated request is made to `DELETE /api/v1/users/me`
- **THEN** the system sets `deleted_at` to the current UTC timestamp on the caller's user record
- **AND** all active refresh tokens for that user are revoked within the same transaction
- **AND** a `UserDeleted` domain event is published after the transaction commits
- **AND** the system returns `200 OK` with a JSend success envelope and no data payload

### Scenario: User deletes own account a second time (idempotent)

- **WHEN** an authenticated request is made to `DELETE /api/v1/users/me` for a user whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with a JSend success envelope (idempotent — no error)

### Scenario: Unauthenticated request to self-delete

- **WHEN** a request is made to `DELETE /api/v1/users/me` without a valid JWT
- **THEN** the system returns `401 Unauthorized`

## Requirement: Admin deletes a single user

An admin SHALL be able to soft-delete any user account by UUID via `DELETE /api/v1/users/{id}`.

### Scenario: Admin successfully deletes a user

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users/{id}` with a valid UUID of an active user
- **THEN** the system sets `deleted_at` on that user record
- **AND** all active refresh tokens for that user are revoked within the same transaction
- **AND** a `UserDeleted` domain event is published after the transaction commits
- **AND** the system returns `200 OK` with a JSend success envelope

### Scenario: Target user not found

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users/{id}` with a UUID that does not correspond to any user record (active or deleted)
- **THEN** the system returns `404 Not Found` with a JSend fail envelope and error code `USER_NOT_FOUND`

### Scenario: Admin attempts to delete already-deleted user

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users/{id}` for a user whose `deleted_at` is already set
- **THEN** the system returns `200 OK` with a JSend success envelope (idempotent)

### Scenario: Non-admin attempts to delete another user

- **WHEN** a non-admin authenticated user makes a request to `DELETE /api/v1/users/{id}` where `{id}` does not match their own identity
- **THEN** the system returns `403 Forbidden`

### Scenario: Invalid UUID format

- **WHEN** a request is made to `DELETE /api/v1/users/{id}` where `{id}` is not a valid UUID string
- **THEN** the system returns `400 Bad Request`

## Requirement: Admin bulk-deletes multiple users

An admin SHALL be able to soft-delete multiple user accounts in one request via `DELETE /api/v1/users` with a JSON body containing a list of user UUIDs.

### Scenario: Admin successfully bulk-deletes users

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users` with body `{ "userIds": ["<uuid1>", "<uuid2>", ...] }` where all UUIDs correspond to active users
- **THEN** the system sets `deleted_at` on all listed user records within a single transaction
- **AND** all active refresh tokens for each deleted user are revoked within the same transaction
- **AND** a `UserDeleted` domain event is published for each deleted user after the transaction commits
- **AND** the system returns `200 OK` with a JSend success envelope containing `{ "deletedCount": N }`

### Scenario: Some IDs in bulk request are already deleted (idempotent)

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users` and some UUIDs in the list have `deleted_at` already set
- **THEN** the system soft-deletes only the active users in the list
- **AND** the system returns `200 OK` with `deletedCount` reflecting only the newly deleted count

### Scenario: Admin's own ID is in the bulk delete list

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users` and the list includes the admin's own UUID
- **THEN** the system returns `400 Bad Request` with a JSend fail envelope and error code `USER_CANNOT_BULK_DELETE_SELF`

### Scenario: Bulk delete list exceeds maximum size

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users` with a `userIds` list containing more than 100 entries
- **THEN** the system returns `400 Bad Request` with a JSend fail envelope indicating validation failure

### Scenario: Bulk delete list is empty

- **WHEN** an admin makes an authenticated request to `DELETE /api/v1/users` with an empty `userIds` list
- **THEN** the system returns `400 Bad Request` with a JSend fail envelope indicating validation failure

### Scenario: Non-admin attempts bulk delete

- **WHEN** a non-admin authenticated user makes a request to `DELETE /api/v1/users` with a `userIds` body
- **THEN** the system returns `403 Forbidden`

## Requirement: Soft-deleted users cannot authenticate

Once a user's `deleted_at` is set, all authentication attempts for that account SHALL be rejected.

### Scenario: Soft-deleted user attempts to log in

- **WHEN** a request is made to the login endpoint with valid credentials for a user whose `deleted_at` is set
- **THEN** the system returns `401 Unauthorized` with error code `USER_INVALID_CREDENTIALS`

### Scenario: Soft-deleted user's access token is used

- **WHEN** a request is made with a valid (non-expired) JWT belonging to a soft-deleted user
- **THEN** the system returns `401 Unauthorized` because token validation fails during `UserDetailsService` loading

### Scenario: Soft-deleted user attempts to refresh token

- **WHEN** a request is made to the token refresh endpoint with a refresh token belonging to a soft-deleted user
- **THEN** the system returns `401 Unauthorized` because the refresh token was revoked at deletion time

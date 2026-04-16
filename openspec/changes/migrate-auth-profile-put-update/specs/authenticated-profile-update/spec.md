# ADDED Requirements

## Requirement: Authenticated profile updates use `PUT` full-replacement semantics

The system SHALL expose authenticated customer profile updates through
`PUT /api/{version}/auth/me` instead of `PATCH /api/{version}/auth/me`. The
request payload MUST represent the full desired state of the editable profile
fields for the authenticated user.

### Scenario: Authenticated client updates profile through `PUT`

- **WHEN** an authenticated client submits a valid request to
  `PUT /api/{version}/auth/me`
- **THEN** the backend updates the authenticated user's profile and returns the
  updated `UserResponse`

### Scenario: Legacy `PATCH` profile update is no longer the supported contract

- **WHEN** the customer API contract is inspected after the change
- **THEN** the authenticated profile update operation is documented as `PUT` and
  not as `PATCH`

## Requirement: Full profile payload enforces required and nullable field rules

The authenticated profile update request SHALL require `fullName` and `email`.
The request SHALL allow `phone`, `dateOfBirth`, `gender`, `idDocumentNumber`,
and `addressLine` to be `null`. The backend MUST treat those nullable fields as
explicit replacements rather than merge directives.

### Scenario: Missing required fields fail validation

- **WHEN** a client submits the authenticated profile update request without a
  valid `fullName` or `email`
- **THEN** the backend rejects the request with the existing validation failure
  response behavior

### Scenario: Nullable optional fields are cleared explicitly

- **WHEN** a client submits `null` for `phone`, `dateOfBirth`, `gender`,
  `idDocumentNumber`, or `addressLine` in the authenticated profile update
  request
- **THEN** the backend stores those profile fields as `null` for the
  authenticated user

## Requirement: Profile update business rules remain unchanged

The authenticated profile update flow SHALL preserve the existing business rules
for authentication, user existence checks, duplicate-email detection, and
JSend-based success or failure responses while using the new request semantics.

### Scenario: Duplicate email is still rejected

- **WHEN** an authenticated client submits a profile update whose `email`
  belongs to another user account
- **THEN** the backend rejects the update with the existing email-conflict
  domain error behavior

### Scenario: Missing authenticated user is still rejected

- **WHEN** the authenticated profile update flow cannot resolve the current user
- **THEN** the backend returns the existing user-not-found failure behavior

## Requirement: Nullable transport dependency is removed from the backend profile update path

The backend SHALL remove `JsonNullable`-based request handling and the
`jackson-databind-nullable` dependency wiring that exists only to support the
legacy partial-merge profile update contract.

### Scenario: Profile update flow no longer uses `JsonNullable`

- **WHEN** the authenticated profile update controller, request DTO, command,
  and use case are inspected
- **THEN** they use plain field types instead of `JsonNullable`

### Scenario: Backend build no longer vendors the nullable library

- **WHEN** the backend build and repository metadata are inspected after the
  change
- **THEN** the Jackson nullable module wiring, Gradle dependency entry,
  composite-build substitution, Git submodule metadata, and vendored
  `jackson-databind-nullable` directory are removed

## Requirement: Shared API contract reflects the migrated profile update endpoint

The system SHALL regenerate the checked-in customer OpenAPI artifact so it
documents the `PUT` authenticated profile update operation and the new request
schema.

### Scenario: Generated contract shows `PUT` profile update

- **WHEN** `shared/api-contracts/openapi.yaml` is regenerated from the backend
- **THEN** the authenticated profile update operation is documented with the
  `put` HTTP verb

### Scenario: Generated contract shows required and nullable request fields

- **WHEN** the generated customer API contract is inspected for the
  authenticated profile update request schema
- **THEN** `fullName` and `email` are documented as required and `phone`,
  `dateOfBirth`, `gender`, `idDocumentNumber`, and `addressLine` are documented
  as nullable replacement fields

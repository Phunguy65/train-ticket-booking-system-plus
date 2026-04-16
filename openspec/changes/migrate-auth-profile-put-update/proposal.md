# Why

The authenticated profile update endpoint currently uses `PATCH` with
`JsonNullable`-based merge semantics, which adds custom request-state handling,
requires a dedicated Jackson module, and keeps a vendored nullable library in
the backend build. The team now wants a simpler full-replacement update model
that uses `PUT`, removes the nullable transport abstraction, and reduces
dependency and build-maintenance overhead.

## What Changes

- **BREAKING** Replace `PATCH /api/{version}/auth/me` with
  `PUT /api/{version}/auth/me` for authenticated profile updates.
- **BREAKING** Change the update request contract from partial merge semantics
  to full-replacement semantics: clients MUST send the complete profile-update
  payload, with `fullName` and `email` required and nullable optional fields
  explicitly set to `null` when they should be cleared.
- Remove `JsonNullable` from the authenticated profile update request DTO,
  application command, and use-case logic.
- Remove the backend Jackson nullable module wiring and the
  `jackson-databind-nullable` dependency, including composite-build and Git
  submodule references used to vendor the library.
- Regenerate the checked-in customer OpenAPI artifact so the shared contract
  reflects the new `PUT` operation and request schema.

## Capabilities

### New Capabilities

- `authenticated-profile-update`: Defines the authenticated customer profile
  update contract, including HTTP method, full-replacement payload semantics,
  validation rules, and contract artifact regeneration.

### Modified Capabilities

- None.

## Impact

- Affected backend code:
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/infrastructure/web/AuthController.java`,
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/infrastructure/web/request/UpdateAuthenticatedUserRequest.java`,
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/application/command/UpdateUserCommand.java`,
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/application/usecase/UpdateAuthenticatedUserUseCase.java`,
  and
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/shared/infrastructure/web/JacksonConfig.java`.
- Affected build and dependency wiring: `backend/build.gradle.kts`,
  `backend/settings.gradle.kts`, `gradle/libs.versions.toml`, `.gitmodules`, and
  `backend/third-party/jackson-databind-nullable/`.
- Affected shared artifact: `shared/api-contracts/openapi.yaml` must be
  regenerated from the backend contract.
- Affected API consumers: existing clients that call `PATCH /auth/me` or rely on
  omitted-field merge semantics must migrate to `PUT` and send the full update
  payload.

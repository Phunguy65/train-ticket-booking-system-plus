# Tasks

## 1. Migrate the authenticated profile update contract

- [x] 1.1 Replace the authenticated profile update controller mapping from
      `PATCH /api/{version}/auth/me` to `PUT /api/{version}/auth/me` while
      preserving the existing authentication and response behavior.
- [x] 1.2 Update `UpdateAuthenticatedUserRequest` to remove `JsonNullable`, keep
      `fullName` and `email` required, and model optional profile fields as
      plain nullable values.
- [x] 1.3 Update `UpdateUserCommand` and `UpdateAuthenticatedUserUseCase` to use
      plain field values and full-replacement semantics instead of
      `JsonNullable` merge checks.

## 2. Remove nullable-library wiring and repository metadata

- [x] 2.1 Remove the backend Jackson nullable module configuration and the
      `jackson-databind-nullable` dependency declarations from Gradle build
      files and the shared version catalog.
- [ ] 2.2 Remove the composite-build substitution in
      `backend/settings.gradle.kts` and clean up `.gitmodules` plus the vendored
      `backend/third-party/jackson-databind-nullable/` submodule.

## 3. Refresh generated artifacts and existing verification suites

- [x] 3.1 Update any existing backend tests or contract expectations that fail
      because the profile update endpoint now uses `PUT` and a non-JsonNullable
      request schema.
- [x] 3.2 Run `./backend/gradlew -p ./backend spotlessCheck` and
      `./backend/gradlew -p ./backend test` to verify formatting and backend
      test suites.
- [x] 3.3 Run `./backend/gradlew -p ./backend exportCustomerOpenApi` and ensure
      `shared/api-contracts/openapi.yaml` reflects the migrated authenticated
      profile update contract.

# Context

The backend currently exposes authenticated profile updates through
`PATCH /api/{version}/auth/me` in
`backend/src/main/java/io/github/phunguy65/ttbs/backend/user/infrastructure/web/AuthController.java`.
That endpoint accepts `UpdateAuthenticatedUserRequest`, which models each field
as `JsonNullable<T>` so the use case can distinguish between omitted fields,
explicit `null`, and concrete values. To support that request shape, the backend
registers `JsonNullableModule` in `JacksonConfig`, declares
`jackson-databind-nullable` in `backend/build.gradle.kts`, substitutes the
dependency through a composite build in `backend/settings.gradle.kts`, and keeps
the vendored library as a Git submodule under
`backend/third-party/jackson-databind-nullable/`.

The planned change intentionally removes partial-merge semantics for this
endpoint. The new contract uses `PUT` with full-replacement semantics: clients
must send the complete update payload, required fields stay required, and
nullable optional fields are cleared by sending `null`. The change is localized
to the user profile update flow, but it crosses controller, request DTO,
application command/use-case logic, build wiring, and generated API contract
artifacts, so recording the migration decisions up front reduces ambiguity.

## Goals / Non-Goals

**Goals:**

- Replace the authenticated profile update endpoint with `PUT` and model it as a
  full-replacement operation.
- Remove `JsonNullable` from the request DTO, command, and use-case flow so the
  update path uses plain Java field types.
- Preserve existing validation and domain behavior for required fields,
  duplicate-email detection, authentication, and error responses.
- Remove the Jackson nullable module, dependency declaration, composite-build
  substitution, and vendored library metadata tied to
  `jackson-databind-nullable`.
- Regenerate `shared/api-contracts/openapi.yaml` from the backend so the
  checked-in customer contract reflects the `PUT` verb and full payload schema.

**Non-Goals:**

- Adding frontend implementation changes or client migration code.
- Changing the runtime response envelope, authorization model, or user domain
  model shape.
- Introducing new endpoint-level tests beyond fixing existing verification
  suites that fail because of the contract change.
- Updating product or use-case documentation in `docs/`; temporary drift is
  accepted for this change.

## Decisions

### 1. Use `PUT` with full-replacement request semantics

The authenticated profile update operation will move from `PATCH` to `PUT`, and
the request contract will represent the full desired state of the editable
profile fields.

Why this choice:

- It removes the need for transport-level tri-state field handling.
- It aligns the endpoint semantics with the team decision that clients now send
  the complete editable profile payload.
- It makes request validation and backend update logic easier to reason about
  because each field is interpreted directly.

Alternatives considered:

- Keep `PATCH` with `JsonNullable`: rejected because the nullable merge behavior
  is no longer needed.
- Use `PUT` but keep partial-merge behavior: rejected because it preserves the
  ambiguity that the migration is intended to remove.

### 2. Map request and command fields to plain nullable Java values

`UpdateAuthenticatedUserRequest` and `UpdateUserCommand` will use plain field
types instead of `JsonNullable<T>`. `fullName` and `email` remain required and
validated. `phone`, `dateOfBirth`, `gender`, `idDocumentNumber`, and
`addressLine` remain nullable and will be written as `null` when the client
supplies `null`.

Why this choice:

- The `User` aggregate already stores those optional fields as nullable values.
- The use case can reconstitute the updated aggregate directly from the command
  without merge-specific branching.
- This preserves the required-vs-nullable split already present in the domain
  model.

Alternatives considered:

- Make all fields required in the `PUT` request: rejected because the current
  domain intentionally allows several optional profile fields to be absent.
- Interpret `null` as "keep existing value": rejected because it violates the
  chosen full-replacement semantics.

### 3. Keep existing use-case and error-handling patterns

`UpdateAuthenticatedUserUseCase` will keep the current repository lookups,
duplicate-email protection, `Result<T, E>` return style, and `User.reconstitute`
flow. Only the merge logic changes: the use case will read plain command values
instead of branching on `JsonNullable.isPresent()`.

Why this choice:

- Existing business rules around user existence and email uniqueness are still
  valid.
- The project already uses `Result`-based application flows and direct
  repository access in use cases.
- Limiting the scope to transport and update semantics reduces migration risk.

Alternatives considered:

- Rewrite the update path through a new domain mutation API: rejected because it
  is unnecessary for this targeted contract migration.

### 4. Remove nullable-library wiring completely

The change will remove every repository-owned reference that exists only to
support `jackson-databind-nullable`: runtime bean registration, Gradle
dependency declaration, version-catalog entry, composite-build substitution, Git
submodule metadata, and the vendored source directory.

Why this choice:

- Partial removal would leave dead build/configuration paths in the repo.
- The plan explicitly calls for removing the dependency because the backend no
  longer needs its request-model semantics.
- Cleanup avoids future confusion about why the vendored library still exists.

Alternatives considered:

- Stop using the library in code but keep the build/submodule wiring: rejected
  because it preserves unused maintenance burden.

### 5. Treat OpenAPI as a generated artifact that must be refreshed

`shared/api-contracts/openapi.yaml` will be refreshed by running the backend
generation workflow after the controller and request DTO are updated.

Why this choice:

- The repo already treats the checked-in contract as generated from backend
  annotations.
- Leaving the artifact stale would immediately reintroduce contract drift for
  the changed HTTP method and request schema.

Alternatives considered:

- Hand-edit the YAML: rejected because the existing contract workflow is
  generator-based.

## Risks / Trade-offs

- [Existing API clients still send `PATCH` or partial payloads] -> Mitigation:
  mark the endpoint change as breaking in the proposal and update the generated
  contract so downstream consumers see the new `PUT` requirement.
- [Optional field clearing causes unintended data loss when clients omit fields]
  -> Mitigation: define the contract clearly as full replacement and require the
  full payload from clients.
- [Removing the vendored library leaves broken Gradle or Git metadata] ->
  Mitigation: update `backend/settings.gradle.kts`, `.gitmodules`, and remove
  the vendored directory as part of the same change.
- [Existing reflection or contract tests fail after DTO and method changes] ->
  Mitigation: run the existing backend test and contract-generation workflow,
  then update failing expectations without broadening test scope.
- [Checked-in OpenAPI artifact drifts from code] -> Mitigation: run
  `./backend/gradlew -p ./backend exportCustomerOpenApi` as part of the
  implementation verification sequence.

## Migration Plan

1. Update the request DTO, command, use case, and controller to use `PUT` and
   plain field types.
2. Remove Jackson nullable wiring and dependency/build metadata.
3. Remove the vendored nullable-library submodule and related Git metadata.
4. Run backend formatting, tests, and OpenAPI export to refresh generated
   artifacts and catch contract regressions.

Rollback strategy:

- Revert the change set to restore `PATCH`, `JsonNullable`, and the vendored
  dependency wiring if a consumer cannot migrate yet.
- Because there is no data migration, rollback is source-control based and only
  needs the previous code/configuration state.

## Open Questions

- None. The endpoint semantics, field requiredness, null behavior, test scope,
  submodule cleanup, and documentation scope were all decided during
  exploration.

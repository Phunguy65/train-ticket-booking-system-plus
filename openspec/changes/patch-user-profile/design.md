## Context

The user module has `POST /users` (admin create), `GET /users/{id}`, `GET /users/me`, and `GET /users` (list). There is no way for a user to update their own profile, nor for an admin to correct user details after creation.

The `User` domain model is fully immutable (all fields `final`). Updates must go through `User.reconstitute()` which produces a new instance — the existing `passwordHash` and `createdAt` are preserved unchanged.

Spring Security is URL-based (`SecurityFilterChain`), currently with no `@EnableMethodSecurity`. The JWT token stores `sub = userId UUID`, `email`, and `role`; Spring security maps the role to the authority `ROLE_<ROLE_NAME>`.

The `GlobalExceptionHandler` in the `shared` module already handles `MethodArgumentNotValidException` → JSend `fail` with `ViolationCode`-annotated errors. No new exception handling infrastructure is needed.

## Goals / Non-Goals

**Goals:**

- Allow any authenticated user to partially update their own `fullName`, `email`, `phone` via `PATCH /users/me`.
- Allow admins to partially update any user's `fullName`, `email`, `phone` via `PATCH /users/{id}`.
- Use `JsonNullable<T>` to cleanly distinguish "field absent" from "field set to null" (relevant for `phone` which is nullable).
- Reuse `UserHttpResponse` as the 200 OK body so callers see the updated state immediately.
- Keep `password`, `role`, `createdAt`, and `updatedAt` out of the PATCH request body entirely.

**Non-Goals:**

- Password change — separate concern, requires current-password verification.
- Role change — admin-only privilege management, intentionally deferred.
- Bulk update of multiple users.
- Optimistic locking / ETag support.
- Audit log / domain events for profile changes (can be added later).

## Decisions

### D1 — Single `UpdateUserUseCase` shared by both endpoints

Both `PATCH /{id}` and `PATCH /me` call the same `UpdateUserUseCase` with an `UpdateUserCommand(UserId, JsonNullable<fullName>, JsonNullable<email>, JsonNullable<phone>)`. The controller layer resolves which `UserId` to pass (path variable vs. principal). This mirrors the existing precedent where `GetUserByIdUseCase` is reused by both `getById()` and `getMe()`.

### D2 — `@PreAuthorize` for admin endpoint, not URL rules

Adding `PATCH /api/*/users/*` as `hasRole("ADMIN")` in `SecurityFilterChain` would conflict with the existing `/me` path (since "me" matches `{id}`). Using `@PreAuthorize("hasRole('ADMIN')")` directly on the controller method avoids ordering fragility. Requires adding `@EnableMethodSecurity` to `SecurityConfig`.

### D3 — `JsonNullable<T>` with built-in `ValueExtractor`

`jackson-databind-nullable:0.2.9` ships a `jakarta.validation` `ValueExtractor` for `JsonNullable`. Once the dependency is on the classpath and `JsonNullableModule` is registered as a Spring `@Bean`, standard `@NotBlank` and `@Email` annotations on `JsonNullable<String>` fields work correctly: `undefined` skips the constraint; `of(null)` or `of("")` fails `@NotBlank`.

### D4 — `phone` is nullable by design

`phone` carries no `@NotBlank` constraint. Sending `"phone": null` (→ `JsonNullable.of(null)`) explicitly removes the phone; not sending `phone` at all (→ `JsonNullable.undefined()`) preserves the current value.

### D5 — `updatedAt` is set automatically in the use case

The use case reconstructs the `User` via `reconstitute(...)` passing `Instant.now()` as `updatedAt`. It is never read from the request body and never returned in `UserHttpResponse`.

### D6 — Email uniqueness check on update

If `email` is present in the command, the use case checks whether the new email is already owned by a different user (`userRepository.findByEmail()`). If so, it returns `Result.failure(new UserError.EmailAlreadyExists())` → 409 Conflict (handled by the existing `errorResponse()` switch).

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `ValueExtractor` not discovered automatically | Low | `jackson-databind-nullable 0.2.4+` registers it via `META-INF/services`; confirmed for 0.2.9 |
| `/me` path matches `/{id}` route | Medium | `@PreAuthorize` on `/{id}` (not URL rule) eliminates ordering issue; Spring MVC resolves literal `/me` before `/{id}` |
| Race condition: two requests change same email simultaneously | Low | DB unique constraint on `email` column is the final safety net; use case check reduces noise |
| `updatedAt` updated even when nothing changed | Accepted | Keeps logic simple; PATCH is rarely called idempotently in this domain |

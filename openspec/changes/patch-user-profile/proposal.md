# Why

The user module currently supports creating and reading users but has no way to update profile information. Users cannot change their own display name, email or phone number, and admins have no API to correct user details either. Adding a `PATCH` endpoint fills this gap using `JsonNullable<T>` to properly distinguish "field not sent" from "field explicitly cleared".

## What Changes

- Add `PATCH /api/{version}/users/{id}` — admin-only endpoint to partially update any user's `fullName`, `email`, or `phone`.
- Add `PATCH /api/{version}/users/me` — authenticated-user endpoint to partially update their own `fullName`, `email`, or `phone`.
- Both endpoints exclude `password`, `role`, `createdAt`, and `updatedAt` from the request body (these are managed elsewhere or are immutable from the client's perspective).
- Add `org.openapitools:jackson-databind-nullable:0.2.9` dependency to support `JsonNullable<T>` wrapper.
- Register `JsonNullableModule` bean so Jackson correctly serializes/deserializes `JsonNullable` fields.
- Enable `@EnableMethodSecurity` on `SecurityConfig` to allow `@PreAuthorize` on the admin endpoint.

## Capabilities

### New Capabilities

- `user-partial-update`: Partial update of user profile fields (`fullName`, `email`, `phone`) via PATCH. Covers the `UpdateUserUseCase`, `UpdateUserCommand`, `UpdateUserHttpRequest`, and the two controller endpoints (`/{id}` for admin, `/me` for self).

### Modified Capabilities

- `user-get`: The existing user read capability is extended in the sense that the same `UserHttpResponse` shape is returned after a successful PATCH — no requirement change, only reuse of the existing response type.

## Impact

- **API**: Two new endpoints: `PATCH /{version}/users/{id}` (admin) and `PATCH /{version}/users/me` (any authenticated user).
- **Security**: `SecurityConfig` gains `@EnableMethodSecurity`; the admin endpoint is guarded with `@PreAuthorize("hasRole('ADMIN')")`.
- **Dependencies**: `build.gradle.kts` gains `org.openapitools:jackson-databind-nullable:0.2.9`.
- **Jackson configuration**: A `JsonNullableModule` bean must be registered (in `WebConfig` or a dedicated `JacksonConfig`).
- **User domain model**: `User.reconstitute()` is used to build an updated immutable instance inside the use case — no structural change to the domain model.
- **Existing `UserHttpResponse`**: Reused as-is for the PATCH response body (200 OK).
- **`GlobalExceptionHandler`**: Already handles `MethodArgumentNotValidException` — no changes needed.

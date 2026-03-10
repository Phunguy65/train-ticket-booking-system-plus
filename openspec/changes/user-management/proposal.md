# Why

The backend currently has no dedicated user management API. User data is only
accessible as a side-effect of authentication flows (register/login). A
`UserController` is needed to expose user operations as a proper REST resource,
starting with retrieving user profiles — separate from the existing
`AuthController` which handles authentication concerns only.

## What Changes

- Add `UserController` under `POST /api/v1/users` namespace (separate from
  `/api/v1/auth`)
- Implement `GET /api/v1/users/{id}` — retrieve a user by ID
- Implement `GET /api/v1/users/me` — retrieve the currently authenticated user's
  profile
- Add `GetUserUseCase` in the application layer (query-only, no side effects)
- Add `UserHttpResponse` response DTO and `UserRequestMapper` for the new
  controller (reuse or extend existing `UserDto`)
- Security guards are **out of scope** for this change — endpoints will be
  protected only by the existing `anyRequest().authenticated()` rule

## Capabilities

### New Capabilities

- `user-get`: Retrieve user profiles via a dedicated `UserController` — both by
  ID and as the current authenticated user (`/me`)

### Modified Capabilities

<!-- No existing spec-level requirements are changing -->

## Impact

- **New files**: `UserController`, `GetUserUseCase`, `GetUserByIdQuery`,
  `UserRequestMapper` (web layer mapper)
- **Existing files**: `UserRepository` port already exposes `findById` — no
  domain changes needed
- **Security config**: No changes — new endpoints fall under existing
  `anyRequest().authenticated()` rule
- **No breaking changes** to existing `/api/v1/auth/**` endpoints

# Why

The backend has no dedicated admin endpoint to create user accounts. User
creation only exists via the public self-registration flow (`POST
/api/v1/auth/register`), which is unsuitable for admin-provisioned accounts.
A `POST /api/v1/users` endpoint is needed so that administrators can create
`CUSTOMER` accounts with system-generated temporary passwords without exposing
password management to the caller.

## What Changes

- Add `POST /api/v1/users` endpoint in `UserController` (admin-provisioned
  user creation, separate concern from public auth registration)
- Add `CreateUserUseCase` in the application layer — auto-generates a
  cryptographically secure temporary password, encodes it with BCrypt, and
  returns the raw temporary password **once** in the `201 Created` response
- Add `CreateUserCommand` (application layer input DTO): `email`, `fullName`,
  `phone`; **no password field** — password is generated internally
- Add `CreateUserHttpRequest` (web layer DTO) with Bean Validation constraints
- Add `CreateUserHttpResponse` (web layer DTO) extending the standard user
  fields with a `temporaryPassword` field — only present on creation
- Role is fixed to `UserRole.CUSTOMER` — no caller-controlled role assignment
- The endpoint requires JWT authentication (`anyRequest().authenticated()` rule
  already in place); role-based access guard (`ADMIN` only) is **out of scope**
  for this change

## Capabilities

### New Capabilities

- `user-create`: Admin-provisioned creation of `CUSTOMER` accounts via
  `POST /api/v1/users` with a system-generated temporary password returned
  once in the response

### Modified Capabilities

<!-- No existing spec-level requirements are changing -->

## Impact

- **New files**: `CreateUserUseCase`, `CreateUserCommand`,
  `CreateUserHttpRequest`, `CreateUserHttpResponse`
- **Modified files**: `UserController` (new POST endpoint),
  `UserRequestMapper` (new `toCommand` + `toCreateResponse` methods)
- **Domain model**: `User.create()` signature unchanged — role stays hardcoded
  to `CUSTOMER`; no domain changes needed
- **Security config**: No changes — new endpoint falls under existing
  `anyRequest().authenticated()` rule
- **No breaking changes** to existing `/api/v1/auth/**` or
  `GET /api/v1/users/**` endpoints

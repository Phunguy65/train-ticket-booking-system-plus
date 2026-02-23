## Context

`UserController` currently only exposes read operations (`GET /users/{id}`,
`GET /users/me`). User creation exists exclusively via the public
self-registration flow in `AuthController` (`POST /api/v1/auth/register`),
which is caller-initiated and unsuitable for admin-provisioned accounts.

The existing `RegisterUserUseCase` already encodes passwords with BCrypt via
the `PasswordEncoder` port. The new `CreateUserUseCase` follows the same
pattern but generates the temporary password internally, so no password field
appears in the HTTP request.

`User.create()` hardcodes `UserRole.CUSTOMER` — this is intentional and
unchanged. Admin-created accounts are always customers.

## Goals / Non-Goals

**Goals:**

- Expose `POST /api/v1/users` in `UserController` for admin-provisioned
  `CUSTOMER` account creation
- Auto-generate a cryptographically secure temporary password inside
  `CreateUserUseCase`; return it **once** in the `201 Created` response body
- Reuse the existing `PasswordEncoder` port and `UserRepository.save()` — no
  new infrastructure required
- Follow the existing Result monad + fold pattern used by all other use cases
- Validate the HTTP request with Bean Validation (`@Valid`)

**Non-Goals:**

- Role assignment by the caller — role is always `CUSTOMER`
- Email delivery of the temporary password (future enhancement via
  `UserRegistered` event listener)
- Role-based access guard (`ADMIN`-only) on the endpoint — deferred
- Force-password-reset flag on first login — deferred
- Password complexity rules beyond `@Size(min=8, max=72)`

## Decisions

### D1 — Password generated in use case, not in controller or domain

The `CreateUserUseCase` owns password generation because it already holds the
`PasswordEncoder` port and controls the complete creation transaction.
Generating in the controller would require passing a raw password back up
through the application layer; generating in the domain would couple `User`
to an infrastructure concern (`SecureRandom`).

Generation strategy: `UUID.randomUUID().toString().replace("-", "")` — 32
lowercase hex characters, 122-bit entropy, always BCrypt-safe (≤ 72 chars),
no external dependency. This is intentionally simple and auditable.

### D2 — `CreateUserHttpResponse` is a new DTO, not an extension of `UserHttpResponse`

`UserHttpResponse` is used by GET endpoints and must never expose a
`temporaryPassword` field. A dedicated `CreateUserHttpResponse` (with all
standard user fields plus `temporaryPassword`) keeps the GET response contract
clean and prevents accidental exposure.

### D3 — `CreateUserResult` carries both `UserDto` and `temporaryPassword`

`CreateUserUseCase.execute()` returns `Result<CreateUserResult, UserError>`
where `CreateUserResult` is a record containing `UserDto user` and
`String temporaryPassword`. This avoids polluting `UserDto` (which is shared
with read use cases) with a field that only exists at creation time.

### D4 — `UserRequestMapper` extended (not a new mapper)

`UserRequestMapper` already maps `UserDto → UserHttpResponse` for the GET
endpoints in `UserController`. Adding `toCommand(CreateUserHttpRequest)` and
`toCreateResponse(CreateUserResult)` here is consistent — one mapper per
controller is the established pattern (see `AuthRequestMapper`).

### D5 — Temporary password NOT stored separately

The raw temporary password is returned to the caller once and then discarded.
Only the BCrypt hash is persisted in `UserEntity.passwordHash`. There is no
"is_temporary" flag or separate table — this is intentional simplicity.
Force-reset enforcement is deferred.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Temporary password exposed in logs | Medium | Ensure no logging of response bodies in production; `temporaryPassword` is only in `201` response |
| UUID hex lacks symbol characters (some systems require them) | Low | Acceptable for temp passwords; user is expected to change it promptly |
| Race condition on duplicate email | Low | `UserEntity.email` has `@Column(unique=true)` DB constraint; `EmailAlreadyExists` check in use case handles application-level race |
| Caller ignores temp password and user is locked out | Medium | Documented in API contract; future enhancement: force-reset flag |

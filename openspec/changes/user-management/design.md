## Context

The backend `user` module currently exposes user data only through auth endpoints
(`POST /api/v1/auth/register`, `POST /api/v1/auth/login`). There is no dedicated
REST resource for user profiles. The domain layer already has `UserRepository`
with `findById(UserId)` and `findByEmail(String)`, and `UserDto` exists as the
application-layer output model — but there is no use case nor controller wired up
for querying users.

`AuthController` handles authentication concerns. User resource management
belongs in a separate `UserController` at `/api/v1/users` to keep concerns clean
and allow independent evolution (future: list users, update profile, admin
create).

## Goals / Non-Goals

**Goals:**

- Add `GET /api/v1/users/{id}` — retrieve a user by UUID
- Add `GET /api/v1/users/me` — retrieve the authenticated user's own profile
- Introduce `UserController` as the dedicated REST controller for user resources
- Add `GetUserByIdUseCase` (query use case, no side effects, no `@Transactional`)
- Keep the domain layer untouched — no new domain logic required

**Non-Goals:**

- Role-based access control (ADMIN guard) — deferred to a future change
- Admin create user (`POST /api/v1/users`) — deferred
- Pagination / list users — deferred
- Profile update (`PATCH /api/v1/users/me`) — deferred
- Password change — deferred

## Decisions

### D1 — One use case, two endpoints

`GetUserByIdUseCase` accepts a `UserId` and returns `Result<UserDto, UserError>`.
Both `/users/{id}` and `/users/me` call the same use case:

- `GET /users/{id}` passes the path variable UUID
- `GET /users/me` extracts the authenticated user's UUID from `SecurityContext`
  and passes it to the same use case

This avoids duplicating query logic and keeps the application layer thin.

### D2 — `UserRequestMapper` (new) vs reusing `AuthRequestMapper`

A new `UserRequestMapper` is added in `infrastructure/web/` for the
`UserController`. `AuthRequestMapper` is package-private and owned by
`AuthController` — sharing it would couple two unrelated controllers. The mapper
is trivial: it only converts `UserDto` → `UserHttpResponse`.

`UserHttpResponse` already exists in the codebase (used by `AuthController`). It
will be reused as-is — no new response DTO needed.

### D3 — No `@Transactional` on query use case

`GetUserByIdUseCase` is a pure read. It is annotated `@Service` only, with no
`@Transactional`. Spring Data JPA's default read transaction is sufficient for
simple `findById` calls.

### D4 — `/me` resolution in controller, not use case

`SecurityContextHolder` is an infrastructure concern (Spring Security). The
controller extracts `UserId` from the authenticated principal and delegates to
the same `GetUserByIdUseCase`. The use case stays framework-free.

## Key Flows

### GET /api/v1/users/{id}

```
Client
  │  GET /api/v1/users/{id}
  │  Authorization: Bearer <jwt>
  ▼
JwtAuthenticationFilter          ← validates token, sets SecurityContext
  ▼
UserController
  │  UUID pathVariable → UserId.of(uuid)
  ▼
GetUserByIdUseCase
  │  userRepository.findById(userId)
  ├── found    → Result.success(UserDto)  → 200 OK  + UserHttpResponse
  └── not found → Result.failure(UserNotFound) → 404
```

### GET /api/v1/users/me

```
Client
  │  GET /api/v1/users/me
  │  Authorization: Bearer <jwt>
  ▼
JwtAuthenticationFilter          ← validates token, sets SecurityContext
  ▼
UserController
  │  SecurityContextHolder → Authentication → principal UUID → UserId
  ▼
GetUserByIdUseCase               ← same use case as /users/{id}
  ├── found    → 200 OK + UserHttpResponse
  └── not found → 404 (edge case: user deleted after token issued)
```

## Components

| Component | Layer | Responsibility | Location |
|---|---|---|---|
| `GetUserByIdUseCase` | Application | Query user by ID via repository port | `user/application/usecase/` |
| `UserController` | Infrastructure/Web | REST endpoints `/users/{id}` and `/users/me` | `user/infrastructure/web/` |
| `UserRequestMapper` | Infrastructure/Web | Maps `UserDto` → `UserHttpResponse` | `user/infrastructure/web/` |
| `UserHttpResponse` | Infrastructure/Web | Response DTO (already exists, reuse) | `user/infrastructure/web/` |
| `UserRepository` | Domain | Port — `findById(UserId)` already exists | `user/domain/repository/` |
| `UserDto` | Application | Output DTO (already exists, reuse) | `user/application/dto/` |

## Error Handling

| Error Case | Type | HTTP Status | Error Code |
|---|---|---|---|
| User not found by ID | `UserError.UserNotFound` | `404 Not Found` | `USER_NOT_FOUND` |
| Invalid UUID format in path | `MethodArgumentTypeMismatchException` | `400 Bad Request` | Handled by `GlobalExceptionHandler` |
| No authentication token | Spring Security 401 | `401 Unauthorized` | — |
| Invalid/expired JWT | Spring Security 401 | `401 Unauthorized` | — |

`UserError.UserNotFound` is already defined in `UserError` sealed interface.
`ErrorCode.USER_NOT_FOUND` already exists in `ErrorCode` enum.
No new error types needed.

## Test Strategy

| Test | Type | What to verify |
|---|---|---|
| `GetUserByIdUseCaseTest` | Unit (`@ExtendWith(MockitoExtension)`) | Returns `UserDto` on found; returns `UserNotFound` on missing |
| `UserControllerTest` | `@WebMvcTest(UserController.class)` | `GET /users/{id}` → 200; UUID not found → 404; `GET /users/me` → 200 |

## Risks / Trade-offs

- **`/users/me` requires authenticated principal in controller** — If
  `SecurityContext` is empty (edge case in tests), the controller must handle
  gracefully with a 401. `@WebMvcTest` will use `@WithMockUser` to simulate this.
- **`UserHttpResponse` is shared** — If `AuthController` and `UserController`
  need divergent response shapes in the future, they will need separate DTOs.
  For now, sharing is intentional and acceptable.

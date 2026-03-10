## Context

The `user` module's `application/` layer currently contains a `service/` sub-package with two classes:

- `RefreshTokenService` — orchestrates JWT access token generation, raw refresh token generation, SHA-256 hashing, and persistence of the token hash. Used by `LoginUserUseCase`, `RefreshTokenUseCase`, and `LogoutUserUseCase`.
- `UserDtoMapper` — single `toDto(User) → UserDto` method. Used by all four read/write use cases.

The `booking` module (the canonical reference slice) has no `application/service/` package: mapping is inlined as a `private` method in each use case, and all orchestration lives directly in the use case class. This divergence causes confusion when navigating the codebase and contradicts the `backend-vertical-slice-structure` spec which permits only `domain/`, `application/`, and `infrastructure/` sub-packages per slice.

## Goals / Non-Goals

**Goals:**

- Remove `application/service/` from the `user` module entirely.
- Introduce a `RefreshTokenManager` port in `application/port/` following the existing `TokenProvider` / `PasswordEncoder` pattern.
- Provide a `JwtRefreshTokenManager` infrastructure adapter in `infrastructure/security/`.
- Inline `toDto()` as a `private` method inside each use case that needs it.
- Keep all existing behaviour and tests green — no HTTP API changes.

**Non-Goals:**

- Changing authentication logic or JWT configuration.
- Modifying any other module.
- Introducing new features or endpoints.

## Decisions

### Decision 1 — Port + Adapter for refresh-token orchestration (not inline)

`RefreshTokenService` combines token generation (via `TokenProvider`), hashing (SHA-256), and persistence (via `RefreshTokenRepository`). Inlining this logic would duplicate ~50 lines across `LoginUserUseCase` and `RefreshTokenUseCase`, and replicate the `hashToken` helper across three use cases.

The codebase already uses the port-adapter pattern for exactly this type of concern:

```
application/port/PasswordEncoder    ←→  infrastructure/security/BCryptPasswordEncoderAdapter
application/port/TokenProvider      ←→  infrastructure/security/JwtTokenProvider
```

Adding `application/port/RefreshTokenManager` with a single `JwtRefreshTokenManager` adapter is the natural, consistent extension. It keeps each use case's constructor lean and makes the boundary testable via a single mock.

### Decision 2 — `TokenPair` record moved into the port interface

`RefreshTokenService.TokenPair` is currently a nested record. After deleting the service, use cases and tests import the record from `RefreshTokenManager.TokenPair` instead — no semantic change.

### Decision 3 — `UserDtoMapper` inlined, not ported

Mapping `User → UserDto` is a trivial six-field projection. Duplicating it as `private UserDto toDto(User user)` in four use cases costs ~6 lines each and perfectly matches the `booking` module reference. No port needed.

### Decision 4 — `@Value` config stays in infrastructure

`refreshTokenExpirySeconds` is injected via `@Value` in `RefreshTokenService`. This moves into `JwtRefreshTokenManager` where it belongs — infrastructure configuration lives in the infrastructure layer.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|---|---|---|
| Token hash mismatch after move | Low | SHA-256 algorithm unchanged; unit test covers `hashToken` on adapter |
| Transaction boundary broken | Low | `@Transactional` remains on each use case; adapter has no `@Transactional` of its own |
| Test mock update misses | Medium | Grep for `RefreshTokenService` and `UserDtoMapper` import before finalising |
| Spring Modulith boundary violation | Low | `application/service/` removal shrinks public surface; `@ApplicationModuleTest` re-run confirms |

**Trade-off — slight duplication of `toDto()`**: The six-field mapping body is repeated in four use cases. This is an intentional vertical-slice trade-off: self-contained use cases are preferred over a shared mapper component.

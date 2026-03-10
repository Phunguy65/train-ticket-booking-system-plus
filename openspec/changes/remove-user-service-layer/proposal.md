# Why

The `user` module has an `application/service/` sub-package containing `RefreshTokenService` and `UserDtoMapper` that violates the vertical slice clean architecture spec. Every other module (e.g., `booking`) organises orchestration exclusively inside `application/usecase/` with no intermediate service layer. Removing this package aligns the `user` module with the established pattern and the `backend-vertical-slice-structure` spec.

## What Changes

- **Delete** `application/service/RefreshTokenService.java` — token generation and hashing logic moves to a new infrastructure adapter behind an application port.
- **Delete** `application/service/UserDtoMapper.java` — mapping logic inlined as a `private` method in each use case that needs it (mirrors the `booking` module pattern).
- **Add** `application/port/RefreshTokenManager.java` — new port interface (`generateAndSaveTokens`, `hashToken`, `TokenPair` record) consistent with existing `TokenProvider` and `PasswordEncoder` ports.
- **Add** `infrastructure/security/JwtRefreshTokenManager.java` — adapter implementing the new port; owns SHA-256 hashing, JWT generation, and refresh-token persistence.
- **Update** `LoginUserUseCase`, `RefreshTokenUseCase`, `LogoutUserUseCase` — inject `RefreshTokenManager` port instead of the deleted service.
- **Update** `RegisterUserUseCase`, `GetUserByIdUseCase`, `LoginUserUseCase`, `RefreshTokenUseCase` — inline `private UserDto toDto(User user)` method; remove `UserDtoMapper` dependency.
- **Update** test files (8) — replace `RefreshTokenService` / `UserDtoMapper` mocks with the new port / inline equivalents.

## Capabilities

### New Capabilities

_(none — this is a structural refactoring with no new externally observable behaviour)_

### Modified Capabilities

_(no spec-level requirement changes — internal implementation restructuring only)_

## Impact

- **Files deleted**: 2 (`application/service/RefreshTokenService.java`, `application/service/UserDtoMapper.java`)
- **Files created**: 2 (`application/port/RefreshTokenManager.java`, `infrastructure/security/JwtRefreshTokenManager.java`)
- **Files updated**: 5 use cases + 8 test classes ≈ 13 files
- **APIs**: no change — all HTTP request/response contracts remain identical
- **Behaviour**: no change — token generation, hashing, and user mapping logic is preserved; only the package location changes
- **Spring Modulith boundary**: the `application/service/` package is removed; module public surface shrinks (positive)

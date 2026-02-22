## 1. Create RefreshTokenManager port interface

- [x] 1.1 Create `application/port/RefreshTokenManager.java` — interface with `generateAndSaveTokens(User): TokenPair`, `hashToken(String): String`, and nested `record TokenPair(String accessToken, String refreshToken)`

## 2. Create JwtRefreshTokenManager adapter

- [x] 2.1 Create `infrastructure/security/JwtRefreshTokenManager.java` — `@Component` implementing `RefreshTokenManager`, migrating SHA-256 hash logic and `generateAndSaveTokens` body from `RefreshTokenService`, injecting `RefreshTokenRepository`, `TokenProvider`, and `@Value("${jwt.refresh-token-expiry:604800}") long refreshTokenExpirySeconds`

## 3. Update use cases — inject RefreshTokenManager

- [x] 3.1 Update `LoginUserUseCase` — replace `RefreshTokenService` field with `RefreshTokenManager`; update constructor; update `execute()` to call `tokenManager.generateAndSaveTokens()`; add `private UserDto toDto(User user)` inline method; remove `UserDtoMapper` field and constructor param
- [x] 3.2 Update `RefreshTokenUseCase` — replace `RefreshTokenService` + `UserDtoMapper` with `RefreshTokenManager`; update constructor; update `execute()` to use `tokenManager.hashToken()` and `tokenManager.generateAndSaveTokens()`; add `private UserDto toDto(User user)` inline method
- [x] 3.3 Update `LogoutUserUseCase` — replace `RefreshTokenService` with `RefreshTokenManager`; update constructor; update `execute()` to call `tokenManager.hashToken()`
- [x] 3.4 Update `RegisterUserUseCase` — remove `UserDtoMapper` field and constructor param; add `private UserDto toDto(User user)` inline method; update `execute()` call site
- [x] 3.5 Update `GetUserByIdUseCase` — remove `UserDtoMapper` field and constructor param; add `private UserDto toDto(User user)` inline method; update `execute()` call site

## 4. Delete service layer

- [x] 4.1 Delete `application/service/RefreshTokenService.java`
- [x] 4.2 Delete `application/service/UserDtoMapper.java`
- [x] 4.3 Delete the now-empty `application/service/` directory

## 5. Update use case tests

- [x] 5.1 Update `LoginUserUseCaseTest` — replace `@Mock RefreshTokenService` with `@Mock RefreshTokenManager`; replace `UserDtoMapper` mock/stub with direct `UserDto` assertion; update import statements
- [x] 5.2 Update `RefreshTokenUseCaseTest` — replace `@Mock RefreshTokenService` + `@Mock UserDtoMapper` with `@Mock RefreshTokenManager`; update stubbing calls (`hashToken`, `generateAndSaveTokens`); update import statements
- [x] 5.3 Update `LogoutUserUseCaseTest` — replace `@Mock RefreshTokenService` with `@Mock RefreshTokenManager`; update `hashToken` stubbing; update import statements
- [x] 5.4 Update `RegisterUserUseCaseTest` — remove `@Mock UserDtoMapper`; remove stubbing of `userDtoMapper.toDto()`; assert on returned `UserDto` fields directly; update import statements
- [x] 5.5 Update `GetUserByIdUseCaseTest` — remove `@Mock UserDtoMapper`; remove stubbing of `userDtoMapper.toDto()`; assert on returned `UserDto` fields directly; update import statements

## 6. Verify

- [x] 6.1 Run `./gradlew :backend:compileJava` — zero compilation errors
- [x] 6.2 Run `./gradlew :backend:test` — all tests green
- [x] 6.3 Confirm no remaining imports of `application.service` package with `grep -r "application.service" backend/src/`

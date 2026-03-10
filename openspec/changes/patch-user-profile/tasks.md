## 1. Dependencies & Configuration

- [x] 1.1 Add `implementation("org.openapitools:jackson-databind-nullable:0.2.9")` to `backend/build.gradle.kts`
- [x] 1.2 Register `JsonNullableModule` as a Spring `@Bean` in `backend/src/main/java/io/github/phunguy65/ttbs/backend/shared/infrastructure/web/WebConfig.java`
- [x] 1.3 Add `@EnableMethodSecurity` annotation to `SecurityConfig.java` (alongside the existing `@EnableWebSecurity`)

## 2. Application Layer — Command & Use Case

- [x] 2.1 Create `UpdateUserCommand.java` in `user/application/command/` as a record with fields: `UserId userId`, `JsonNullable<String> fullName`, `JsonNullable<String> email`, `JsonNullable<String> phone`
- [x] 2.2 Create `UpdateUserUseCase.java` in `user/application/usecase/` as `@Service @Transactional`
- [x] 2.3 In `UpdateUserUseCase.execute()`: load user by `userId`, return `Result.failure(new UserError.UserNotFound())` if absent
- [x] 2.4 In `UpdateUserUseCase.execute()`: if `email` is present and differs from current, check `userRepository.findByEmail()` and return `Result.failure(new UserError.EmailAlreadyExists())` if owned by a different user
- [x] 2.5 In `UpdateUserUseCase.execute()`: apply `JsonNullable.isPresent()` checks for each field and reconstruct via `User.reconstitute(...)` with `updatedAt = Instant.now()`
- [x] 2.6 In `UpdateUserUseCase.execute()`: save via `userRepository.save()` and return `Result.success(toDto(saved))`

## 3. Web Layer — Request DTO & Mapper

- [x] 3.1 Create `UpdateUserHttpRequest.java` in `user/infrastructure/web/` as a record with `JsonNullable<String> fullName` (`@NotBlank`), `JsonNullable<String> email` (`@NotBlank @Email`), `JsonNullable<String> phone` (no constraint); all fields default to `JsonNullable.undefined()`
- [x] 3.2 Add `toUpdateCommand(UUID userId, UpdateUserHttpRequest request)` method to `UserRequestMapper.java`

## 4. Controller — PATCH Endpoints

- [x] 4.1 Inject `UpdateUserUseCase` into `UserController` constructor
- [x] 4.2 Add `PATCH /{id}` endpoint (`@PatchMapping(value = "/{id}", version = "1.0")`) with `@PreAuthorize("hasRole('ADMIN')")`, `@PathVariable UUID id`, `@Valid @RequestBody UpdateUserHttpRequest`; delegate to `updateUserUseCase` and use `.fold()` for response
- [x] 4.3 Add `PATCH /me` endpoint (`@PatchMapping(value = "/me", version = "1.0")`) extracting `principalId` from `SecurityContextHolder`; delegate to same `updateUserUseCase` and use `.fold()` for response
- [x] 4.4 Extend the `errorResponse()` switch in `UserController` to include any new `UserError` variants if added (verify existing `UserNotFound` and `EmailAlreadyExists` cases cover PATCH errors — no new variants needed)

## 5. Tests — Use Case

- [x] 5.1 Create `UpdateUserUseCaseTest.java` in `user/application/usecase/` with `@ExtendWith(MockitoExtension.class)`
- [x] 5.2 Test: update succeeds with single field (`fullName` only) — other fields unchanged
- [x] 5.3 Test: update succeeds with `phone = null` (explicit removal)
- [x] 5.4 Test: update with empty command `{}` — no changes applied, returns current user
- [x] 5.5 Test: returns `UserNotFound` when user ID does not exist
- [x] 5.6 Test: returns `EmailAlreadyExists` when new email belongs to different user
- [x] 5.7 Test: does NOT return `EmailAlreadyExists` when email is unchanged (same user owns it)

## 6. Tests — Controller

- [x] 6.1 Create or extend `UserControllerTest.java` in `user/infrastructure/web/` with `@WebMvcTest`
- [x] 6.2 Test: `PATCH /me` with valid partial body → `200 OK` with JSend success + updated user
- [x] 6.3 Test: `PATCH /me` with blank `fullName` → `400 Bad Request` + `REQUIRED` violation
- [x] 6.4 Test: `PATCH /me` with invalid email format → `400 Bad Request` + `INVALID_FORMAT` violation
- [x] 6.5 Test: `PATCH /me` unauthenticated → `401 Unauthorized`
- [x] 6.6 Test: `PATCH /{id}` as admin → `200 OK`
- [x] 6.7 Test: `PATCH /{id}` as non-admin (customer) → `403 Forbidden`
- [x] 6.8 Test: `PATCH /{id}` targeting non-existent user → `404 Not Found` + `USER_NOT_FOUND`
- [x] 6.9 Test: `PATCH /me` with duplicate email → `409 Conflict` + `USER_EMAIL_ALREADY_EXISTS`

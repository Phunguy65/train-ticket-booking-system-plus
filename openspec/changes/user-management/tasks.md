## 1. Application Layer — GetUserByIdUseCase

- [x] 1.1 Create `GetUserByIdUseCase` in `user/application/usecase/GetUserByIdUseCase.java` — `@Service`, no `@Transactional`, calls `userRepository.findById(UserId)`, returns `Result<UserDto, UserError>`
- [x] 1.2 Verify `UserError.UserNotFound` is already defined in `UserError` sealed interface (no new error type needed)
- [x] 1.3 Verify `UserDto` already contains all required fields (`id`, `email`, `fullName`, `phone`, `role`, `createdAt`) with no `passwordHash`

## 2. Infrastructure Web Layer — UserController

- [x] 2.1 Create `UserRequestMapper` in `user/infrastructure/web/UserRequestMapper.java` — `@Component`, maps `UserDto` → `UserHttpResponse` (reuse existing `UserHttpResponse`)
- [x] 2.2 Create `UserController` in `user/infrastructure/web/UserController.java` — `@RestController`, `@RequestMapping("/api/v1/users")`
- [x] 2.3 Implement `GET /api/v1/users/{id}` in `UserController` — parse UUID path variable → `UserId.of(uuid)` → call `GetUserByIdUseCase` → fold result to `200 OK` or `404`
- [x] 2.4 Implement `GET /api/v1/users/me` in `UserController` — extract `UserId` from `SecurityContextHolder` authenticated principal → call same `GetUserByIdUseCase` → fold result

## 3. Error Handling

- [x] 3.1 Verify `ErrorCode.USER_NOT_FOUND` exists in `ErrorCode` enum for the 404 response
- [x] 3.2 Add `UserError.UserNotFound` → `404 Not Found` mapping in `UserController.errorResponse()` method (pattern same as `AuthController`)

## 4. Tests — Application Layer

- [x] 4.1 Create `GetUserByIdUseCaseTest` in `user/application/usecase/GetUserByIdUseCaseTest.java` — `@ExtendWith(MockitoExtension)`, test: found user returns `Result.success(UserDto)`, missing user returns `Result.failure(UserNotFound)`

## 5. Tests — Web Layer

- [x] 5.1 Create `UserControllerTest` in `user/infrastructure/web/UserControllerTest.java` — `@WebMvcTest(UserController.class)`, mock `GetUserByIdUseCase` and `TokenProvider`
- [x] 5.2 Test `GET /api/v1/users/{id}` — valid UUID + found → 200 with user fields
- [x] 5.3 Test `GET /api/v1/users/{id}` — valid UUID + not found → 404 with `USER_NOT_FOUND` error code
- [x] 5.4 Test `GET /api/v1/users/me` — `@WithMockUser` → 200 with user fields

## 6. Verification

- [x] 6.1 Run `./gradlew :backend:test --tests "*.user.*"` — all user module tests pass
- [x] 6.2 Run `./gradlew :backend:test --tests "*.UserModuleTest"` — Spring Modulith module verification passes

## 1. Application Layer — DTOs

- [x] 1.1 Create `CreateUserCommand` in `user/application/command/CreateUserCommand.java` — record with fields: `String email`, `String fullName`, `String phone` (nullable); **no password field**
- [x] 1.2 Create `CreateUserResult` in `user/application/dto/CreateUserResult.java` — record with fields: `UserDto user`, `String temporaryPassword`

## 2. Application Layer — CreateUserUseCase

- [x] 2.1 Create `CreateUserUseCase` in `user/application/usecase/CreateUserUseCase.java` — `@Service`, `@Transactional`, inject `UserRepository`, `PasswordEncoder`, `ApplicationEventPublisher`
- [x] 2.2 Implement `execute(CreateUserCommand command)` returning `Result<CreateUserResult, UserError>`:
  - Check email uniqueness via `userRepository.findByEmail(command.email())` — return `Result.failure(new UserError.EmailAlreadyExists())` if present
  - Generate temporary password: `UUID.randomUUID().toString().replace("-", "")`
  - Encode: `passwordEncoder.encode(temporaryPassword)`
  - Create aggregate: `User.create(UserId.of(UUID.randomUUID()), command.email(), passwordHash, command.fullName(), command.phone())`
  - Save: `userRepository.save(user)`
  - Publish domain events from `user.getDomainEvents()`, then `user.clearDomainEvents()`
  - Return `Result.success(new CreateUserResult(toDto(saved), temporaryPassword))`
- [x] 2.3 Add private `toDto(User user)` helper mapping to `UserDto` (same as in `RegisterUserUseCase`)

## 3. Infrastructure Web Layer — HTTP DTOs

- [x] 3.1 Create `CreateUserHttpRequest` in `user/infrastructure/web/CreateUserHttpRequest.java` — record with:
  - `@Email @NotBlank String email`
  - `@NotBlank String fullName`
  - `String phone` (nullable, no validation)
- [x] 3.2 Create `CreateUserHttpResponse` in `user/infrastructure/web/CreateUserHttpResponse.java` — record with fields: `UUID id`, `String email`, `String fullName`, `String phone`, `String role`, `Instant createdAt`, `String temporaryPassword`

## 4. Infrastructure Web Layer — Mapper & Controller

- [x] 4.1 Extend `UserRequestMapper` — add `CreateUserCommand toCommand(CreateUserHttpRequest request)` method
- [x] 4.2 Extend `UserRequestMapper` — add `CreateUserHttpResponse toCreateResponse(CreateUserResult result)` method mapping `result.user()` fields plus `result.temporaryPassword()`
- [x] 4.3 Inject `CreateUserUseCase` into `UserController` constructor
- [x] 4.4 Add `POST /api/v1/users` endpoint in `UserController`:
  - `@PostMapping`, `@Valid @RequestBody CreateUserHttpRequest request`
  - Call `createUserUseCase.execute(mapper.toCommand(request))`
  - Fold: success → `ResponseEntity.created(location).body(JsendResponse.success(mapper.toCreateResponse(result)))` where `location = URI of /api/v1/users/{id}`
  - Fold: error → delegate to existing `errorResponse(error)` method

## 5. Tests — Application Layer

- [x] 5.1 Create `CreateUserUseCaseTest` in `user/application/usecase/CreateUserUseCaseTest.java` — `@ExtendWith(MockitoExtension.class)`, mock `UserRepository`, `PasswordEncoder`, `ApplicationEventPublisher`
- [x] 5.2 Test: unique email → `Result.success` with `CreateUserResult` containing non-blank `temporaryPassword` and correct `UserDto` fields
- [x] 5.3 Test: `passwordEncoder.encode()` is called with the generated temporary password (verify `encode` is invoked once)
- [x] 5.4 Test: `UserRegistered` event is published via `eventPublisher.publishEvent()`
- [x] 5.5 Test: duplicate email → `Result.failure(UserError.EmailAlreadyExists)` and `userRepository.save()` is never called

## 6. Tests — Web Layer

- [x] 6.1 Create `UserControllerTest` additions (or create new test if none exists) — `@WebMvcTest(UserController.class)`, mock `CreateUserUseCase` and `GetUserByIdUseCase`
- [x] 6.2 Test `POST /api/v1/users` — valid request + unique email → `201 Created` with `temporaryPassword` field present in response body
- [x] 6.3 Test `POST /api/v1/users` — duplicate email → `409 Conflict` with `USER_EMAIL_ALREADY_EXISTS` error code
- [x] 6.4 Test `POST /api/v1/users` — blank email → `400 Bad Request` with `VALIDATION_ERROR` code and field violation for `email`
- [x] 6.5 Test `POST /api/v1/users` — blank fullName → `400 Bad Request` with `VALIDATION_ERROR` code and field violation for `fullName`
- [x] 6.6 Verify `GET /api/v1/users/{id}` response does NOT contain `temporaryPassword` field

## 7. Verification

- [x] 7.1 Run `./gradlew :backend:test --tests "*.user.*"` — all user module tests pass
- [x] 7.2 Run `./gradlew :backend:test --tests "*.UserModuleTest"` — Spring Modulith module verification passes

## 1. Move UserId to user/domain/model

- [x] 1.1 Tạo file `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/domain/model/UserId.java` với package declaration mới: `package io.github.phunguy65.ttbs.backend.user.domain.model;` (giữ nguyên toàn bộ logic)
- [x] 1.2 Xóa file cũ `backend/src/main/java/io/github/phunguy65/ttbs/backend/shared/domain/UserId.java`

## 2. Configure Spring Modulith Named Interface

- [x] 2.1 Tạo `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/domain/model/package-info.java` với annotation `@org.springframework.modulith.NamedInterface("model")`
- [x] 2.2 Update `backend/src/main/java/io/github/phunguy65/ttbs/backend/booking/package-info.java`: thêm `allowedDependencies = "user::model"` vào `@ApplicationModule`

## 3. Update imports — user slice (main)

- [x] 3.1 Update import trong `user/application/usecase/RegisterUserUseCase.java`
- [x] 3.2 Update import trong `user/application/usecase/GetUserByIdUseCase.java`
- [x] 3.3 Update import trong `user/application/usecase/CreateUserUseCase.java`
- [x] 3.4 Update import trong `user/application/port/TokenProvider.java`
- [x] 3.5 Update import trong `user/application/command/UpdateUserCommand.java`
- [x] 3.6 Update import trong `user/domain/event/UserRegistered.java`
- [x] 3.7 Update import trong `user/domain/model/User.java`
- [x] 3.8 Update import trong `user/domain/repository/UserRepository.java`
- [x] 3.9 Update import trong `user/domain/repository/RefreshTokenRepository.java`
- [x] 3.10 Update import trong `user/infrastructure/web/UserRequestMapper.java`
- [x] 3.11 Update import trong `user/infrastructure/web/UserController.java`
- [x] 3.12 Update import trong `user/infrastructure/security/JwtTokenProvider.java`
- [x] 3.13 Update import trong `user/infrastructure/security/UserDetailsServiceImpl.java`
- [x] 3.14 Update import trong `user/infrastructure/security/JwtAuthenticationFilter.java`
- [x] 3.15 Update import trong `user/infrastructure/persistence/UserRepositoryAdapter.java`
- [x] 3.16 Update import trong `user/infrastructure/persistence/RefreshTokenRepositoryAdapter.java`
- [x] 3.17 Update import trong `user/infrastructure/persistence/UserEntityMapper.java`

## 4. Update imports — booking slice (main)

- [x] 4.1 Update import trong `booking/domain/model/Booking.java`

## 5. Update imports — test files

- [x] 5.1 Update import trong `user/application/usecase/UpdateUserUseCaseTest.java`
- [x] 5.2 Update import trong `user/application/usecase/RefreshTokenUseCaseTest.java`
- [x] 5.3 Update import trong `user/application/usecase/LoginUserUseCaseTest.java`
- [x] 5.4 Update import trong `user/application/usecase/GetUserByIdUseCaseTest.java`
- [x] 5.5 Update import trong `user/application/usecase/ListUsersUseCaseTest.java`
- [x] 5.6 Update import trong `user/application/usecase/LogoutUserUseCaseTest.java`
- [x] 5.7 Update import trong `user/infrastructure/security/JwtTokenProviderTest.java`
- [x] 5.8 Update import trong `user/infrastructure/persistence/UserRepositoryAdapterTest.java`
- [x] 5.9 Update fully-qualified reference trong `user/application/usecase/RegisterUserUseCaseTest.java`
- [x] 5.10 Update fully-qualified reference trong `user/application/usecase/CreateUserUseCaseTest.java`

## 6. Verify

- [x] 6.1 Chạy `./gradlew compileJava compileTestJava` — đảm bảo zero compilation errors
- [x] 6.2 Grep toàn bộ codebase để đảm bảo không còn reference nào tới `shared.domain.UserId`: `grep -r "shared.domain.UserId" backend/src/`
- [x] 6.3 Chạy `./gradlew test --tests "*BookingModuleTest*"` — đảm bảo module boundary test pass
- [x] 6.4 Chạy `./gradlew test --tests "*UserModuleTest*"` — đảm bảo module boundary test pass
- [x] 6.5 Chạy `./gradlew test` — đảm bảo toàn bộ test suite pass

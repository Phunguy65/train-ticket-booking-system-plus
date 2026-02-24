## 1. Move UserId to user/domain/model

- [ ] 1.1 Tạo file `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/domain/model/UserId.java` với package declaration mới: `package io.github.phunguy65.ttbs.backend.user.domain.model;` (giữ nguyên toàn bộ logic)
- [ ] 1.2 Xóa file cũ `backend/src/main/java/io/github/phunguy65/ttbs/backend/shared/domain/UserId.java`

## 2. Configure Spring Modulith Named Interface

- [ ] 2.1 Tạo `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/domain/model/package-info.java` với annotation `@org.springframework.modulith.NamedInterface("model")`
- [ ] 2.2 Update `backend/src/main/java/io/github/phunguy65/ttbs/backend/booking/package-info.java`: thêm `allowedDependencies = "user::model"` vào `@ApplicationModule`

## 3. Update imports — user slice (main)

- [ ] 3.1 Update import trong `user/application/usecase/RegisterUserUseCase.java`
- [ ] 3.2 Update import trong `user/application/usecase/GetUserByIdUseCase.java`
- [ ] 3.3 Update import trong `user/application/usecase/CreateUserUseCase.java`
- [ ] 3.4 Update import trong `user/application/port/TokenProvider.java`
- [ ] 3.5 Update import trong `user/application/command/UpdateUserCommand.java`
- [ ] 3.6 Update import trong `user/domain/event/UserRegistered.java`
- [ ] 3.7 Update import trong `user/domain/model/User.java`
- [ ] 3.8 Update import trong `user/domain/repository/UserRepository.java`
- [ ] 3.9 Update import trong `user/domain/repository/RefreshTokenRepository.java`
- [ ] 3.10 Update import trong `user/infrastructure/web/UserRequestMapper.java`
- [ ] 3.11 Update import trong `user/infrastructure/web/UserController.java`
- [ ] 3.12 Update import trong `user/infrastructure/security/JwtTokenProvider.java`
- [ ] 3.13 Update import trong `user/infrastructure/security/UserDetailsServiceImpl.java`
- [ ] 3.14 Update import trong `user/infrastructure/security/JwtAuthenticationFilter.java`
- [ ] 3.15 Update import trong `user/infrastructure/persistence/UserRepositoryAdapter.java`
- [ ] 3.16 Update import trong `user/infrastructure/persistence/RefreshTokenRepositoryAdapter.java`
- [ ] 3.17 Update import trong `user/infrastructure/persistence/UserEntityMapper.java`

## 4. Update imports — booking slice (main)

- [ ] 4.1 Update import trong `booking/domain/model/Booking.java`

## 5. Update imports — test files

- [ ] 5.1 Update import trong `user/application/usecase/UpdateUserUseCaseTest.java`
- [ ] 5.2 Update import trong `user/application/usecase/RefreshTokenUseCaseTest.java`
- [ ] 5.3 Update import trong `user/application/usecase/LoginUserUseCaseTest.java`
- [ ] 5.4 Update import trong `user/application/usecase/GetUserByIdUseCaseTest.java`
- [ ] 5.5 Update import trong `user/application/usecase/ListUsersUseCaseTest.java`
- [ ] 5.6 Update import trong `user/application/usecase/LogoutUserUseCaseTest.java`
- [ ] 5.7 Update import trong `user/infrastructure/security/JwtTokenProviderTest.java`
- [ ] 5.8 Update import trong `user/infrastructure/persistence/UserRepositoryAdapterTest.java`
- [ ] 5.9 Update fully-qualified reference trong `user/application/usecase/RegisterUserUseCaseTest.java`
- [ ] 5.10 Update fully-qualified reference trong `user/application/usecase/CreateUserUseCaseTest.java`

## 6. Verify

- [ ] 6.1 Chạy `./gradlew compileJava compileTestJava` — đảm bảo zero compilation errors
- [ ] 6.2 Grep toàn bộ codebase để đảm bảo không còn reference nào tới `shared.domain.UserId`: `grep -r "shared.domain.UserId" backend/src/`
- [ ] 6.3 Chạy `./gradlew test --tests "*BookingModuleTest*"` — đảm bảo module boundary test pass
- [ ] 6.4 Chạy `./gradlew test --tests "*UserModuleTest*"` — đảm bảo module boundary test pass
- [ ] 6.5 Chạy `./gradlew test` — đảm bảo toàn bộ test suite pass

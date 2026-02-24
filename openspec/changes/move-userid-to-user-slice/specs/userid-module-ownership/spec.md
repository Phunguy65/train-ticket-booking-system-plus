## ADDED Requirements

### Requirement: UserId lives in user domain model package

`UserId` SHALL reside at `io.github.phunguy65.ttbs.backend.user.domain.model.UserId` as a value object owned by the `user` module.

#### Scenario: UserId package location is correct

-  **WHEN** the codebase is compiled
-  **THEN** `UserId.java` exists at `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/domain/model/UserId.java`
-  **THEN** the package declaration reads `package io.github.phunguy65.ttbs.backend.user.domain.model;`
-  **THEN** no `UserId.java` exists at `backend/src/main/java/io/github/phunguy65/ttbs/backend/shared/domain/UserId.java`

### Requirement: user domain model package is exposed as Named Interface

The `user` module SHALL expose its `domain/model` sub-package as a Spring Modulith Named Interface named `"model"` so other modules can access it explicitly.

#### Scenario: Named interface declared in package-info

-  **WHEN** Spring Modulith scans the application
-  **THEN** `user/domain/model/package-info.java` exists with `@NamedInterface("model")` annotation
-  **THEN** all public types in `io.github.phunguy65.ttbs.backend.user.domain.model` are accessible to modules that declare `"user::model"` as an allowed dependency

### Requirement: booking module declares explicit dependency on user::model

The `booking` module SHALL explicitly declare its dependency on the `user::model` named interface via `@ApplicationModule(allowedDependencies = "user::model")`.

#### Scenario: booking module passes ApplicationModuleTest after dependency declaration

-  **WHEN** `BookingModuleTest` is executed with `@ApplicationModuleTest`
-  **THEN** the test passes without module boundary violations
-  **THEN** Spring Modulith confirms `booking` is allowed to access `user::model`

#### Scenario: booking module cannot access internal user packages

-  **WHEN** code in `booking` module attempts to import from `io.github.phunguy65.ttbs.backend.user.application` or `io.github.phunguy65.ttbs.backend.user.infrastructure`
-  **THEN** Spring Modulith reports a module boundary violation at test time

### Requirement: All codebase references use new UserId package

All Java files that previously imported `io.github.phunguy65.ttbs.backend.shared.domain.UserId` SHALL be updated to import `io.github.phunguy65.ttbs.backend.user.domain.model.UserId`.

#### Scenario: No stale imports remain after the move

-  **WHEN** the project is compiled with `./gradlew compileJava compileTestJava`
-  **THEN** compilation succeeds with zero errors related to `UserId` not found
-  **THEN** no file contains the string `import io.github.phunguy65.ttbs.backend.shared.domain.UserId`

#### Scenario: Fully-qualified references are also updated

-  **WHEN** the project is compiled
-  **THEN** no file contains the fully-qualified reference `io.github.phunguy65.ttbs.backend.shared.domain.UserId` (including in test files)

### Requirement: UserId behavior is unchanged after the move

The `UserId` record SHALL retain identical behavior: construction validation, `of()` factory method, and `toString()` implementation.

#### Scenario: UserId rejects null values

-  **WHEN** `new UserId(null)` is called
-  **THEN** an `IllegalArgumentException` is thrown with message `"UserId value must not be null"`

#### Scenario: UserId factory method works correctly

-  **WHEN** `UserId.of(uuid)` is called with a valid `UUID`
-  **THEN** a `UserId` instance is returned with `value()` equal to the given `UUID`

#### Scenario: UserId toString returns UUID string

-  **WHEN** `userId.toString()` is called
-  **THEN** the result equals `userId.value().toString()`

### Requirement: Module tests pass after the refactoring

Both `@ApplicationModuleTest` tests SHALL pass with no module structure violations.

#### Scenario: UserModuleTest passes

-  **WHEN** `UserModuleTest` is executed
-  **THEN** the Spring context loads successfully
-  **THEN** all existing user module tests continue to pass

#### Scenario: BookingModuleTest passes

-  **WHEN** `BookingModuleTest` is executed
-  **THEN** the Spring context loads successfully
-  **THEN** all existing booking module tests continue to pass

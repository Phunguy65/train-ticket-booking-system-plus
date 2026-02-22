# ADDED Requirements

## Requirement: Backend package structure follows vertical slice per bounded context

The backend SHALL organize all code into top-level feature packages (bounded contexts) under `io.github.phunguy65.ttbs.backend`, with each bounded context being an independent Spring Modulith module. Technical layer packages (controllers/, services/, repositories/) at the global level are PROHIBITED.

Bounded context packages SHALL include: `booking/`, `payment/`, `train/`, `user/`. The `shared/` package is reserved for cross-cutting domain building blocks only.

#### Scenario: Feature code is colocated within its bounded context package

- **WHEN** a developer implements any code related to a feature (e.g., booking)
- **THEN** all code for that feature SHALL reside inside the corresponding bounded context package (`booking/`) and its sub-packages, never in a global technical layer package

#### Scenario: No global technical layer packages exist

- **WHEN** a developer adds a new controller, service, or repository
- **THEN** the file SHALL be placed inside a bounded context package (e.g., `booking/infrastructure/web/`), NOT in a global `controllers/` or `services/` package at the root backend level

## Requirement: Each vertical slice has a defined internal layer structure

Within each bounded context package, code SHALL be organized into exactly three sub-packages following Clean Architecture: `domain/`, `application/`, and `infrastructure/`. The infrastructure package SHALL be further divided into `persistence/` and `web/` sub-packages.

#### Scenario: Domain sub-package contains only pure domain objects

- **WHEN** a file is placed in `{context}/domain/`
- **THEN** it SHALL be a domain model, value object, domain event, or repository interface with zero dependencies on Spring, JPA, Jackson, or any other framework

#### Scenario: Application sub-package contains use cases and DTOs

- **WHEN** a file is placed in `{context}/application/`
- **THEN** it SHALL be a use case class or application-level DTO (command, query, result), and SHALL only depend on `domain/` and `shared/`

#### Scenario: Infrastructure sub-package contains adapters

- **WHEN** a file is placed in `{context}/infrastructure/`
- **THEN** it SHALL be a framework-specific adapter: JPA entity, Spring Data repository, mapper, REST controller, or request/response DTO

## Requirement: Spring Modulith module boundaries are declared and enforced

Each bounded context package SHALL have a `package-info.java` file at its root to declare the Spring Modulith public API. Sub-packages under `infrastructure/` SHALL be treated as internal and not accessible from other modules.

#### Scenario: Public API is declared via package-info.java

- **WHEN** a bounded context package is created
- **THEN** a `package-info.java` SHALL exist at the package root with `@org.springframework.modulith.ApplicationModule` annotation

#### Scenario: Module boundary violations are caught at test time

- **WHEN** one module attempts to directly access an internal class of another module (e.g., accessing `booking.infrastructure.persistence.BookingEntity` from `payment` module)
- **THEN** a Spring Modulith `@ApplicationModuleTest` SHALL fail with a dependency violation

## Requirement: Modules communicate only via domain events

Direct method calls between bounded context modules ARE PROHIBITED. Modules SHALL communicate asynchronously via Spring Modulith application events published from the `domain/event/` package.

#### Scenario: Cross-module communication uses events

- **WHEN** a booking is confirmed and the payment module needs to know about it
- **THEN** the booking module SHALL publish a `BookingConfirmed` domain event, and the payment module SHALL listen to that event — no direct service-to-service method call occurs

#### Scenario: Shared kernel is accessible to all modules

- **WHEN** any module needs common domain building blocks (e.g., `Money`, `AggregateRoot`, `ValueObject`)
- **THEN** it SHALL import from `shared/domain/` which is accessible to all modules without restriction

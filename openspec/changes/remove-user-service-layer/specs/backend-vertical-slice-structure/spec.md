## MODIFIED Requirements

### Requirement: Each vertical slice has a defined internal layer structure

Within each bounded context package, code SHALL be organized into exactly three sub-packages following Clean Architecture: `domain/`, `application/`, and `infrastructure/`. The infrastructure package SHALL be further divided into `persistence/` and `web/` sub-packages.

The `application/` sub-package SHALL contain only `usecase/`, `dto/`, `command/`, and `port/` sub-packages. A nested `service/` sub-package inside `application/` is PROHIBITED.

Token-management or any other cross-use-case helper that requires infrastructure dependencies SHALL be expressed as a port interface in `application/port/` and implemented as an adapter in `infrastructure/`.

Pure mapping logic (domain model → application DTO) SHALL be inlined as a `private` method in the use case class that needs it, not extracted into a shared mapper component inside `application/`.

#### Scenario: Domain sub-package contains only pure domain objects

- **WHEN** a file is placed in `{context}/domain/`
- **THEN** it SHALL be a domain model, value object, domain event, or repository interface with zero dependencies on Spring, JPA, Jackson, or any other framework

#### Scenario: Application sub-package contains use cases, DTOs, and ports only

- **WHEN** a file is placed in `{context}/application/`
- **THEN** it SHALL be a use case class, application-level DTO (command, query, result), or port interface, and SHALL only depend on `domain/` and `shared/`

#### Scenario: No service sub-package exists inside application

- **WHEN** a developer adds a shared helper class inside `{context}/application/`
- **THEN** the file SHALL NOT be placed in an `application/service/` sub-package; if the helper requires infrastructure dependencies it SHALL be a port interface in `application/port/` with an adapter in `infrastructure/`; if it is a pure function it SHALL be inlined in the consuming use case

#### Scenario: Infrastructure sub-package contains adapters

- **WHEN** a file is placed in `{context}/infrastructure/`
- **THEN** it SHALL be a framework-specific adapter: JPA entity, Spring Data repository, mapper, REST controller, request/response DTO, or port implementation

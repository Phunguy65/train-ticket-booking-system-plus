# ADDED Requirements

## Requirement: ORM entities and domain models are separate classes

JPA `@Entity` classes and domain model classes SHALL be distinct, separate Java/Kotlin classes. A single class SHALL NOT have both `@Entity` annotation and business domain logic. Domain models SHALL be free of all ORM annotations (`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, etc.).

#### Scenario: Domain model has no JPA annotations

- **WHEN** examining a domain model class in any `{context}/domain/model/` package
- **THEN** the class SHALL NOT contain any JPA annotations (`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@OneToMany`, `@ManyToOne`, etc.)

#### Scenario: JPA entity has no business logic

- **WHEN** examining a JPA entity class in any `{context}/infrastructure/persistence/` package
- **THEN** the class SHALL only contain field declarations, JPA annotations, and a protected/package-private default constructor — no business methods, no domain event registration

#### Scenario: ORM entities are confined to infrastructure/persistence

- **WHEN** searching for all classes annotated with `@Entity`
- **THEN** all such classes SHALL reside exclusively in a `{context}/infrastructure/persistence/` package, never in `domain/` or `application/`

## Requirement: Repository pattern uses Port and Adapter

Each bounded context that requires persistence SHALL define a repository interface (port) in `domain/repository/` and a concrete implementation (adapter) in `infrastructure/persistence/`. The domain/application layers SHALL depend ONLY on the repository interface, never on Spring Data JPA interfaces directly.

#### Scenario: Repository port is defined in domain layer

- **WHEN** a bounded context needs to persist or retrieve domain objects
- **THEN** a repository interface SHALL exist in `{context}/domain/repository/` with methods that accept/return domain model types (not JPA entity types)

#### Scenario: Repository adapter implements domain port

- **WHEN** examining the infrastructure persistence package
- **THEN** a `*RepositoryAdapter` class SHALL exist that implements the domain repository interface and delegates to a Spring Data JPA repository internally

#### Scenario: Use cases depend on domain repository interface

- **WHEN** a use case class needs to persist or retrieve data
- **THEN** it SHALL inject the domain repository interface (port), NOT the Spring Data JPA repository directly

## Requirement: Mapper classes handle all conversions between layers

Dedicated mapper classes SHALL handle all conversions between: (a) JPA Entity ↔ Domain Model, and (b) HTTP Request/Response DTO ↔ Application Command/Result. Domain models SHALL NOT have any `toEntity()`, `toDto()`, or similar methods — conversion logic belongs exclusively in mappers.

#### Scenario: Entity-to-domain mapping is handled by EntityMapper

- **WHEN** the repository adapter needs to convert a JPA entity to a domain model
- **THEN** it SHALL delegate to a `*EntityMapper` class (or Kotlin extension functions) in the `infrastructure/persistence/` package

#### Scenario: DTO-to-command mapping is handled by DtoMapper

- **WHEN** a REST controller receives an HTTP request and needs to invoke a use case
- **THEN** it SHALL use a `*RequestMapper` or `*DtoMapper` in `infrastructure/web/` to convert the HTTP DTO to an application command before calling the use case

#### Scenario: Domain model has no serialization or persistence methods

- **WHEN** examining a domain model class
- **THEN** it SHALL NOT have methods named `toEntity()`, `toDto()`, `toJson()`, or any method whose purpose is to convert the domain object to another representation

## Requirement: Domain models support reconstitution from persistence

Domain aggregate classes SHALL provide a factory method or secondary constructor to reconstitute the domain object from raw data (as returned by the mapper from a JPA entity). This reconstitution path SHALL bypass business rule validation that is only appropriate for new object creation.

#### Scenario: Aggregate has a reconstitute factory method

- **WHEN** the mapper reads data from a JPA entity and constructs the domain model
- **THEN** it SHALL use a `reconstitute(...)` static factory method (or named constructor) on the domain class, which sets fields without triggering creation-time domain events

#### Scenario: Creation factory method emits domain events

- **WHEN** a new aggregate is created via its `create(...)` static factory method
- **THEN** the factory SHALL register the appropriate creation domain event (e.g., `BookingCreated`) via `registerEvent()`

#### Scenario: Reconstitution factory method does NOT emit domain events

- **WHEN** an aggregate is reconstituted from persistence via `reconstitute(...)`
- **THEN** NO domain events SHALL be registered — the object is being restored, not created anew

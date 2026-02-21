## Context

Backend hiện tại (`io.github.phunguy65.ttbs.backend`) có foundation DDD tốt với `AggregateRoot<ID>`, `Money`, `ValueObject`, `DomainEvent` đã được implement trong `shared/domain/`. Stack: Spring Boot 4.0.3, Spring Modulith 2.0.3, Hibernate 7.2.1, Spring Data JPA, PostgreSQL. Database schema đã được định nghĩa qua Flyway migrations với 7 tables (users, stations, trains, routes, seats, bookings, transactions) dùng UUID v7 làm primary key. Hiện tại **chưa có bất kỳ feature nào được implement** — đây là cơ hội thiết lập kiến trúc chuẩn ngay từ đầu.

## Goals / Non-Goals

**Goals:**
- Thiết lập package structure và layer conventions theo Vertical Slice Architecture
- Tách biệt ORM Entity (`@Entity`) hoàn toàn khỏi Domain Model (pure Java/Kotlin)
- Áp dụng Repository Port/Adapter pattern: domain định nghĩa interface, infrastructure implement
- Implement `booking` module làm reference implementation (full vertical slice)
- Tận dụng Spring Modulith module boundaries và event-driven inter-module communication
- Đảm bảo domain layer có thể unit test độc lập (không cần Spring context hay database)

**Non-Goals:**
- Implement tất cả features/bounded contexts trong change này (chỉ `booking` làm reference)
- Thay đổi database schema (Flyway migrations đã được định nghĩa)
- Implement authentication/authorization logic
- Implement payment hoặc notification flows

## Decisions

### D1: Vertical Slice theo Bounded Context, không theo Layer

**Decision**: Tổ chức top-level packages theo bounded context (`booking/`, `payment/`, `train/`, `user/`), không theo technical layer (`controllers/`, `services/`, `repositories/`).

**Rationale**: Spring Modulith enforce package-based module boundaries. Code liên quan đến một feature nằm cùng nhau → dễ hiểu, dễ thay đổi, dễ xóa. Thêm feature mới chỉ cần thêm 1 package mới.

**Package structure trong mỗi slice:**
```
booking/
├── package-info.java          # Spring Modulith: khai báo public API
├── domain/
│   ├── model/                 # Domain aggregates, value objects, enums
│   ├── event/                 # Domain events (sealed classes)
│   └── repository/            # Repository interfaces (ports)
├── application/
│   ├── usecase/               # Use case classes (one class per operation)
│   └── dto/                   # Application DTOs (command/query/result)
└── infrastructure/
    ├── persistence/           # JPA entities, Spring Data repos, adapters, mappers
    └── web/                   # REST controllers, request/response DTOs, mappers
```

### D2: Tách biệt ORM Entity và Domain Model

**Decision**: Domain model là pure Java/Kotlin objects extends `AggregateRoot<ID>` hoặc implements `ValueObject`. JPA `@Entity` classes tồn tại hoàn toàn độc lập trong `infrastructure/persistence/`. Hai bên được bridge bởi `*Mapper` classes.

**Rationale**: Persistence ignorance — business rules không bị ảnh hưởng bởi database schema changes. Testability — domain tests chạy không cần Spring context. Power — domain model có thể dùng Kotlin `sealed class`, `value class`, immutable `val` properties; JPA entity cần `var`, nullable fields, default constructor.

**Mapping flow:**
```
HTTP Request → RequestDTO → [DtoMapper] → Command → [UseCase] → Domain Model
Domain Model → [EntityMapper] → JPA Entity → persist to DB
DB read → JPA Entity → [EntityMapper] → Domain Model → [DtoMapper] → ResponseDTO → HTTP Response
```

### D3: Repository Port/Adapter Pattern

**Decision**: Domain layer định nghĩa repository interface (port) không có JPA/Spring dependency. Infrastructure layer cung cấp concrete implementation (adapter) using Spring Data JPA.

**Rationale**: Dependency Inversion Principle — application/domain không phụ thuộc vào infrastructure. Có thể swap persistence layer (JPA → Exposed → R2DBC) mà không thay đổi domain/application code. Dễ mock trong unit tests.

**Pattern:**
```java
// Domain port (domain/repository/)
public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(BookingId id);
}

// Infrastructure adapter (infrastructure/persistence/)
@Repository
class BookingRepositoryAdapter implements BookingRepository {
    // Delegates to Spring Data JPA
}
```

### D4: Use Cases làm Application Service

**Decision**: Mỗi use case là một class riêng biệt với một method public `execute()`. Không có "service" god class. `@Transactional` annotation chỉ ở use case layer.

**Rationale**: Single Responsibility Principle — mỗi use case class chỉ biết về một operation. Dễ test, dễ audit, dễ thêm/xóa feature. Transaction boundaries rõ ràng.

### D5: Spring Modulith Module Communication

**Decision**: Các modules giao tiếp qua Spring Modulith application events, không gọi trực tiếp public API của nhau (trừ shared kernel).

**Rationale**: Loose coupling giữa modules. Spring Modulith enforce boundary violations tại test time. Event publication được Spring Modulith persist và retry để đảm bảo at-least-once delivery.

### D6: Value Objects với Kotlin `@JvmInline value class`

**Decision**: ID types và simple wrappers được implement là Kotlin `@JvmInline value class`. Regular domain concepts dùng `data class`.

**Rationale**: Type safety ở compile time (không thể nhầm `BookingId` với `UserId`), zero runtime overhead với `@JvmInline`.

## Risks / Trade-offs

| Risk | Mức độ | Mitigation |
|------|--------|------------|
| **Mapping boilerplate** | MEDIUM | Chấp nhận — viết tay mapper rõ ràng hơn code generation. Kotlin extension functions giảm verbose. MapStruct có thể dùng nếu cần. |
| **JPA Lazy Loading leaking** | HIGH | Repository adapter phải fetch đầy đủ data và trả về fully-constructed domain model. Không để domain model trigger lazy load. |
| **Transaction scope** | MEDIUM | `@Transactional` chỉ ở use case layer. Domain model và repository interface không biết về transactions. |
| **Spring Modulith circular events** | LOW | Thiết kế event flow theo một chiều, tránh event cycles giữa modules. |
| **Over-engineering cho CRUD đơn giản** | LOW | Với queries đơn giản (read-only), có thể dùng "thin slice" (không cần full domain model). |
| **JPA entity `@Entity` chỉ có default constructor** | MEDIUM | JPA entity dùng mutable `var` và protected default constructor. Mappers chịu trách nhiệm khởi tạo đúng cách. |

---

## Components

| Component | Trách nhiệm | Location |
|-----------|-------------|----------|
| `Booking` | Domain aggregate — enforce booking business rules, domain events | `booking/domain/model/Booking.java` |
| `BookingId`, `UserId`, `RouteId`, `SeatId` | Type-safe ID value objects | `booking/domain/model/*.java` |
| `BookingStatus` | Booking lifecycle enum | `booking/domain/model/BookingStatus.java` |
| `BookingCreated`, `BookingConfirmed` | Domain events | `booking/domain/event/*.java` |
| `BookingRepository` | Repository port (interface) | `booking/domain/repository/BookingRepository.java` |
| `CreateBookingUseCase` | Application service — orchestrate booking creation | `booking/application/usecase/CreateBookingUseCase.java` |
| `GetBookingUseCase` | Application service — fetch booking by ID | `booking/application/usecase/GetBookingUseCase.java` |
| `CreateBookingCommand` | Input DTO for booking creation | `booking/application/dto/CreateBookingCommand.java` |
| `BookingDto` | Output DTO (response) | `booking/application/dto/BookingDto.java` |
| `BookingEntity` | JPA entity — maps to `bookings` table | `booking/infrastructure/persistence/BookingEntity.java` |
| `BookingJpaRepository` | Spring Data JPA interface | `booking/infrastructure/persistence/BookingJpaRepository.java` |
| `BookingRepositoryAdapter` | Repository adapter — implements domain port | `booking/infrastructure/persistence/BookingRepositoryAdapter.java` |
| `BookingEntityMapper` | Maps `BookingEntity` ↔ `Booking` domain model | `booking/infrastructure/persistence/BookingEntityMapper.java` |
| `BookingController` | REST controller — HTTP endpoints for booking | `booking/infrastructure/web/BookingController.java` |
| `BookingRequestMapper` | Maps HTTP request DTOs ↔ Application commands | `booking/infrastructure/web/BookingRequestMapper.java` |
| `package-info.java` | Spring Modulith public API declaration | `booking/package-info.java` |

## Key Flows

### Flow 1: Create Booking (POST /api/bookings)

```
Client
  │ POST /api/bookings {userId, routeId, seatId}
  ▼
BookingController
  │ map: CreateBookingHttpRequest → CreateBookingCommand (BookingRequestMapper)
  ▼
CreateBookingUseCase (@Transactional)
  │ validate input, call domain
  │
  ├── routeRepository.findById(routeId)  → Route domain model
  ├── seatRepository.findById(seatId)    → Seat domain model
  │
  │ Booking.create(userId, routeId, seatId, price)  → registers BookingCreated event
  │
  ├── bookingRepository.save(booking)    → BookingEntityMapper maps → DB persist
  │
  │ SpringModulith publishes BookingCreated event (async)
  │    └─▶ SeatInventoryModule listens → reserves seat
  │
  │ return BookingDto
  ▼
BookingController
  │ map: BookingDto → BookingHttpResponse
  ▼
Client (201 Created)
```

### Flow 2: Entity ↔ Domain Model Mapping

```
BookingJpaRepository.findById(uuid)
  │
  ▼ BookingEntity (JPA entity, has @Entity)
  │  - id: UUID
  │  - userId: UUID
  │  - status: "CONFIRMED" (String enum)
  │
  ▼ BookingEntityMapper.toDomain(entity)
  │
  ▼ Booking (domain model, extends AggregateRoot<BookingId>)
     - id: BookingId(uuid)    ← value class wrapper
     - userId: UserId(uuid)   ← value class wrapper
     - status: BookingStatus.CONFIRMED  ← rich enum
```

## Error Handling

| Error Case | Response |
|------------|----------|
| Booking không tồn tại | `404 Not Found` với `BookingNotFoundException` |
| Seat đã được đặt | `409 Conflict` với `SeatAlreadyTakenException` |
| Route không tồn tại | `404 Not Found` với `RouteNotFoundException` |
| Confirm booking đang PENDING | Không cho confirm → `422 Unprocessable Entity` |
| Duplicate idempotency key | `200 OK` trả về booking hiện có |

## Boundary Definitions

- **Spring Modulith module boundary**: `booking/` package là một module. `package-info.java` khai báo những gì public. Packages `booking/infrastructure/` là internal (không accessible từ outside).
- **Shared Kernel boundary**: `shared/domain/` và `shared/infrastructure/` accessible từ tất cả modules.
- **Database boundary**: Domain model không biết về database. Chỉ infrastructure/persistence/ interact với JPA.

## Test Strategy

| Layer | Test Type | Tool | Focus |
|-------|-----------|------|-------|
| Domain model | Unit test | JUnit 5 (no Spring) | Business rules, domain events, invariants |
| Use cases | Unit test | JUnit 5 + Mockito | Orchestration logic, mocked repositories |
| Repository adapter | Integration test | `@DataJpaTest` | Mapping correctness, query accuracy |
| REST controller | Integration test | `@WebMvcTest` | HTTP contracts, request/response mapping |
| Module boundaries | Modulith test | `@ApplicationModuleTest` | Spring Modulith boundary verification |
| Full slice | Integration test | `@SpringBootTest` | End-to-end happy path |

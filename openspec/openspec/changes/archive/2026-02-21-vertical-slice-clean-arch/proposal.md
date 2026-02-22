# Why

Backend hiện tại chưa có feature nào được implement, nhưng đã có DDD foundation tốt (`AggregateRoot<ID>`, `Money`, `ValueObject`). Nếu không thiết lập kiến trúc rõ ràng ngay từ đầu, team sẽ có xu hướng mix ORM annotations vào domain model (anemic domain model), tạo ra coupling giữa persistence layer và business logic, và tổ chức code theo layer ngang (controller/service/repository) thay vì theo feature. Cần thiết lập Vertical Slice Architecture + Clean Architecture ngay bây giờ, trước khi bất kỳ feature nào được implement, để tạo ra chuẩn mực kiến trúc mà toàn team tuân theo.

## What Changes

- **Thiết lập package structure** theo Vertical Slice: mỗi bounded context (booking, payment, train, user) là một top-level package dưới `backend/`
- **Định nghĩa layer convention** trong mỗi slice: `domain/` → `application/` → `infrastructure/persistence/` + `infrastructure/web/`
- **Tách biệt ORM Entity khỏi Domain Model**: `@Entity` classes chỉ tồn tại trong `infrastructure/persistence/`, domain models là pure Java/Kotlin không có framework annotations
- **Thiết lập Repository Pattern**: domain định nghĩa interface (port), infrastructure cung cấp implementation (adapter) với JPA
- **Thiết lập Mapper Convention**: `EntityMapper` (Entity ↔ Domain) trong persistence package, `DtoMapper` (DTO ↔ Domain Command) trong web package
- **Tận dụng Spring Modulith**: module boundaries qua package structure, inter-module communication qua domain events
- **Implement Booking slice làm reference**: tạo full vertical slice cho `booking/create-booking` và `booking/get-booking` làm template mẫu

## Capabilities

### New Capabilities

- `backend-vertical-slice-structure`: Định nghĩa package structure, layer conventions, dependency rules, và naming conventions cho Vertical Slice Architecture với Clean Architecture trong backend
- `backend-orm-domain-separation`: Patterns và conventions cho việc tách ORM entity khỏi domain model, bao gồm Repository port/adapter pattern và mapper conventions
- `backend-booking-slice`: Reference implementation của booking bounded context với full vertical slice (domain model, use cases, JPA entity, repository adapter, REST controller, mappers)

### Modified Capabilities

- `backend-api`: Implementation structure của backend API thay đổi từ layered sang vertical slice — cùng API contracts nhưng tổ chức code khác biệt

## Impact

- **Package structure**: Toàn bộ `backend/src/main/java/io/github/phunguy65/ttbs/backend/` sẽ được tổ chức lại thành feature packages
- **New patterns**: Mọi feature implementation sau này phải follow vertical slice pattern (không thêm global controller/service/repository packages)
- **Spring Modulith**: Cần `package-info.java` cho mỗi module để khai báo public API
- **Build**: Không có dependency changes — tất cả frameworks (Spring Boot 4, Modulith 2, Hibernate 7) đã có sẵn
- **Database**: Không thay đổi schema — chỉ implement JPA entities mapping to existing tables
- **Testing**: Mỗi domain layer có thể test độc lập (unit test không cần Spring context), infrastructure layer dùng `@DataJpaTest`

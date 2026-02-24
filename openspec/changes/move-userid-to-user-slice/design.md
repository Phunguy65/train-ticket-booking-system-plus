## Context

`UserId` là một type-safe UUID wrapper đại diện cho identity của User aggregate. Hiện tại nó nằm trong `shared/domain` — module OPEN cho toàn bộ ứng dụng — dù về mặt DDD, nó thuộc về User bounded context. Module `booking` cũng cần `UserId` vì `Booking` aggregate lưu reference đến user tạo booking.

Spring Modulith 2.0.3 hỗ trợ **Named Interface** — cơ chế cho phép một module expose một package con cụ thể cho các module khác access một cách explicit và documented, thay vì phải để cả module là OPEN.

## Goals / Non-Goals

**Goals:**

-  Di chuyển `UserId.java` vào đúng module owner: `user/domain/model/`
-  Dùng `@NamedInterface("model")` để expose package `user/domain/model` cho `booking` module
-  Khai báo explicit dependency từ `booking` → `user::model` qua `allowedDependencies`
-  Cập nhật tất cả 29 import statements trên toàn codebase
-  Đảm bảo `@ApplicationModuleTest` vẫn pass cho cả `booking` và `user` modules

**Non-Goals:**

-  Không thay đổi behavior của `UserId` (logic giữ nguyên hoàn toàn)
-  Không thay đổi REST API, database schema, hay domain events
-  Không di chuyển các class khác trong `shared/domain`
-  Không thêm `@NamedInterface` cho toàn bộ `user/domain` — chỉ expose `model` package

## Decisions

### Decision 1: Đặt `UserId.java` tại `user/domain/model/` (không phải `user/domain/`)

**Rationale**: Nhất quán với pattern của `BookingId`, `SeatId`, `RouteId` — tất cả đều nằm trong `<module>/domain/model/`. Package `user/domain/model/` sẽ chứa toàn bộ value objects và aggregate root của user: `User.java`, `UserRole.java`, và `UserId.java`.

### Decision 2: Dùng `@NamedInterface` trên `package-info.java` trong `user/domain/model/`

**Rationale**: Spring Modulith kiểm soát visibility ở mức package, không phải class. Tạo named interface `"model"` cho phép `booking` access toàn bộ types trong `user/domain/model/` (hiện tại là `User`, `UserRole`, `UserId`) mà không cần expose phần còn lại của `user` module.

```
user/domain/model/package-info.java:
  @NamedInterface("model")
  package io.github.phunguy65.ttbs.backend.user.domain.model;
```

### Decision 3: Khai báo `allowedDependencies = "user::model"` trong `booking/package-info.java`

**Rationale**: Spring Modulith yêu cầu modules phải khai báo explicitly các dependencies để `@ApplicationModuleTest` validate. Cú pháp `"user::model"` chỉ rõ: booking phụ thuộc vào named interface `"model"` của module `user`, không phải toàn bộ `user` module.

```
booking/package-info.java:
  @ApplicationModule(allowedDependencies = "user::model")
  package io.github.phunguy65.ttbs.backend.booking;
```

### Decision 4: Giữ `shared` module OPEN, xóa `UserId` khỏi shared

**Rationale**: `shared` module vẫn phù hợp là OPEN vì nó chứa các cross-cutting building blocks thực sự: `AggregateRoot`, `DomainEvent`, `Result<T,E>`, `Money`, `PageResult`, `SortDirection`, `ValueObject`. `UserId` không phải cross-cutting concern — nó là identity của một specific aggregate.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `@ApplicationModuleTest` fail nếu `allowedDependencies` thiếu | High nếu bỏ sót | Chạy `BookingModuleTest` và `UserModuleTest` ngay sau khi thay đổi |
| Miss một vài import statements trong 29 files | Medium | Dùng IDE refactor hoặc grep toàn bộ codebase trước khi commit |
| Fully-qualified references (2 test files) bị bỏ sót | Low | Grep riêng cho `shared.domain.UserId` kể cả trong strings |
| Future modules (`payment`, `train`) cần `UserId` | Low (hiện chưa có code) | Họ sẽ cần thêm `"user::model"` vào `allowedDependencies` khi cần |

**Trade-off chính**: Module `booking` có explicit dependency vào `user::model`. Đây là **intentional coupling** — đúng hơn implicit coupling qua shared module. Nếu sau này `user` module cần refactor `UserId`, `booking` module sẽ phải update theo — nhưng đây là behavior đúng vì chúng genuinely phụ thuộc nhau.

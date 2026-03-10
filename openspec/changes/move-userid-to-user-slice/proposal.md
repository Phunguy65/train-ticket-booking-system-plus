# Why

`UserId` hiện đang nằm trong `shared/domain` — một OPEN module cho tất cả mọi người truy cập. Tuy nhiên, `UserId` là một value object thuộc về User bounded context, không phải cross-cutting concern thực sự. Di chuyển nó vào `user/domain/model` cải thiện cohesion của module `user` và tạo explicit, documented dependency thay vì implicit coupling qua shared module. Đồng thời, Spring Modulith Named Interface được dùng để expose `UserId` cho `booking` module một cách có kiểm soát.

## What Changes

-  **Move** `UserId.java` từ `shared/domain/` sang `user/domain/model/`
-  **Update** package declaration từ `shared.domain` sang `user.domain.model`
-  **Create** `user/domain/model/package-info.java` với `@NamedInterface("model")` để expose package cho các module khác
-  **Update** `booking/package-info.java` để khai báo `allowedDependencies = "user::model"` — explicit cross-module dependency
-  **Update** 29 import statements trong toàn bộ codebase (24 files user slice + 1 booking file + 2 test files với fully-qualified references + 2 additional files)
-  **Delete** `UserId.java` từ vị trí cũ trong `shared/domain/`

## Capabilities

### New Capabilities

-  `userid-module-ownership`: Đưa `UserId` vào đúng module owner (`user`), expose qua Spring Modulith Named Interface cho `booking` module sử dụng một cách explicit và documented

### Modified Capabilities

<!-- Không có thay đổi nào ở cấp độ requirements — đây là refactoring thuần túy, không thay đổi behavior -->

## Impact

-  **`user` module**: `user/domain/model/` nhận thêm `UserId.java`; tạo thêm `package-info.java` cho named interface
-  **`booking` module**: `booking/package-info.java` cần khai báo `allowedDependencies = "user::model"`; `Booking.java` update import
-  **`shared` module**: `UserId.java` bị xóa khỏi `shared/domain/` — shared module vẫn còn `AggregateRoot`, `DomainEvent`, `Result`, `Money`, `PageResult`, `SortDirection`, `ValueObject`
-  **Tests**: `BookingModuleTest` và `UserModuleTest` (cả hai dùng `@ApplicationModuleTest`) cần pass sau khi thay đổi; 7 test files trong user slice cần update import
-  **ArchUnit**: Không bị ảnh hưởng — rules enforce layer boundaries, không enforce module ownership của value objects
-  **No API changes**: Không có thay đổi REST API, database schema, hay event contracts

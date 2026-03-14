# Context

Codebase hiện tại dùng `*RequestMapper` component làm trung gian giữa HTTP layer
và application layer. Mỗi module có một mapper đảm nhận hai việc: convert
request → command và convert application response → HTTP response. Điều này tạo
ra hai lớp DTO gần như giống hệt nhau (`BookingResponse` vs
`BookingHttpResponse`) và một component chỉ làm việc copy field.

Refactor này áp dụng cho 5 modules: `user`, `booking`, `train`, `station`,
`payment`.

## Goals / Non-Goals

**Goals:**

- Request class tự chứa logic convert sang Command (`toCommand()` method)
- Controller dùng application `*Response` trực tiếp, không qua `*HttpResponse`
  wrapper
- Xóa toàn bộ `*RequestMapper` và `*HttpResponse` classes
- `UserResponse.role` dùng `String` thay vì `UserRole` enum để tránh Jackson
  config ở domain

**Non-Goals:**

- Không thay đổi business logic trong use cases
- Không thay đổi domain layer
- Không thay đổi persistence layer
- Không thay đổi API contract (ngoại trừ breaking change đã ghi nhận ở
  `POST /users`)

## Decisions

### 1. `toCommand()` trên Request class, không phải static factory

Request class là infra layer — được phép import application command types.
Method `toCommand()` đặt trực tiếp trên record là tự nhiên nhất vì record đã
chứa đủ data để tạo command.

Với các case cần context ngoài body (ví dụ `userId` từ `SecurityContext`),
method nhận thêm tham số: `request.toCommand(userId)`. Không dùng overloading
phức tạp.

**Thay vì**: `mapper.toCommand(request, userId)` → `request.toCommand(userId)`

### 2. Bỏ `*HttpResponse`, dùng application `*Response` trực tiếp

Hầu hết `*HttpResponse` là bản sao 1:1 của application `*Response`. Lớp trung
gian này không thêm giá trị. Jackson serialize record trực tiếp mà không cần
annotation.

Trường hợp duy nhất có type khác biệt là `UserResponse.role` (`UserRole` enum vs
`String`). Giải quyết bằng cách đổi type trong application response (xem
Decision 3).

### 3. `UserResponse.role` đổi sang `String`, convert tại use case

**Vấn đề**: `UserRole` là domain enum. Nếu giữ nguyên trong `UserResponse`,
Jackson sẽ serialize thành `"CUSTOMER"` (default behavior — đúng), nhưng tạo
coupling giữa JSON output và domain enum name.

**Quyết định**: Đổi `UserResponse.role` thành `String`. Use case gọi
`user.getRole().name()` khi tạo response. Domain enum không bị ảnh hưởng, không
cần Jackson config.

**Lý do không dùng `@JsonValue` trên enum**: Domain layer không được import
Jackson annotation — vi phạm Clean Architecture.

**Lý do không dùng global Jackson config**: Ảnh hưởng toàn bộ app, khó kiểm
soát.

### 4. Naming convention cho Request classes

Hiện tại có hai naming pattern tồn tại song song:

- `*HttpRequest` (user module, ở root `web/` folder)
- `*Request` (booking, train, station modules, ở `web/request/` subfolder)

Refactor này **không chuẩn hóa naming** — chỉ thêm `toCommand()` vào các class
hiện có, giữ nguyên tên và vị trí file.

### 5. `UserListHttpResponse` bị xóa, dùng `UserResponse` trực tiếp

`UserListHttpResponse` intentionally omit `phone` field. Sau refactor, list
endpoint sẽ expose `phone`. Đây là quyết định đã được chấp nhận (xem exploration
session).

## Risks / Trade-offs

- **[Breaking change] `POST /v1.0/users` response shape** → JSON thay đổi từ
  flat sang nested `{ "user": {...}, "temporaryPassword": "..." }`. Mitigation:
  document rõ trong changelog, update API consumers (frontend).
- **[Risk] `phone` exposed trong list endpoint** → `GET /v1.0/users` sẽ trả về
  `phone` field. Mitigation: đã được chấp nhận trong exploration.
- **[Trade-off] Request class import domain types** →
  `CreateBookingRequest.toCommand(userId)` cần import `SeatId` từ domain. Infra
  → domain dependency là hợp lệ trong Clean Architecture (chiều đúng), nhưng cần
  chú ý không để domain import infra.
- **[Risk] `UserListHttpResponse` có `UserRole` enum** → Sau khi xóa và dùng
  `UserResponse` trực tiếp, `role` field serialize thành `String` (vì
  `UserResponse.role` đã là `String`). Consistent.

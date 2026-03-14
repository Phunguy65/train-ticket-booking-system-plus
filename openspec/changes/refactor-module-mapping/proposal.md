# Why

Hiện tại mỗi module có một `*RequestMapper` component đảm nhận hai việc: (1)
convert HTTP request sang command, và (2) convert application response sang HTTP
response. Điều này tạo ra boilerplate không cần thiết và vi phạm nguyên tắc
single responsibility — Request class biết cấu trúc của mình nhưng không tự
convert được, trong khi `*HttpResponse` class chỉ là bản sao gần như giống hệt
application `*Response`. Refactor này loại bỏ cả hai lớp trung gian để code gọn
hơn và dễ trace hơn.

## What Changes

- **Request classes tự map sang Command** thông qua method `toCommand()` (hoặc
  `toCommand(context)` khi cần thêm tham số như `userId`). Controller gọi
  `request.toCommand()` thay vì `mapper.toCommand(request)`.
- **Bỏ toàn bộ `*HttpResponse` classes** ở infra layer. Controller trả về
  application `*Response` trực tiếp vào `JsendResponse`, không qua bước map
  trung gian.
- **`*RequestMapper` classes bị xóa** sau khi không còn method nào cần giữ lại.
- **`UserResponse.role` đổi type từ `UserRole` sang `String`** — use case tự gọi
  `.name()` khi tạo response, tránh Jackson phải serialize enum domain type.
- **`UserListHttpResponse` bị xóa** — list endpoint dùng `UserResponse` trực
  tiếp (chấp nhận expose `phone` field).
- **`CreateUserResult` JSON shape thay đổi** từ flat sang nested:
  `{ "user": {...}, "temporaryPassword": "..." }`. **BREAKING** với API
  consumers của endpoint `POST /users`.
- **`LoginHttpResponse` bị xóa** — `LoginResultResponse` được dùng trực tiếp.

## Capabilities

### New Capabilities

- `module-mapping-convention`: Quy ước mới cho tổ chức mapping trong mỗi module
  — Request tự convert sang Command, Controller dùng application Response trực
  tiếp.

### Modified Capabilities

<!-- Không có spec-level behavior change — đây là refactor thuần túy về cấu trúc code, không thay đổi business logic hay API contract (ngoại trừ breaking change đã ghi nhận ở trên). -->

## Impact

- **Xóa files**: tất cả `*RequestMapper.java`, tất cả `*HttpResponse.java` ở
  `infrastructure/web/`
- **Sửa files**: tất cả `*HttpRequest.java` / `*Request.java` (thêm
  `toCommand()`), tất cả use case `toDto()` methods (thêm `.name()` cho role),
  tất cả Controller (bỏ `mapper.*` calls)
- **API breaking change**: `POST /v1.0/users` response shape thay đổi
- **Modules bị ảnh hưởng**: user, booking, train, station, payment
- **Không ảnh hưởng**: domain layer, application use case logic, persistence
  layer, security layer

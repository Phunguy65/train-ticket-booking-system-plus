# ADDED Requirements

## Requirement: Request class tự convert sang Command

Mỗi HTTP Request class trong `infrastructure/web/` SHALL có method `toCommand()`
để tự convert sang Command object tương ứng. Controller SHALL gọi
`request.toCommand()` thay vì dùng mapper component.

Khi cần context ngoài request body (ví dụ `userId` từ `SecurityContext`), method
SHALL nhận thêm tham số: `request.toCommand(contextParam)`.

### Scenario: Request không cần context ngoài

- **WHEN** controller nhận HTTP request không cần thêm context (ví dụ
  `CreateRouteRequest`, `LoginHttpRequest`)
- **THEN** controller gọi `request.toCommand()` trực tiếp để lấy Command object

### Scenario: Request cần context từ SecurityContext

- **WHEN** controller nhận HTTP request cần `userId` từ authenticated principal
  (ví dụ `CreateBookingRequest`, `UpdateUserHttpRequest`)
- **THEN** controller gọi `request.toCommand(userId)` với `userId` được extract
  từ `SecurityContextHolder`

### Scenario: Không còn RequestMapper component

- **WHEN** một module đã được refactor
- **THEN** module đó SHALL NOT có `*RequestMapper` class trong
  `infrastructure/web/`

## Requirement: Controller dùng application Response trực tiếp

Controller SHALL trả về application `*Response` object trực tiếp vào
`JsendResponse`, không qua `*HttpResponse` wrapper class.

### Scenario: Endpoint trả về single resource

- **WHEN** use case trả về application Response (ví dụ `BookingResponse`,
  `RouteResponse`)
- **THEN** controller wrap trực tiếp vào `JsendResponse.success(response)` mà
  không tạo `*HttpResponse` object trung gian

### Scenario: Endpoint trả về paginated list

- **WHEN** use case trả về `PageResult<XxxResponse>`
- **THEN** controller map items thành `SliceHttpResponse<XxxResponse>` trực
  tiếp, không qua `*HttpResponse` wrapper

### Scenario: Không còn HttpResponse class

- **WHEN** một module đã được refactor
- **THEN** module đó SHALL NOT có `*HttpResponse` class trong
  `infrastructure/web/` (ngoại trừ `SliceHttpResponse` ở shared module)

## Requirement: UserResponse.role là String

`UserResponse` record SHALL có field `role` kiểu `String`, không phải `UserRole`
enum.

### Scenario: Use case tạo UserResponse

- **WHEN** use case tạo `UserResponse` từ `User` domain object
- **THEN** use case gọi `user.getRole().name()` để convert enum sang String
  trước khi set vào `UserResponse`

### Scenario: Serialize UserResponse qua HTTP

- **WHEN** controller trả về `UserResponse` trong `JsendResponse`
- **THEN** JSON output có `"role": "CUSTOMER"` hoặc `"role": "ADMIN"` (uppercase
  string, không phải enum object)

### Scenario: Domain UserRole enum không bị ảnh hưởng

- **WHEN** security layer, persistence layer, hoặc domain logic cần `UserRole`
- **THEN** các layer đó vẫn dùng `UserRole` enum trực tiếp từ domain model,
  không bị ảnh hưởng bởi thay đổi này

# Tasks

## 1. UserResponse — đổi role sang String

- [x] 1.1 Đổi `UserResponse.role` từ `UserRole` sang `String`
- [x] 1.2 Cập nhật `GetUserByIdUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`
- [x] 1.3 Cập nhật `CreateUserUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`
- [x] 1.4 Cập nhật `RegisterUserUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`
- [x] 1.5 Cập nhật `ListUsersUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`
- [x] 1.6 Cập nhật `UpdateUserUseCase.toDto()` (2 chỗ): `user.getRole()` →
      `user.getRole().name()`
- [x] 1.7 Cập nhật `LoginUserUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`
- [x] 1.8 Cập nhật `RefreshTokenUseCase.toDto()`: `user.getRole()` →
      `user.getRole().name()`

## 2. User module — Request toCommand()

- [x] 2.1 Thêm `toCommand()` vào `CreateUserHttpRequest` → trả về
      `CreateUserCommand`
- [x] 2.2 Thêm `toCommand(UUID userId)` vào `UpdateUserHttpRequest` → trả về
      `UpdateUserCommand`
- [x] 2.3 Thêm `toCommand()` vào `LoginHttpRequest` → trả về `LoginCommand`
- [x] 2.4 Thêm `toCommand()` vào `RegisterHttpRequest` → trả về
      `RegisterUserCommand`
- [x] 2.5 Thêm `toCommand()` vào `RefreshTokenHttpRequest` → trả về
      `RefreshTokenCommand`

## 3. User module — Controller dùng Response trực tiếp, xóa Mapper và HttpResponse

- [x] 3.1 Cập nhật `UserController`: bỏ `mapper.*` calls, dùng application
      response trực tiếp, dùng `request.toCommand()`
- [x] 3.2 Cập nhật `AuthController`: bỏ `mapper.*` calls, dùng application
      response trực tiếp, dùng `request.toCommand()`
- [x] 3.3 Xóa `UserRequestMapper.java`
- [x] 3.4 Xóa `AuthRequestMapper.java`
- [x] 3.5 Xóa `UserHttpResponse.java`
- [x] 3.6 Xóa `UserListHttpResponse.java`
- [x] 3.7 Xóa `CreateUserHttpResponse.java`
- [x] 3.8 Xóa `LoginHttpResponse.java`

## 4. Booking module — Request toCommand() + Controller + xóa Mapper/HttpResponse

- [x] 4.1 Thêm `toCommand(UUID userId)` vào `CreateBookingRequest` → trả về
      `CreateBookingCommand` (cần import `SeatId`)
- [x] 4.2 Cập nhật `BookingController`: bỏ `mapper.*` calls, dùng
      `request.toCommand(userId)` và application response trực tiếp
- [x] 4.3 Xóa `BookingRequestMapper.java`
- [x] 4.4 Xóa `BookingHttpResponse.java`

## 5. Station module — Request toCommand() + Controller + xóa Mapper/HttpResponse

- [x] 5.1 Thêm `toCommand()` vào `CreateStationRequest` → trả về
      `CreateStationCommand`
- [x] 5.2 Thêm `toCommand(UUID id)` vào `PatchStationRequest` → trả về
      `UpdateStationCommand`
- [x] 5.3 Cập nhật `StationController`: bỏ `mapper.*` calls, dùng
      `request.toCommand()` và application response trực tiếp
- [x] 5.4 Xóa `StationRequestMapper.java`
- [x] 5.5 Xóa `StationHttpResponse.java`

## 6. Train module — Request toCommand() + Controllers + xóa Mapper/HttpResponse

- [x] 6.1 Thêm `toCommand()` vào `CreateTrainRequest` → trả về
      `CreateTrainCommand`
- [x] 6.2 Thêm `toCommand(UUID id)` vào `PatchTrainRequest` → trả về
      `UpdateTrainCommand`
- [x] 6.3 Thêm `toCommand()` vào `CreateRouteRequest` → trả về
      `CreateRouteCommand` (cần `Money.vnd()`)
- [x] 6.4 Thêm `toCommand(UUID id)` vào `PatchRouteRequest` → trả về
      `UpdateRouteCommand` (cần `Money.vnd()` + `JsonNullable`)
- [x] 6.5 Thêm `toCommand()` vào `CreateCoachRequest` → trả về
      `CreateCoachCommand`
- [x] 6.6 Thêm `toCommand()` vào `CreateSeatRequest` → trả về
      `CreateSeatCommand`
- [x] 6.7 Cập nhật `TrainController`: bỏ `mapper.*` calls, dùng
      `request.toCommand()` và application response trực tiếp
- [x] 6.8 Cập nhật `RouteController`: bỏ `mapper.*` calls, dùng
      `request.toCommand()` và application response trực tiếp
- [x] 6.9 Cập nhật `CoachController`: bỏ `mapper.*` calls, dùng
      `request.toCommand()` và application response trực tiếp
- [x] 6.10 Cập nhật `SeatController`: bỏ `mapper.*` calls, dùng
      `request.toCommand()` và application response trực tiếp
- [x] 6.11 Xóa `TrainRequestMapper.java`
- [x] 6.12 Xóa `RouteRequestMapper.java`
- [x] 6.13 Xóa `CoachRequestMapper.java`
- [x] 6.14 Xóa `SeatRequestMapper.java`
- [x] 6.15 Xóa `TrainHttpResponse.java`
- [x] 6.16 Xóa `RouteHttpResponse.java`
- [x] 6.17 Xóa `CoachHttpResponse.java`
- [x] 6.18 Xóa `SeatHttpResponse.java`

## 7. Payment module — Controller dùng Response trực tiếp, xóa HttpResponse

- [x] 7.1 Cập nhật `PaymentController`: bỏ `toResponse()` helper method, dùng
      `PaymentResponse` trực tiếp
- [x] 7.2 Xóa `PaymentHttpResponse.java`

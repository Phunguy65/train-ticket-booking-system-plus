# Why

Backend hiện tại chỉ có các endpoint trả về single item (findById), không có list endpoint nào. Khi cần hiển thị danh sách users (admin), danh sách bookings, routes... thì không có API để gọi. Cần thêm pagination và `Slice<T>` support để các list endpoint hoạt động hiệu quả mà không cần COUNT query tốn kém.

## What Changes

- **Thêm domain abstraction `PageResult<T>`** vào `shared/domain/` – pure Java record, không phụ thuộc Spring, dùng cho tất cả list queries
- **Thêm `ListUsersUseCase`** trong `user/application/usecase/` – trả về `PageResult<UserDto>` với offset-based Slice (không có COUNT)
- **Mở rộng `UserRepository`** (domain port) với method `findAll(int page, int size)` trả về `PageResult<User>`
- **Mở rộng `UserRepositoryAdapter`** (infra) – dùng Spring Data's `Slice<T>` và map sang `PageResult<T>`
- **Thêm `GET /api/v1/users`** endpoint trong `UserController` với query params `page`, `size`, `sort`
- **Thêm `SliceHttpResponse<T>`** wrapper trong `shared/infrastructure/web/` – JSON response envelope chuẩn cho tất cả slice/paginated responses
- **Thêm TypeScript types** trong Next.js admin frontend để consume Slice response

## Capabilities

### New Capabilities

- `backend-pagination-slice`: Domain abstraction `PageResult<T>` và pattern cho list queries dùng Spring Data `Slice<T>` – không có COUNT query, có `hasNext`, `hasPrevious`, metadata trang

### Modified Capabilities

- `user-get`: Thêm list endpoint `GET /api/v1/users` vào capability lấy thông tin user (hiện tại chỉ có get-by-id)

## Impact

- **Backend – shared**: Thêm `PageResult<T>` record vào `shared/domain/`; thêm `SliceHttpResponse<T>` vào `shared/infrastructure/web/`
- **Backend – user module**: Mở rộng `UserRepository` port, `UserJpaRepository`, `UserRepositoryAdapter`, `UserController`; thêm `ListUsersUseCase`
- **API contract**: Thêm endpoint mới `GET /api/v1/users?page=0&size=20&sort=createdAt,desc` – không breaking
- **Frontend – admin**: Cần định nghĩa TypeScript types cho Slice response; trang users list sẽ consume API này
- **Không ảnh hưởng**: booking module, auth module, các module khác

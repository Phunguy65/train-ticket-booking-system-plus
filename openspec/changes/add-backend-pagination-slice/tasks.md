## 1. Shared Domain – PageResult abstraction

- [x] 1.1 Tạo `SortDirection.java` enum (`ASC`, `DESC`) trong `shared/domain/` – pure Java, zero dependencies
- [x] 1.2 Tạo `PageResult<T>` record trong `shared/domain/` với fields: `items`, `pageNumber`, `pageSize`, `hasNext`, `hasPrevious`; thêm static factory `fromSlice(Slice<X>, Function<X,T>)` tại infra layer (không import Slice ở đây – factory method sẽ ở adapter)
- [x] 1.3 Viết unit test `PageResultTest` (JUnit 5, no Spring): kiểm tra `hasNext`, `hasPrevious`, empty case, mapping

## 2. Shared Infrastructure – SliceHttpResponse envelope

- [x] 2.1 Tạo `SliceHttpResponse<T>` record trong `shared/infrastructure/web/` với fields: `content`, `page`, `size`, `hasNext`, `hasPrevious`

## 3. User Domain – Repository port extension

- [x] 3.1 Thêm method `PageResult<User> findAll(int page, int size, String sortField, SortDirection direction)` vào `UserRepository` interface (domain port)

## 4. User Infrastructure – Persistence adapter

- [x] 4.1 Thêm `findAll(Pageable pageable): Slice<UserEntity>` vào `UserJpaRepository` (kế thừa từ `JpaRepository` – không cần thêm code, chỉ verify method tồn tại)
- [x] 4.2 Implement `findAll(int page, int size, String sortField, SortDirection direction)` trong `UserRepositoryAdapter`: tạo `PageRequest.of(page, size, Sort.by(...))`, gọi `userJpaRepository.findAll(Pageable)`, map `Slice<UserEntity>` → `PageResult<User>` dùng `UserEntityMapper`
- [x] 4.3 Viết `@DataJpaTest` cho `UserRepositoryAdapter`: test trang đầu, trang giữa, `hasNext=true/false`, sort, kết quả rỗng

## 5. User Application – ListUsersUseCase

- [x] 5.1 Tạo `ListUsersUseCase.java` trong `user/application/usecase/` – `@Service`, `@Transactional(readOnly = true)`, nhận `(int page, int size, String sortField, SortDirection direction)`, trả về `PageResult<UserDto>`
- [x] 5.2 Viết `@ExtendWith(MockitoExtension)` unit test cho `ListUsersUseCase`: mock `UserRepository`, kiểm tra mapping `PageResult<User>` → `PageResult<UserDto>`, trường hợp rỗng

## 6. User Infrastructure – Web layer

- [x] 6.1 Tạo `UserListHttpResponse` record trong `user/infrastructure/web/` với fields: `id`, `email`, `fullName`, `role`, `createdAt` (không có `phone`, `passwordHash`)
- [x] 6.2 Thêm `GET /api/v1/users` endpoint vào `UserController`:
  - `@RequestParam(defaultValue = "0") int page`
  - `@RequestParam(defaultValue = "20") int size` (validate 1–100)
  - `@RequestParam(defaultValue = "createdAt,desc") String sort`
  - Whitelist sort fields: `createdAt`, `email`, `fullName`, `role`
  - Trả về `JsendResponse<SliceHttpResponse<UserListHttpResponse>>`
- [x] 6.3 Cấu hình Spring Security: `GET /api/v1/users` yêu cầu `ROLE_ADMIN` trong `SecurityConfig`
- [x] 6.4 Viết `@WebMvcTest` cho `UserController` (GET /api/v1/users): test 200 với admin JWT, 403 với user JWT, 401 không có JWT, 400 khi page=-1, 400 khi size=0, 400 khi sort field không hợp lệ, kiểm tra JSON structure

## 7. Integration test

- [x] 7.1 Cập nhật `UserModuleTest` (`@ApplicationModuleTest`): thêm test `GET /api/v1/users` end-to-end (controller → use case → adapter → DB)

## 8. Frontend – TypeScript types

- [x] 8.1 Tạo `frontend/admin/src/types/api.ts` với types: `SliceResponse<T>`, `UserListItem` (`id`, `email`, `fullName`, `role`, `createdAt`), `JsendSuccess<T>`
- [x] 8.2 Tạo `frontend/admin/src/lib/api.ts` với hàm `fetchUsers(page, size, sort)` dùng Next.js `fetch` – trả về `JsendSuccess<SliceResponse<UserListItem>>`

## Context

Backend hiện tại (Spring Boot 4 + Vertical Slice + DDD) chỉ có các repository method trả về single-item (`findById`, `findByEmail`). Chưa có list endpoint nào, chưa có `Pageable` hay `Page/Slice` support. Mục tiêu là thêm `GET /api/v1/users` với Slice-based pagination (không COUNT query) cho admin frontend (Next.js 16 + React 19, Tailwind, chưa có data-fetching library).

Domain layer được enforce rất strict (9.5/10): **zero Spring/JPA dependency** trong `domain/` package – được verify bằng ArchUnit. Không thể dùng Spring's `Pageable` hay `Slice<T>` trực tiếp trong domain port.

## Goals / Non-Goals

**Goals:**

- Thêm `PageResult<T>` – pure Java domain abstraction cho paginated/sliced results, không có COUNT
- Mở rộng `UserRepository` domain port với list method dùng `PageResult<T>`
- Thêm `ListUsersUseCase` trả về `PageResult<UserDto>`
- Thêm `SliceHttpResponse<T>` shared web wrapper (JSON envelope) cho tất cả slice responses
- Implement `GET /api/v1/users?page=0&size=20&sort=createdAt,desc` trả về Slice (admin only, ROLE_ADMIN)
- Định nghĩa TypeScript types trong Next.js admin để consume response

**Non-Goals:**

- Pagination cho booking, route, station (sẽ là separate change)
- Cursor-based / keyset pagination (offset-based Slice đủ cho scope này)
- `Page<T>` với COUNT query (không cần total count cho UX "load more")
- Full CRUD admin UI (chỉ API + types)

## Decisions

### 1. `PageResult<T>` thay vì dùng Spring `Slice<T>` trong domain
**Quyết định:** Tạo `shared/domain/PageResult.java` – pure Java record.  
**Lý do:** Domain layer phải zero-framework theo ArchUnit rules. Mọi domain type khác (`Result<T,E>`, `UserId`, `Money`) đều pure Java. `PageResult<T>` giữ nhất quán với pattern hiện tại. Infra adapter map `Slice<Entity>` → `PageResult<DomainModel>`, use case map `PageResult<DomainModel>` → `PageResult<Dto>`.

### 2. `Slice<T>` (không phải `Page<T>`) tại infrastructure layer
**Quyết định:** `UserJpaRepository` trả về `Slice<UserEntity>`, KHÔNG dùng `Page<UserEntity>`.  
**Lý do:** Tránh COUNT(*) query tốn kém. Admin user list phù hợp với UX "load more" / "next page". `Slice` request size+1 rows để detect `hasNext`. `PageResult.hasNext` map trực tiếp từ `Slice.hasNext()`.

### 3. Không expose `totalElements` / `totalPages`
**Quyết định:** `PageResult<T>` chỉ có: `items`, `pageNumber`, `pageSize`, `hasNext`, `hasPrevious`.  
**Lý do:** Slice không có total count. Frontend sẽ dùng `hasNext` để render "Load More" button hoặc disable next-page navigation.

### 4. Sort validation tại web layer
**Quyết định:** Sort field whitelist trong `UserController`, không pass raw string xuống domain.  
**Lý do:** Tránh arbitrary field sort (SQL injection risk, leak internal field names). Allowed: `createdAt`, `email`, `fullName`, `role`.

### 5. `SliceHttpResponse<T>` là generic shared wrapper
**Quyết định:** Tạo `shared/infrastructure/web/SliceHttpResponse.java` record generic – dùng được cho tất cả modules sau này.  
**Lý do:** Giữ response format nhất quán cho tất cả list endpoints. Wrap vào `JsendResponse.success(sliceResponse)`.

## Architecture

```
GET /api/v1/users?page=0&size=20&sort=createdAt,desc
         │
         ▼ [web layer]
   UserController
   - validates @RequestParam (page ≥ 0, size 1-100, sort whitelist)
   - builds Sort object
   - calls ListUsersUseCase
         │
         ▼ [application layer]
   ListUsersUseCase
   - @Transactional(readOnly = true)
   - calls UserRepository.findAll(page, size, sortField, sortDir)
   - maps PageResult<User> → PageResult<UserDto>
         │
         ▼ [domain port]
   UserRepository (interface)
   + PageResult<User> findAll(int page, int size, String sortField, SortDirection dir)
         │
         ▼ [infra adapter]
   UserRepositoryAdapter
   - builds PageRequest.of(page, size, Sort.by(...))
   - calls UserJpaRepository.findAll(Pageable) → Slice<UserEntity>
   - maps Slice<UserEntity> → PageResult<User>
         │
         ▼ [Spring Data JPA]
   UserJpaRepository extends JpaRepository<UserEntity, UUID>
   (inherits findAll(Pageable): Slice<UserEntity>)
         │
         ▼
   PostgreSQL: SELECT ... FROM users ORDER BY ... LIMIT size+1
```

## Components

| Component | Responsibility | Location |
|-----------|---------------|----------|
| `PageResult<T>` | Domain abstraction cho slice kết quả, không có total count | `shared/domain/PageResult.java` |
| `SortDirection` | Enum ASC/DESC cho domain sort contract | `shared/domain/SortDirection.java` |
| `SliceHttpResponse<T>` | JSON response envelope: `{content, page, size, hasNext, hasPrevious}` | `shared/infrastructure/web/SliceHttpResponse.java` |
| `UserRepository` (update) | Thêm `findAll(page, size, sortField, dir)` method | `user/domain/repository/UserRepository.java` |
| `ListUsersUseCase` | Query use case: list users với pagination | `user/application/usecase/ListUsersUseCase.java` |
| `UserJpaRepository` (update) | Kế thừa `findAll(Pageable): Slice<UserEntity>` từ `JpaRepository` | `user/infrastructure/persistence/UserJpaRepository.java` |
| `UserRepositoryAdapter` (update) | Implement `findAll(...)`: `Slice<UserEntity>` → `PageResult<User>` | `user/infrastructure/persistence/UserRepositoryAdapter.java` |
| `UserController` (update) | Thêm `GET /api/v1/users` endpoint với sort whitelist | `user/infrastructure/web/UserController.java` |
| `UserListHttpResponse` | HTTP response DTO cho từng user item trong list | `user/infrastructure/web/UserListHttpResponse.java` |
| `SliceResponse<T>` (TS type) | TypeScript type cho Next.js admin | `frontend/admin/src/types/api.ts` |

## Data Models

### `PageResult<T>` (shared/domain)
```
record PageResult<T>(
    List<T> items,          // actual content
    int     pageNumber,     // 0-indexed current page
    int     pageSize,       // requested page size
    boolean hasNext,        // true if more pages exist
    boolean hasPrevious     // true if pageNumber > 0
)
```
Factory: `PageResult.fromSlice(Slice<X> slice, Function<X,T> mapper)`

### `SortDirection` (shared/domain)
```
enum SortDirection { ASC, DESC }
```

### `SliceHttpResponse<T>` (shared/infra/web)
```
record SliceHttpResponse<T>(
    List<T>  content,
    int      page,
    int      size,
    boolean  hasNext,
    boolean  hasPrevious
)
```

### JSON Response
```json
{
  "status": "success",
  "data": {
    "content": [
      { "id": "...", "email": "...", "fullName": "...", "role": "ADMIN", "createdAt": "..." }
    ],
    "page": 0,
    "size": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Key Flows

### List Users Flow

```
Admin Browser          UserController      ListUsersUseCase     UserRepositoryAdapter     DB
      │                      │                    │                     │                  │
      │ GET /api/v1/users     │                    │                     │                  │
      │ ?page=0&size=20       │                    │                     │                  │
      │ &sort=createdAt,desc  │                    │                     │                  │
      │──────────────────────▶│                    │                     │                  │
      │                       │ validate params    │                     │                  │
      │                       │ (page≥0, 1≤size≤100, sort whitelist)    │                  │
      │                       │                    │                     │                  │
      │                       │ execute(page, size, CREATED_AT, DESC)   │                  │
      │                       │───────────────────▶│                     │                  │
      │                       │                    │ findAll(0, 20, ...)  │                  │
      │                       │                    │────────────────────▶│                  │
      │                       │                    │                     │ PageRequest.of() │
      │                       │                    │                     │──────────────────▶
      │                       │                    │                     │ SELECT ... LIMIT 21
      │                       │                    │                     │◀──────────────────
      │                       │                    │                     │ Slice<UserEntity> │
      │                       │                    │ PageResult<User>    │                  │
      │                       │                    │◀────────────────────│                  │
      │                       │ PageResult<UserDto>│                     │                  │
      │                       │◀───────────────────│                     │                  │
      │                       │ SliceHttpResponse  │                     │                  │
      │                       │ + JsendResponse    │                     │                  │
      │ 200 OK { success, data: { content, page, size, hasNext } }       │                  │
      │◀──────────────────────│                    │                     │                  │
```

## Error Handling

| Điều kiện | HTTP Status | JSend | Message |
|-----------|-------------|-------|---------|
| `page < 0` | 400 | `fail` | `page must be >= 0` |
| `size < 1 \|\| size > 100` | 400 | `fail` | `size must be between 1 and 100` |
| Sort field không hợp lệ | 400 | `fail` | `sort field not allowed: <field>` |
| User không có ROLE_ADMIN | 403 | `fail` | Spring Security tự xử lý |
| Server error | 500 | `error` | GlobalExceptionHandler |

## Boundary Definitions

- **Spring Security**: `GET /api/v1/users` yêu cầu `ROLE_ADMIN` – configure trong `SecurityConfig`
- **Spring Data JPA**: `JpaRepository.findAll(Pageable)` trả về `Slice<T>` khi dùng `PageRequest` – standard API, không cần custom query
- **Next.js Admin**: consume `SliceHttpResponse` JSON qua `fetch` API của Next.js App Router (Server Component hoặc Client Component)

## Test Strategy

| Layer | Test Type | Class | Coverage |
|-------|-----------|-------|---------|
| Domain | Unit (JUnit 5, no Spring) | `PageResultTest` | `fromSlice` mapping, `hasNext`/`hasPrevious` logic |
| Use Case | Unit (Mockito) | `ListUsersUseCaseTest` | Happy path, empty result, boundary page |
| Repository Adapter | Integration (`@DataJpaTest`) | `UserRepositoryAdapterTest` | `findAll` với page/size/sort, `hasNext` detection, empty DB |
| Controller | Integration (`@WebMvcTest`) | `UserControllerTest` | Valid params, invalid params (400), unauthorized (403), success response structure |
| Module | Integration (`@ApplicationModuleTest`) | `UserModuleTest` | End-to-end slice từ controller đến DB |

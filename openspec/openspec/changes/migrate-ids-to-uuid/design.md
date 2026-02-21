## Context

Hệ thống train ticket booking đang dùng `BIGSERIAL` cho tất cả primary keys và `BIGINT` cho tất cả foreign keys trên 7 bảng: `users`, `stations`, `trains`, `routes`, `seats`, `bookings`, `transactions`. Dự án đang ở giai đoạn dev, chưa có data thật, chưa có backend Java entities được implement.

PostgreSQL version: **17** (hỗ trợ `uuidv7()` built-in).

## Goals / Non-Goals

**Goals:**

- Migrate toàn bộ primary keys và foreign keys sang `UUID` bằng một Flyway migration script
- Dùng `uuidv7()` làm default value để đảm bảo monotonic ordering cho B-tree index
- Cập nhật OpenAPI contract phản ánh UUID format
- Thiết lập pattern chuẩn cho Java entity layer (dùng khi implement)

**Non-Goals:**

- Backward compatibility với numeric IDs (không có external consumers)
- Zero-downtime migration (dev environment, không có data)
- Migration data cũ (database trống)

## Decisions

### D1: Dùng `uuidv7()` thay vì `gen_random_uuid()` (UUID v4)

**Lý do**: UUID v7 có monotonic ordering theo thời gian → B-tree index locality tốt hơn → ít page splits hơn khi insert → performance gần với BIGSERIAL. UUID v4 hoàn toàn random gây index fragmentation cao. PostgreSQL 17 hỗ trợ `uuidv7()` built-in, không cần extension.

### D2: Migration sạch (drop-and-recreate) thay vì add-column strategy

**Lý do**: Database đang trống ở dev environment. Add-column strategy (add UUID → populate → swap) chỉ cần thiết khi có data production cần preserve. Migration sạch đơn giản hơn nhiều và không có risk data loss.

**Approach**:
1. Drop tất cả FK constraints và indexes liên quan
2. Alter column types (BIGSERIAL/BIGINT → UUID)
3. Set default `uuidv7()` cho PK columns
4. Recreate FK constraints và indexes
5. Thực hiện theo thứ tự dependency: `users/stations/trains` → `routes/seats` → `bookings` → `transactions`

### D3: Java entity pattern — `@GeneratedValue(strategy = GenerationType.UUID)` với `java.util.UUID`

**Lý do**: Spring Boot 3.x + Hibernate 6.2+ hỗ trợ `GenerationType.UUID` native, không cần legacy `@GenericGenerator`. Dùng `java.util.UUID` (không phải `String`) để có type safety và tránh parsing overhead. Hibernate tự động handle PostgreSQL UUID ↔ Java UUID conversion.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(columnDefinition = "uuid", updatable = false)
private UUID id;
```

### D4: OpenAPI — `type: string, format: uuid` cho tất cả ID fields

**Lý do**: OpenAPI spec không có native `uuid` type. Convention chuẩn là `type: string, format: uuid`. Điều này cho phép code generators (openapi-generator) tạo ra đúng type (`UUID` trong Java, `string` trong TypeScript).

### D5: Frontend dùng `string` type cho IDs

**Lý do**: TypeScript không có UUID type native. Dùng `string` (không phải `number`) là đúng và đơn giản. Kotlin Compose Multiplatform dùng `String` hoặc `java.util.UUID` tùy target platform.

## Risks / Trade-offs

| Risk | Mức độ | Mitigation |
|------|--------|------------|
| UUID index size 2x lớn hơn BIGINT | Thấp | UUID v7 bù đắp phần lớn bằng locality; chấp nhận ~5-10% trade-off |
| Developer quên dùng UUID type trong entity mới | Trung bình | Document rõ pattern trong tasks; review checklist |
| `uuidv7()` không available nếu PostgreSQL downgrade | Thấp | PG 17 đang dùng; document requirement |
| OpenAPI code generators cần re-run sau khi update contract | Thấp | Chạy lại generator là bước tường minh trong tasks |

### Thứ tự migration (FK dependency order)

```
Round 1 (independent tables):
  users ──────────────────────────────────────────────┐
  stations ───────────────────────────────────────────┤ → Migrate first
  trains ─────────────────────────────────────────────┘

Round 2 (depends on Round 1):
  routes (FK: trains, stations×2) ────────────────────┐
  seats  (FK: trains) ────────────────────────────────┘ → Migrate second

Round 3 (depends on Round 2):
  bookings (FK: users, routes, seats) ────────────────── → Migrate third

Round 4 (depends on Round 3):
  transactions (FK: bookings) ────────────────────────── → Migrate last
```

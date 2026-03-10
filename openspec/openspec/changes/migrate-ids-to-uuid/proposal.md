# Why

Hệ thống hiện đang dùng `BIGSERIAL` cho tất cả primary keys và `BIGINT` cho foreign keys. UUID cho phép generate ID an toàn ở tầng application (không cần round-trip DB), hỗ trợ kiến trúc phân tán trong tương lai, và loại bỏ rủi ro enumeration attack trên các public API endpoint. Dự án đang ở giai đoạn dev, chưa có data thật — đây là thời điểm tốt nhất để thực hiện thay đổi này với chi phí thấp nhất.

## What Changes

- **BREAKING** Tất cả primary key columns (`id`) trên 7 bảng đổi từ `BIGSERIAL` sang `UUID`
- **BREAKING** Tất cả foreign key columns (`*_id`) đổi từ `BIGINT` sang `UUID`
- Thêm Flyway migration mới `V2.0.0__migrate_ids_to_uuid.sql` thực hiện toàn bộ schema change
- Cập nhật OpenAPI contract: tất cả ID fields đổi từ `type: integer, format: int64` sang `type: string, format: uuid`
- Tất cả Java entities (khi implement) sẽ dùng `UUID` type thay vì `Long`
- Default value cho UUID dùng `uuidv7()` (PostgreSQL 17 built-in) để đảm bảo monotonic ordering tốt cho B-tree index

## Capabilities

### New Capabilities

- `uuid-id-schema`: Định nghĩa chuẩn UUID cho tất cả primary key và foreign key trong database schema, OpenAPI contract, và Java entity layer

### Modified Capabilities

<!-- Không có spec nào tồn tại trước đây -->

## Impact

- **Database**: 3 migration files hiện có là reference; thêm `V2.0.0__migrate_ids_to_uuid.sql` cho 7 bảng (`users`, `stations`, `trains`, `routes`, `seats`, `bookings`, `transactions`) và tất cả indexes liên quan
- **OpenAPI**: `shared/api-contracts/openapi.yaml` — tất cả `id` fields, FK fields, path parameters cần đổi type sang `string, format: uuid`
- **Backend Java**: Khi implement entities/repositories/services/controllers, tất cả phải dùng `java.util.UUID` thay vì `Long`; `JpaRepository<Entity, UUID>`; `@GeneratedValue(strategy = GenerationType.UUID)`
- **Frontend (Next.js Admin)**: Dùng `string` type cho ID fields trong TypeScript models thay vì `number`
- **Frontend (Kotlin CMP App)**: Dùng `String` hoặc `java.util.UUID` cho ID fields trong data classes
- **No backward compatibility needed**: Hệ thống chưa có external consumers

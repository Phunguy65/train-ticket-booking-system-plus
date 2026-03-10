## ADDED Requirements

### Requirement: UUID primary keys trên tất cả bảng
Tất cả bảng trong database SHALL dùng `UUID` (PostgreSQL native `uuid` type) làm primary key, với default value là `uuidv7()` để đảm bảo monotonic ordering.

Các bảng áp dụng: `users`, `stations`, `trains`, `routes`, `seats`, `bookings`, `transactions`.

#### Scenario: PK column có kiểu uuid và default uuidv7
- **WHEN** schema được apply qua Flyway migration
- **THEN** mỗi bảng có column `id` kiểu `UUID NOT NULL DEFAULT uuidv7()` làm PRIMARY KEY

#### Scenario: Insert row không cung cấp id
- **WHEN** một row được insert vào bất kỳ bảng nào mà không cung cấp giá trị `id`
- **THEN** database tự động sinh UUID v7 cho `id`

#### Scenario: UUID v7 có monotonic ordering
- **WHEN** nhiều rows được insert liên tiếp
- **THEN** các UUID được sinh ra có thứ tự tăng dần theo thời gian (có thể sort)

---

### Requirement: UUID foreign keys tham chiếu đúng kiểu
Tất cả foreign key columns SHALL dùng kiểu `UUID` (không phải `BIGINT`) và tham chiếu đúng column `id UUID` của bảng liên quan.

Các FK columns: `routes.train_id`, `routes.origin_station_id`, `routes.destination_station_id`, `seats.train_id`, `bookings.user_id`, `bookings.route_id`, `bookings.seat_id`, `transactions.booking_id`.

#### Scenario: FK constraint tham chiếu UUID column
- **WHEN** schema được apply
- **THEN** tất cả FK constraints tham chiếu `id UUID` của bảng cha, không phải `id BIGINT`

#### Scenario: Insert FK value không tồn tại trong bảng cha
- **WHEN** một row được insert với FK value không tồn tại trong bảng cha
- **THEN** database từ chối với FK violation error

---

### Requirement: Flyway migration tạo schema UUID trong một bước
Việc chuyển đổi từ BIGSERIAL sang UUID SHALL được thực hiện bằng một Flyway versioned migration duy nhất (`V2.0.0__migrate_ids_to_uuid.sql`) theo đúng thứ tự FK dependency.

#### Scenario: Migration chạy thành công trên database trống
- **WHEN** Flyway chạy `V2.0.0__migrate_ids_to_uuid.sql` trên database đã có schema từ V1.x
- **THEN** migration hoàn thành không có lỗi và tất cả bảng có UUID columns

#### Scenario: Migration thực hiện đúng thứ tự dependency
- **WHEN** migration chạy
- **THEN** các independent tables (`users`, `stations`, `trains`) được migrate trước; sau đó `routes`, `seats`; sau đó `bookings`; cuối cùng `transactions`

#### Scenario: Tất cả indexes được recreate sau migration
- **WHEN** migration hoàn thành
- **THEN** tất cả indexes (`idx_seats_train_status`, `idx_bookings_user`, `idx_routes_train`, `idx_routes_departure`, `idx_unique_active_booking`) tồn tại trên UUID columns tương ứng

---

### Requirement: OpenAPI contract dùng uuid format cho tất cả ID fields
Tất cả ID fields trong OpenAPI contract SHALL có `type: string` và `format: uuid` (không phải `type: integer, format: int64`). Áp dụng cho cả response schemas và path/query parameters.

#### Scenario: Schema ID field có đúng format
- **WHEN** OpenAPI contract được đọc
- **THEN** tất cả `id` fields và FK fields (như `userId`, `routeId`, `seatId`, `trainId`) có `type: string, format: uuid`

#### Scenario: Path parameter ID có đúng type
- **WHEN** OpenAPI contract được đọc
- **THEN** tất cả path parameters nhận ID (như `{id}` trong `/trains/{id}`, `/bookings/{id}`) có `schema: type: string, format: uuid`

---

### Requirement: Java entity dùng `java.util.UUID` với `@GeneratedValue(strategy = GenerationType.UUID)`
Tất cả JPA entity classes SHALL dùng `java.util.UUID` làm type cho `@Id` field, với annotation `@GeneratedValue(strategy = GenerationType.UUID)` (Hibernate 6.2+ / Spring Boot 3.x).

#### Scenario: Entity có UUID id field
- **WHEN** một JPA entity được định nghĩa
- **THEN** field `id` có type `java.util.UUID`, annotated với `@Id` và `@GeneratedValue(strategy = GenerationType.UUID)`

#### Scenario: Repository dùng UUID làm ID type parameter
- **WHEN** một Spring Data repository được định nghĩa
- **THEN** repository extends `JpaRepository<Entity, UUID>` hoặc `CrudRepository<Entity, UUID>`

#### Scenario: Service/Controller method dùng UUID cho ID parameters
- **WHEN** một service method hoặc controller endpoint nhận hoặc trả về ID
- **THEN** ID parameter/return type là `java.util.UUID`, không phải `Long` hoặc `String`

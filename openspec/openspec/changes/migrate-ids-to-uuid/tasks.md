## 1. Database Migration

- [x] 1.1 Tạo file `database/migrations/V2.0.0__migrate_ids_to_uuid.sql`
- [x] 1.2 Drop tất cả FK constraints liên quan đến ID columns (`routes`, `seats`, `bookings`, `transactions`)
- [x] 1.3 Drop tất cả indexes liên quan (`idx_seats_train_status`, `idx_bookings_user`, `idx_routes_train`, `idx_routes_departure`, `idx_unique_active_booking`)
- [x] 1.4 Migrate Round 1 — alter PK columns: `users.id`, `stations.id`, `trains.id` từ BIGSERIAL sang `UUID DEFAULT uuidv7() NOT NULL`
- [x] 1.5 Migrate Round 2 — alter PK + FK columns: `routes.id`, `routes.train_id`, `routes.origin_station_id`, `routes.destination_station_id`, `seats.id`, `seats.train_id`
- [x] 1.6 Migrate Round 3 — alter PK + FK columns: `bookings.id`, `bookings.user_id`, `bookings.route_id`, `bookings.seat_id`
- [x] 1.7 Migrate Round 4 — alter PK + FK columns: `transactions.id`, `transactions.booking_id`
- [x] 1.8 Recreate tất cả FK constraints với UUID references
- [x] 1.9 Recreate tất cả indexes trên UUID columns
- [ ] 1.10 Verify migration chạy thành công: `flyway migrate` không có lỗi trên database trống

## 2. OpenAPI Contract

- [ ] 2.1 Mở `shared/api-contracts/openapi.yaml`
- [ ] 2.2 Đổi tất cả ID fields trong response schemas: `type: integer, format: int64` → `type: string, format: uuid` (áp dụng cho `User.id`, `Train.id`, `Seat.id`, `Seat.trainId`, `Booking.id`, `Booking.userId`, `Booking.routeId`, `Booking.seatId`)
- [ ] 2.3 Đổi tất cả path parameters nhận ID: schema `type: integer` → `type: string, format: uuid` (áp dụng cho `GET /trains/{id}`, `GET /bookings/{id}`)
- [ ] 2.4 Đổi FK fields trong request bodies: `routeId`, `seatId` trong `POST /bookings` → `type: string, format: uuid`
- [ ] 2.5 Validate OpenAPI file hợp lệ (dùng `openapi-generator validate` hoặc Swagger Editor)

## 3. Java Entity Pattern Documentation

- [ ] 3.1 Tạo hoặc cập nhật developer guide ghi rõ UUID pattern cho entity: `@GeneratedValue(strategy = GenerationType.UUID)`, `@Column(columnDefinition = "uuid", updatable = false)`, type `java.util.UUID`
- [ ] 3.2 Ghi rõ pattern cho repository: `JpaRepository<Entity, UUID>`
- [ ] 3.3 Ghi rõ pattern cho service/controller: dùng `UUID` cho ID parameter/return type, không dùng `Long` hoặc `String`

## 4. Verification

- [ ] 4.1 Chạy `docker compose up -d db` và verify Flyway migration áp dụng thành công
- [ ] 4.2 Kiểm tra schema: `\d users`, `\d bookings`, v.v. — tất cả `id` columns phải là `uuid`
- [ ] 4.3 Kiểm tra FK constraints tồn tại đúng trên UUID columns: `\d+ bookings`
- [ ] 4.4 Kiểm tra indexes tồn tại: `\di` — tất cả indexes từ V1 phải có mặt
- [ ] 4.5 Test insert một row vào mỗi bảng không cung cấp `id` — verify UUID được tự sinh
- [ ] 4.6 Test FK violation — verify insert FK value không tồn tại bị reject

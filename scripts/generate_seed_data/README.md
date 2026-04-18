# Generate Seed Data Script

Script Python để generate dữ liệu mẫu cho database hệ thống đặt vé tàu.

## Yêu cầu

- Python 3.10+
- pip

## Cài đặt

```bash
cd scripts/generate_seed_data
pip install -r requirements.txt
```

## Sử dụng

### Cơ bản

```bash
python generate.py
```

Script sẽ tạo file `R__seed_dev_data.sql` trong thư mục migrations của backend.

### Tùy chỉnh

```bash
# Chỉ định output path
python generate.py --output ./output.sql

# Generate trips cho 60 ngày
python generate.py --days 60

# Đặt random seed (để reproducible)
python generate.py --seed 42

# Kết hợp
python generate.py -o ./output.sql -d 60 -s 42
```

## Dữ liệu được generate

| Bảng                   | Số lượng | Mô tả                        |
| ---------------------- | -------- | ---------------------------- |
| stations               | 19       | Ga tàu tuyến Bắc-Nam thực tế |
| trains                 | 10       | SE1-SE8, TN1-TN2             |
| coaches                | 42       | 3-5 toa/tàu                  |
| seats                  | ~1000    | 20-28 ghế/toa                |
| route_templates        | 12       | Các tuyến chính              |
| scheduled_trips        | ~360     | 30 ngày × 12 routes          |
| trip_seat_availability | ~100k    | seats × trips                |

## Danh sách ga tàu

Ga tàu theo tuyến Bắc-Nam thực tế:

1. Ga Hà Nội (HAN)
2. Ga Phủ Lý (PYE)
3. Ga Nam Định (NDI)
4. Ga Ninh Bình (NBH)
5. Ga Thanh Hóa (THA)
6. Ga Vinh (VIN)
7. Ga Đồng Hới (DHA)
8. Ga Đông Hà (DNG)
9. Ga Huế (HUE)
10. Ga Đà Nẵng (DAN)
11. Ga Tam Kỳ (TKY)
12. Ga Quảng Ngãi (QNG)
13. Ga Diêu Trì (DPH)
14. Ga Tuy Hòa (TUH)
15. Ga Nha Trang (NTG)
16. Ga Tháp Chàm (THP)
17. Ga Bình Thuận (BTH)
18. Ga Biên Hòa (BHO)
19. Ga Sài Gòn (SGN)

## Các tuyến chính

- Hà Nội ↔ Sài Gòn (3 chuyến/ngày)
- Hà Nội ↔ Đà Nẵng (2 chuyến/ngày)
- Đà Nẵng ↔ Sài Gòn (2 chuyến/ngày)
- Hà Nội ↔ Huế (1 chuyến/ngày)
- Hà Nội ↔ Vinh (2 chuyến/ngày)
- Nha Trang ↔ Sài Gòn (1 chuyến/ngày)

## Chạy SQL

### Cách 1: Flyway migration (khuyến nghị)

File output là Flyway repeatable migration (`R__` prefix), sẽ tự động chạy khi
khởi động backend nếu nội dung thay đổi.

```bash
# Khởi động backend sẽ tự động apply migration
./gradlew bootRun
```

### Cách 2: Chạy thủ công

```bash
# Connect vào PostgreSQL và chạy
psql -U postgres -d trainbooking -f R__seed_dev_data.sql
```

## Lưu ý

- Script sẽ **xóa toàn bộ dữ liệu** trong các bảng trước khi insert. Chỉ dùng
  cho môi trường development.
- UUID được generate bằng UUIDv7 (time-ordered) để giống production.
- Giá vé được tính dựa trên khoảng cách thực tế (~550 VND/km).

## UC-10: Xem đặt vé

### 1. Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập, UC-09: Đặt vé tàu                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Mục đích                       | Khách hàng cần theo dõi trạng thái đặt vé của mình (đang giữ, đã thanh toán, đã hủy) để biết cần thanh toán hay không, và xem lại thông tin chi tiết chuyến tàu, ghế, thanh toán liên quan đến mỗi đặt vé.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Mô tả                          | Khách hàng xem danh sách đặt vé của mình (phân trang) hoặc xem chi tiết một đặt vé cụ thể bao gồm thông tin hành khách, chuyến tàu, ghế và thanh toán.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Actor chính                    | Khách hàng (Customer)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Actor liên quan                | Không                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Dãy lệnh thực hiện bình thường | **Xem danh sách đặt vé:** <br> 1. Khách hàng gửi yêu cầu xem danh sách đặt vé với `userId`, tham số phân trang (`page`, `size`). <br> 2. Hệ thống xác thực quyền truy cập: `requestingUserId == userId` (chỉ xem đặt vé của chính mình). <br> 3. Hệ thống truy vấn danh sách đặt vé phân trang, sắp xếp theo `createdAt DESC, id DESC`. <br> 4. Hệ thống trả về `PageResponse<UserBookingResponse>`. <br><br> **Xem chi tiết đặt vé:** <br> 1. Khách hàng gửi yêu cầu xem chi tiết một đặt vé theo `bookingId`. <br> 2. Hệ thống xác thực quyền truy cập: `booking.userId == requestingUserId`. <br> 3. Hệ thống tổng hợp thông tin từ booking, scheduled trip (bao gồm đã xóa), payment, và ghế. <br> 4. Hệ thống trả về `BookingDetailResponse`. |
| Hậu điều kiện (thành công)     | Không có thay đổi trạng thái. Đây là thao tác chỉ đọc.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Hậu điều kiện (thất bại)       | Không có thay đổi trạng thái. Đây là thao tác chỉ đọc.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Xử lý ngoại lệ                 | Chưa xác thực → 401 Unauthorized. <br> Xem đặt vé của người khác (list) → 403 + `ACCESS_DENIED`. <br> Đặt vé không tồn tại (detail) → 404 + `BOOKING_NOT_FOUND`. <br> Xem đặt vé của người khác (detail) → 403 + `ACCESS_DENIED`. <br> Tham số phân trang không hợp lệ → 400 + `VALIDATION_ERROR`.                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

### 2. Lược đồ tuần tự

```plantuml
@startuml UC-10
title UC-10: Xem đặt vé

actor "Khách hàng" as Actor
participant "Hệ thống" as API

== Danh sách đặt vé (phân trang) ==

Actor -> API: GetUserBookings(userId, page, size)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else userId != requestingUserId
    API --> Actor: 403 + ACCESS_DENIED
else Tham số phân trang không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Thành công
    API -> API: Truy vấn danh sách booking theo userId\n(sort: createdAt DESC, id DESC)
    API --> Actor: 200 + PageResponse<UserBookingResponse>(\n  content[{id, userId, scheduledTripId, totalPrice,\n  currency, status, paymentDeadline, createdAt}],\n  page, size, hasNext, hasPrevious, total)
end

== Chi tiết đặt vé ==

Actor -> API: GetBookingDetail(bookingId)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Đặt vé không tồn tại
    API --> Actor: 404 + BOOKING_NOT_FOUND
else booking.userId != requestingUserId
    API --> Actor: 403 + ACCESS_DENIED
else Thành công
    API -> API: Tổng hợp booking + trip + payment + seats
    API --> Actor: 200 + BookingDetailResponse(\n  id, userId, scheduledTripId, passengerInfo,\n  totalPrice, currency, status, paymentDeadline,\n  createdAt, trip, payment, seats)
end
@enduml
```

### 3. Lược đồ hoạt động

```plantuml
@startuml UC-10-activity
title UC-10: Xem đặt vé - Activity Diagram

start

switch (Loại tra cứu?)
case (Danh sách)
  if (Đã xác thực?) then (không)
    :Trả 401 Unauthorized;
    stop
  else (có)
  endif

  if (Tham số phân trang hợp lệ?) then (không)
    :Trả 400 VALIDATION_ERROR;
    stop
  else (có)
  endif

  if (requestingUserId == userId?) then (không)
    :Trả 403 ACCESS_DENIED;
    stop
  else (có)
  endif

  :Truy vấn BookingRepository.findByUserId()\n(sort: createdAt DESC, id DESC);
  :Map BookingSummary → UserBookingResponse;
  :Trả 200 + PageResponse<UserBookingResponse>;

case (Chi tiết)
  if (Đã xác thực?) then (không)
    :Trả 401 Unauthorized;
    stop
  else (có)
  endif

  if (Booking tồn tại?) then (không)
    :Trả 404 BOOKING_NOT_FOUND;
    stop
  else (có)
  endif

  if (booking.userId == requestingUserId?) then (không)
    :Trả 403 ACCESS_DENIED;
    stop
  else (có)
  endif

  :Lấy Booking từ BookingRepository;
  :Lấy ScheduledTrip từ ScheduledTripRepository\n(findEnrichedByIdIncludingDeleted);
  :Lấy Payment từ PaymentRepository\n(findSummaryByBookingId, có thể null);
  :Lấy Seats từ RouteSeatAvailabilityRepository\n(findBookedSeatSummariesByBookingId);
  :Tổng hợp BookingDetailResponse;
  :Trả 200 + BookingDetailResponse;
endswitch

stop
@enduml
```

### 4. Lược đồ trạng thái

<!-- UC-10 là thao tác chỉ đọc, không có thay đổi trạng thái. Mục này bỏ qua. -->

_Không áp dụng — UC-10 là thao tác chỉ đọc._

### 5. Lược đồ lớp ý niệm

```plantuml
@startuml UC-10-class
title UC-10: Xem đặt vé - Conceptual Class Diagram

class "Booking" as Booking {
  - bookingId: UUID
  - userId: UUID
  - scheduledTripId: UUID
  - userInfo: BookingUserInfo
  - totalPrice: Money
  - status: BookingStatus
  - idempotencyKey: String
  - paymentDeadline: Instant
  - createdAt: Instant
}

enum "BookingStatus" as BookingStatus {
  HELD
  CONFIRMED
  CANCELLED
}

class "BookingUserInfo" as BookingUserInfo {
  - fullName: String
  - email: String
  - phone: String
  - dateOfBirth: LocalDate
  - gender: String
  - idDocumentNumber: String
  - addressLine: String
}

class "UserBookingResponse" as ListDTO {
  + id: UUID
  + userId: UUID
  + scheduledTripId: UUID
  + totalPrice: long
  + currency: String
  + status: BookingStatus
  + paymentDeadline: Instant
  + createdAt: Instant
}

class "BookingDetailResponse" as DetailDTO {
  + id: UUID
  + userId: UUID
  + scheduledTripId: UUID
  + passengerInfo: PassengerInfoResponse
  + totalPrice: long
  + currency: String
  + status: BookingStatus
  + paymentDeadline: Instant
  + createdAt: Instant
  + trip: Trip
  + payment: PaymentDetailResponse
  + seats: List<Seat>
}

class "BookingDetailResponse.Trip" as TripDTO {
  + id: UUID
  + routeTemplateId: UUID
  + trainId: UUID
  + departureTime: Instant
  + arrivalTime: Instant
  + status: String
  + createdAt: Instant
  + train: Train
  + route: Route
}

class "BookingDetailResponse.Seat" as SeatDTO {
  + seatId: UUID
  + coachId: UUID
  + coachNumber: int
  + seatNumber: String
  + status: RouteSeatAvailabilityStatus
  + priceAtBooking: Long
}

class "PaymentDetailResponse" as PaymentDTO {
  + id: UUID
  + status: PaymentStatus
  + checkoutUrl: String
  + amount: long
  + currency: String
  + stripePaymentIntentId: String
  + createdAt: Instant
}

class "PassengerInfoResponse" as PassengerDTO {
  + fullName: String
  + email: String
  + phone: String
  + dateOfBirth: LocalDate
  + gender: String
  + idDocumentNumber: String
  + addressLine: String
}

class "GetUserBookingsRequest" as ListReq {
  + page: int (min=0)
  + size: int (min=1, max=100)
}

Booking *-- BookingUserInfo
Booking --> BookingStatus
DetailDTO *-- TripDTO
DetailDTO *-- PaymentDTO
DetailDTO *-- SeatDTO
DetailDTO *-- PassengerDTO
@enduml
```

### 6. Phân rã thành phần PM

#### 6.1 Controller: `BookingController`

**Endpoint 1 — Danh sách đặt vé:**

- **Nhiệm vụ**: Nhận HTTP request xem danh sách đặt vé, lấy `requestingUserId`
  từ `Authentication`, ủy thác cho `GetUserBookingsUseCase`.
- **Endpoint**: `GET /api/v1/users/{userId}/bookings`
- **Input**: Path `userId: UUID` + Query `page: int` (default 0), `size: int`
  (default 20, max 100)
- **Output thành công**: `200` + `PageResponse<UserBookingResponse>` —
  `{ content[{id, userId, scheduledTripId, totalPrice, currency, status, paymentDeadline, createdAt}], page, size, hasNext, hasPrevious, total }`
- **Output lỗi**: `400` + `VALIDATION_ERROR` | `403` + `ACCESS_DENIED`
- **Metadata**:
  `@SuccessPayload(value = UserBookingResponse.class, kind = SuccessResponseKind.PAGE)`

**Endpoint 2 — Chi tiết đặt vé:**

- **Nhiệm vụ**: Nhận HTTP request xem chi tiết đặt vé, lấy `requestingUserId` từ
  `Authentication`, ủy thác cho `GetBookingDetailUseCase`.
- **Endpoint**: `GET /api/v1/bookings/{id}`
- **Input**: Path `id: UUID`
- **Output thành công**: `200` + `BookingDetailResponse` —
  `{ id, userId, scheduledTripId, passengerInfo, totalPrice, currency, status, paymentDeadline, createdAt, trip, payment, seats }`
- **Output lỗi**: `403` + `ACCESS_DENIED` | `404` + `BOOKING_NOT_FOUND`
- **Metadata**: `@SuccessPayload(BookingDetailResponse.class)`

#### 6.2 UseCase

**GetUserBookingsUseCase:**

- **Nhiệm vụ**: Trả danh sách đặt vé phân trang cho khách hàng, kiểm tra quyền
  sở hữu.
- **Input**: `GetUserBookingsQuery` — `{ userId, requestingUserId, page, size }`
- **Output**: `Result<PageResponse<UserBookingResponse>, BookingError>`
- **Logic**:
    1. So sánh `userId == requestingUserId` → nếu khác, trả
       `BookingError.Forbidden`
    2. Gọi
       `BookingRepository.findByUserId(userId, page, size, [desc(createdAt), desc(id)])`
    3. Map `BookingSummary` → `UserBookingResponse`
    4. Trả `PageResponse`

**GetBookingDetailUseCase:**

- **Nhiệm vụ**: Trả chi tiết đặt vé bao gồm thông tin chuyến tàu, thanh toán và
  ghế, kiểm tra quyền sở hữu.
- **Input**: `GetBookingDetailQuery` — `{ bookingId, requestingUserId }`
- **Output**: `Result<BookingDetailResponse, BookingError>`
- **Gọi đến**:
    - `BookingRepository.findById(bookingId)` — lấy booking entity
    - `ScheduledTripRepository.findEnrichedByIdIncludingDeleted(scheduledTripId)`
      — lấy thông tin chuyến tàu (bao gồm đã xóa mềm, có thể null)
    - `PaymentRepository.findSummaryByBookingId(bookingId)` — lấy thông tin
      thanh toán (có thể null nếu chưa tạo checkout session)
    - `RouteSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(bookingId)`
      — lấy danh sách ghế đã đặt (JOIN seats + coaches, loại trừ soft-deleted)
<!-- - **Lưu ý**: `trip` và `payment` có thể `null` trong response — trip null nếu
  scheduled trip bị xóa hoàn toàn, payment null nếu chưa tạo checkout session. -->

#### 6.3 Repository

**BookingRepository:**

- **Nhiệm vụ**: Truy xuất domain entity `Booking` và projection
  `BookingSummary`.
- **Phương thức liên quan đến UC**:
    - `findById(BookingId): Optional<Booking>` — lấy booking entity đầy đủ
    - `findByUserId(UserId, page, size, sort): PageResponse<BookingSummary>` —
      danh sách đặt vé phân trang
- **Table**: `bookings`

**ScheduledTripRepository:**

- **Nhiệm vụ**: Truy xuất thông tin enriched của chuyến tàu.
- **Phương thức liên quan đến UC**:
    - `findEnrichedByIdIncludingDeleted(ScheduledTripId): Optional<ScheduledTripEnrichedSummary>`
      — lấy chuyến tàu kèm thông tin train, route, stations (bao gồm
      soft-deleted trips)
- **Table**: `scheduled_trips` JOIN `route_templates`, `trains`, `stations`

**PaymentRepository:**

- **Nhiệm vụ**: Truy xuất projection thanh toán.
- **Phương thức liên quan đến UC**:
    - `findSummaryByBookingId(BookingId): Optional<PaymentSummary>` — lấy thông
      tin thanh toán theo booking
- **Table**: `payments`

**RouteSeatAvailabilityRepository:**

- **Nhiệm vụ**: Truy xuất thông tin ghế đã đặt cho booking.
- **Phương thức liên quan đến UC**:
    - `findBookedSeatSummariesByBookingId(BookingId): List<BookedSeatSummary>` —
      lấy danh sách ghế kèm coach info, loại trừ seats/coaches đã soft-delete
- **Table**: `trip_seat_availability` JOIN `seats`, `coaches`

#### 6.4 Port

Không có actor hỗ trợ bên ngoài.

#### 6.5 Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-10-internal
title UC-10: Xem đặt vé - Internal Sequence

actor "Khách hàng" as Actor
participant "BookingController" as CTL
participant "GetUserBookingsUseCase" as LIST_UC
participant "GetBookingDetailUseCase" as DETAIL_UC
participant "BookingRepository" as BOOKING_REPO
participant "ScheduledTripRepository" as TRIP_REPO
participant "PaymentRepository" as PAY_REPO
participant "RouteSeatAvailabilityRepository" as SEAT_REPO
database "DB" as DB

== Danh sách đặt vé ==

Actor -> CTL: GET /api/v1/users/{userId}/bookings?page=0&size=20
CTL -> LIST_UC: execute(GetUserBookingsQuery(userId, requestingUserId, page, size))

alt userId != requestingUserId
    LIST_UC --> CTL: Result.failure(Forbidden)
    CTL --> Actor: 403 + ACCESS_DENIED
else userId == requestingUserId
    LIST_UC -> BOOKING_REPO: findByUserId(userId, page, size, [desc(createdAt), desc(id)])
    BOOKING_REPO -> DB: SELECT * FROM bookings\nWHERE user_id = ? ORDER BY created_at DESC, id DESC\nLIMIT ? OFFSET ?
    DB --> BOOKING_REPO: Page<BookingEntity>
    BOOKING_REPO --> LIST_UC: PageResponse<BookingSummary>
    LIST_UC -> LIST_UC: map BookingSummary → UserBookingResponse
    LIST_UC --> CTL: Result.success(PageResponse<UserBookingResponse>)
    CTL --> Actor: 200 + PageResponse<UserBookingResponse>
end

== Chi tiết đặt vé ==

Actor -> CTL: GET /api/v1/bookings/{id}
CTL -> DETAIL_UC: execute(GetBookingDetailQuery(bookingId, requestingUserId))
DETAIL_UC -> BOOKING_REPO: findById(bookingId)
BOOKING_REPO -> DB: SELECT * FROM bookings WHERE id = ?
DB --> BOOKING_REPO: Optional<Booking>
BOOKING_REPO --> DETAIL_UC: Optional<Booking>

alt Booking không tồn tại
    DETAIL_UC --> CTL: Result.failure(BookingNotFound)
    CTL --> Actor: 404 + BOOKING_NOT_FOUND
else booking.userId != requestingUserId
    DETAIL_UC --> CTL: Result.failure(Forbidden)
    CTL --> Actor: 403 + ACCESS_DENIED
else Thành công
    DETAIL_UC -> TRIP_REPO: findEnrichedByIdIncludingDeleted(scheduledTripId)
    TRIP_REPO -> DB: SELECT ... FROM scheduled_trips\nJOIN route_templates JOIN trains JOIN stations\nWHERE st.id = ?
    DB --> TRIP_REPO: Optional<ScheduledTripEnrichedSummary>
    TRIP_REPO --> DETAIL_UC: Optional<ScheduledTripEnrichedSummary>

    DETAIL_UC -> PAY_REPO: findSummaryByBookingId(bookingId)
    PAY_REPO -> DB: SELECT ... FROM payments WHERE booking_id = ?
    DB --> PAY_REPO: Optional<PaymentSummary>
    PAY_REPO --> DETAIL_UC: Optional<PaymentSummary>

    DETAIL_UC -> SEAT_REPO: findBookedSeatSummariesByBookingId(bookingId)
    SEAT_REPO -> DB: SELECT s.id, c.id, c.car_number, s.seat_number,\ntsa.status, tsa.price_at_booking\nFROM trip_seat_availability tsa\nJOIN seats s ... JOIN coaches c ...\nWHERE tsa.booking_id = ?\nORDER BY c.car_number ASC, s.seat_number ASC
    DB --> SEAT_REPO: List<BookedSeatSummary>
    SEAT_REPO --> DETAIL_UC: List<BookedSeatSummary>

    DETAIL_UC -> DETAIL_UC: Tổng hợp BookingDetailResponse\n(trip/payment có thể null)
    DETAIL_UC --> CTL: Result.success(BookingDetailResponse)
    CTL --> Actor: 200 + BookingDetailResponse
end
@enduml
```

#### 6.6 Giao diện

##### 6.6.1 Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Danh sách đặt vé của tôi
  ..
  {#
    Mã đặt vé | Chuyến     | Ngày       | Trạng thái | Tổng tiền
    BK001     | SE1        | 15/04/2026 | <color:Orange>HELD      | 1,000,000đ
    BK002     | SE3        | 20/04/2026 | <color:Green>CONFIRMED | 850,000đ
    BK003     | SE5        | 10/04/2026 | <color:Gray>CANCELLED | 480,000đ
  }
  ..
  [< Trước] | Trang 1/2 | [Tiếp >]
}
@endsalt
```

```plantuml
@startsalt
{+
  <b>Chi tiết đặt vé #BK001
  ..
  {^"Thông tin đặt vé"
    Trạng thái      | <color:Orange>ĐANG GIỮ CHỖ
    Hạn thanh toán  | 15/04/2026 10:30
  }
  {^"Thông tin chuyến tàu"
    Chuyến      | SE1 - Sài Gòn → Đà Nẵng
    Khởi hành   | 06:00, 15/04/2026
    Đến         | 12:30, 15/04/2026
  }
  {^"Ghế đã đặt"
    {#
      Toa | Ghế  | Giá
      1   | 01A  | 500,000đ
      1   | 01B  | 500,000đ
    }
  }
  {^"Thanh toán"
    Trạng thái | PENDING
    Tổng tiền  | 1,000,000đ
  }
  ==
  [Hủy đặt vé] | [Thanh toán ngay]
}
@endsalt
```

##### 6.6.2 Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

### 7. Bảng tham chiếu dò vết

| Use Case | Controller        | Endpoint                              | UseCase                 | Repository                                                           | Table                                              |
| -------- | ----------------- | ------------------------------------- | ----------------------- | -------------------------------------------------------------------- | -------------------------------------------------- |
| UC-10    | BookingController | `GET /api/v1/users/{userId}/bookings` | GetUserBookingsUseCase  | BookingRepository.findByUserId()                                     | bookings                                           |
| UC-10    | BookingController | `GET /api/v1/bookings/{id}`           | GetBookingDetailUseCase | BookingRepository.findById()                                         | bookings                                           |
|          |                   |                                       |                         | ScheduledTripRepository.findEnrichedByIdIncludingDeleted()           | scheduled_trips, route_templates, trains, stations |
|          |                   |                                       |                         | PaymentRepository.findSummaryByBookingId()                           | payments                                           |
|          |                   |                                       |                         | RouteSeatAvailabilityRepository.findBookedSeatSummariesByBookingId() | trip_seat_availability, seats, coaches             |

### 8. Tiêu chí kiểm thử

| Tiêu chí              | Phép thử                                                                   | Kết quả mong đợi                                                 | Ghi chú                                                       |
| --------------------- | -------------------------------------------------------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------- |
| Toàn diện (coverage)  | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ                            | Rà soát chéo giữa mục 2 và mục 3                              |
| Nhất quán             | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–6                                 | Đặc biệt kiểm tra tên trong mục 5–6                           |
| Truy vết              | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.5)     | Mọi tương tác trong sequence đều có entry                        | Kiểm tra không thiếu endpoint/method                          |

# UC-08: Đặt vé tàu

## 1. Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập, UC-06: Tra cứu chuyến tàu, UC-07: Xem sơ đồ ghế chuyến tàu                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Mục đích                       | Khách hàng đã đăng nhập muốn giữ chỗ các ghế đã chọn để thanh toán vé tàu. PM tạo booking ở trạng thái `HELD`, khóa các ghế tương ứng trong một khoảng thời gian thanh toán, và cung cấp các thành phần payment/webhook để hỗ trợ chuyển booking sang `CONFIRMED` khi thanh toán thành công.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Mô tả                          | Khách hàng chọn chuyến tàu và ghế ngồi để đặt vé. Hệ thống tạo đặt vé ở trạng thái giữ chỗ (`HELD`) trong 15 phút. Sau khi booking được tạo, hệ thống phát sinh sự kiện `BookingCreated`. Khi thanh toán thành công qua webhook cho một payment hợp lệ, booking chuyển sang trạng thái xác nhận (`CONFIRMED`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Actor chính                    | Khách hàng đã đăng nhập                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Actor liên quan                | Stripe                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ. Khách hàng đã chọn chuyến tàu và danh sách ghế muốn đặt.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Dãy lệnh thực hiện bình thường | 1. Khách hàng gửi yêu cầu đặt vé gồm `scheduledTripId`, danh sách `seatIds`, và `idempotencyKey`. <br> 2. Hệ thống kiểm tra dữ liệu đầu vào hợp lệ. <br> 3. Hệ thống kiểm tra `idempotencyKey`; nếu booking đã được tạo trước đó thì trả về booking cũ. <br> 4. Hệ thống kiểm tra người dùng tồn tại. <br> 5. Hệ thống kiểm tra khách hàng chưa có booking đang giữ chỗ cho cùng chuyến tàu. <br> 6. Hệ thống kiểm tra chuyến tàu tồn tại, lấy `routeTemplate`, tính tổng giá vé theo số ghế. <br> 7. Hệ thống tạo booking ở trạng thái `HELD` với hạn thanh toán 15 phút và lưu vào cơ sở dữ liệu. <br> 8. Hệ thống giữ chỗ các ghế được chọn qua `RouteSeatAvailabilityManager` (trạng thái ghế `AVAILABLE -> HELD`) và lưu kèm `bookingId`, `priceAtBooking`. <br> 9. Nếu giữ chỗ thành công, hệ thống phát sinh sự kiện miền `BookingCreated` và trả về thông tin booking vừa tạo (trạng thái `HELD`). <br> 10. Codebase hiện có các thành phần payment để tạo `Payment`, xử lý webhook Stripe, expiry, và refund. <br> 11. Nếu checkout session được tạo và khách hàng thanh toán thành công, webhook Stripe sẽ xác nhận booking (`HELD -> CONFIRMED`), xác nhận ghế (`HELD -> BOOKED`), và đánh dấu payment `PAID`. |
| Hậu điều kiện (thành công)     | Ở luồng đồng bộ: một booking mới được tạo ở trạng thái `HELD`, có `paymentDeadline`, và các ghế được chọn chuyển sang `HELD`. Ở luồng bất đồng bộ sau thanh toán thành công: booking chuyển sang `CONFIRMED`, ghế chuyển sang `BOOKED`, payment chuyển sang `PAID`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Hậu điều kiện (thất bại)       | Nếu thất bại trước khi lưu booking, booking mới không được tạo. Nếu thất bại xảy ra sau khi booking đã được lưu nhưng giữ chỗ ghế thất bại, code hiện tại có thể để lại một booking ở trạng thái `HELD` đã được persist trước khi trả lỗi `SEAT_NOT_AVAILABLE`. Nếu payment thất bại hoặc session hết hạn, payment được cập nhật sang `FAILED` hoặc `CANCELLED`, booking vẫn ở `HELD` cho đến khi luồng expiry hủy booking. Nếu webhook thanh toán đến muộn sau khi booking đã `CANCELLED`, hệ thống tạo refund ngay lập tức và đánh dấu payment `REFUNDED`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Xử lý ngoại lệ                 | Chưa xác thực -> Hệ thống trả về lỗi 401. <br> Dữ liệu đầu vào không hợp lệ (thiếu trường, danh sách ghế rỗng) -> Hệ thống trả về lỗi `VALIDATION_ERROR`. <br> Người dùng không tồn tại -> Hệ thống trả về lỗi `USER_NOT_FOUND`. <br> Chuyến tàu không tồn tại hoặc không tìm thấy route template để tính giá -> Hệ thống trả về lỗi `SCHEDULED_TRIP_NOT_FOUND`. <br> Một hoặc nhiều ghế không còn trống -> Hệ thống trả về lỗi `SEAT_NOT_AVAILABLE`. <br> Đã có active hold cho cùng chuyến tàu -> Hệ thống trả về lỗi `BOOKING_CANNOT_CONFIRM`. <br> Gửi lại cùng `idempotencyKey` -> Hệ thống trả về booking đã tạo trước đó. <br> Stripe webhook bị lặp lại -> Hệ thống bỏ qua event đã xử lý. <br> Thanh toán thất bại -> payment chuyển sang `FAILED`, booking vẫn `HELD` cho đến khi hết hạn. <br> Checkout session hết hạn -> payment chuyển sang `CANCELLED`; booking sẽ được hủy bởi luồng expiry. <br> Thanh toán đến muộn sau khi booking đã `CANCELLED` -> Hệ thống refund ngay và đánh dấu payment `REFUNDED`.                                                                                                                                                                                                                                                                                                                                                                |

## 2. Lược đồ tuần tự

```plantuml
@startuml UC-08
title UC-08: Book Ticket

actor "Khách hàng" as Actor
participant "Hệ thống" as API
participant "Stripe" as Stripe

== Tạo booking ==

Actor -> API: CreateBooking(scheduledTripId, seatIds, idempotencyKey)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Dữ liệu đầu vào không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Idempotency key đã tồn tại
    API --> Actor: 201 + BookingResponse(id, userId, scheduledTripId, userInfo, totalPrice, currency, status, paymentDeadline, createdAt)
else User không tồn tại
    API --> Actor: 404 + USER_NOT_FOUND
else Đã có active hold cho cùng trip
    API --> Actor: 409 + BOOKING_CANNOT_CONFIRM
else Scheduled trip không tồn tại
    API --> Actor: 404 + SCHEDULED_TRIP_NOT_FOUND
else Ghế không khả dụng
    API --> Actor: 409 + SEAT_NOT_AVAILABLE
else Tạo booking thành công
    API -> API: Tạo booking HELD, paymentDeadline = now + 15m
    API -> API: Hold seats (AVAILABLE -> HELD)
    API -> API: Publish BookingCreated event
    API --> Actor: 201 + BookingResponse(id, userId, scheduledTripId, userInfo, totalPrice, currency, status=HELD, paymentDeadline, createdAt)
end

== Thanh toán bất đồng bộ ==

Actor -> Stripe: Complete payment (sau khi có checkout session)

alt checkout.session.completed
    Stripe -> API: Webhook(payment success)
    API -> API: Confirm booking (HELD -> CONFIRMED)
    API -> API: Confirm seats (HELD -> BOOKED)
    API -> API: Mark payment (PENDING -> PAID)
else payment_intent.payment_failed
    Stripe -> API: Webhook(payment failed)
    API -> API: Mark payment (PENDING -> FAILED)
else checkout.session.expired
    Stripe -> API: Webhook(session expired)
    API -> API: Mark payment (PENDING -> CANCELLED)
else Payment đến muộn sau khi booking đã CANCELLED
    Stripe -> API: Webhook(payment success)
    API -> Stripe: Create refund
    API -> API: Mark payment (current -> REFUNDED)
end
@enduml
```

## 3. Lược đồ hoạt động

```plantuml
@startuml UC-08-activity
title UC-08: Book Ticket - Activity Diagram

start

:Khách hàng gửi yêu cầu đặt vé;

if (Access token hợp lệ?) then (không)
  :Trả lỗi 401 Unauthorized;
  stop
else (có)
endif

if (Payload hợp lệ?) then (không)
  :Trả lỗi 400 VALIDATION_ERROR;
  stop
else (có)
endif

if (Idempotency key đã tồn tại?) then (có)
  :Trả booking đã tạo trước đó;
  stop
else (không)
endif

if (User tồn tại?) then (không)
  :Trả lỗi 404 USER_NOT_FOUND;
  stop
else (có)
endif

if (Đã có active hold cho cùng trip?) then (có)
  :Trả lỗi 409 BOOKING_CANNOT_CONFIRM;
  stop
else (không)
endif

if (Trip và route template tồn tại?) then (không)
  :Trả lỗi 404 SCHEDULED_TRIP_NOT_FOUND;
  stop
else (có)
endif

:Tính tổng giá booking;
:Tạo booking HELD với paymentDeadline;
:Lưu booking;

if (Hold được tất cả ghế?) then (không)
  :Trả lỗi 409 SEAT_NOT_AVAILABLE;
  stop
else (có)
endif

:Phát BookingCreated event;
:Trả 201 + BookingResponse(HELD);

fork
  :Codebase có sẵn use case tạo Payment và xử lý webhook Stripe;
fork again
  :Chờ webhook hoặc expiry job;
end fork

stop
@enduml
```

## 4. Lược đồ trạng thái

```plantuml
@startuml UC-08-state
title UC-08: Book Ticket - State Diagram

state "Booking" as booking {
    [*] --> HELD: Create booking
    HELD --> CONFIRMED: Payment success
    HELD --> CANCELLED: Payment deadline expired\nor manual cancel
    CONFIRMED --> [*]
    CANCELLED --> [*]
}

state "Seat" as seat {
    [*] --> AVAILABLE
    AVAILABLE --> HELD: Booking created
    HELD --> BOOKED: Payment success
    HELD --> AVAILABLE: Booking cancelled\nor expired
    BOOKED --> [*]
}

state "Payment" as payment {
    [*] --> PENDING: Checkout session created
    PENDING --> PAID: Stripe webhook success
    PENDING --> FAILED: Stripe webhook failed
    PENDING --> CANCELLED: Session expired
    PENDING --> REFUNDED: Late payment after booking cancelled
    PAID --> [*]
    REFUNDED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
}

@enduml
```

## 5. Lược đồ lớp ý niệm

```plantuml
@startuml UC-08-class
title UC-08: Book Ticket - Conceptual Class Diagram

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
  + create(...): Booking
  + confirm(): Result
  + cancel(): Result
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

class "Money" as Money {
  - amount: long
  - currency: String
}

class "CreateBookingRequest" as CreateReq {
  + scheduledTripId: UUID
  + seatIds: List<UUID>
  + idempotencyKey: String
}

class "BookingResponse" as CreateRes {
  + id: UUID
  + userId: UUID
  + scheduledTripId: UUID
  + userInfo: PassengerInfoResponse
  + totalPrice: long
  + currency: String
  + status: BookingStatus
  + paymentDeadline: Instant
  + createdAt: Instant
}

class "PassengerInfoResponse" as PassengerInfo {
  + fullName: String
  + email: String
  + phone: String
  + dateOfBirth: LocalDate
  + gender: String
  + idDocumentNumber: String
  + addressLine: String
}

class "Payment" as Payment {
  - paymentId: UUID
  - bookingId: UUID
  - userId: UUID
  - amount: Money
  - status: PaymentStatus
  - checkoutSessionId: String
  - checkoutUrl: String
  + markPaid(...): void
  + markCancelled(): void
  + markFailed(...): void
}

enum "PaymentStatus" as PaymentStatus {
  PENDING
  PAID
  FAILED
  CANCELLED
  REFUNDED
}

class "RouteSeatAvailability" as SeatAvailability {
  - scheduledTripId: UUID
  - seatId: UUID
  - status: RouteSeatAvailabilityStatus
  - bookingId: UUID
  - priceAtBooking: Money
  + hold(...): Result
  + confirmHold(): Result
  + expire(): Result
}

enum "RouteSeatAvailabilityStatus" as SeatStatus {
  AVAILABLE
  HELD
  BOOKED
  CANCELLED
}

Booking --> BookingStatus
Booking *-- BookingUserInfo
Booking *-- Money
CreateRes --> BookingStatus
CreateRes *-- PassengerInfo
Payment --> PaymentStatus
Payment *-- Money
SeatAvailability --> SeatStatus
SeatAvailability *-- Money
@enduml
```

## 6. Phân rã thành phần PM

### 6.1 Controller: `BookingController`

- **Nhiệm vụ**: Nhận HTTP request tạo booking từ khách hàng, validate payload,
  lấy `userId` từ `Authentication`, và ủy thác cho `CreateBookingUseCase`.
- **Endpoint**: `POST /api/v1/bookings`
- **Input**: `CreateBookingRequest` -
  `{ scheduledTripId: UUID, seatIds: UUID[], idempotencyKey: String }`
- **Output thành công**: `201` + `BookingResponse` -
  `{ id, userId, scheduledTripId, userInfo, totalPrice, currency, status, paymentDeadline, createdAt }`
- **Output lỗi**: `400 | 404 | 409` + `JsendResponse` - `{ errorCode, message }`

### 6.2 UseCase: `CreateBookingUseCase`

- **Nhiệm vụ**: Orchestrate luồng tạo booking đồng bộ cho UC này.
- **Input**: `CreateBookingCommand` -
  `{ userId, scheduledTripId, seatIds, idempotencyKey }`
- **Output**: `Result<BookingResponse, BookingError>`
- **Gọi đến**:
    - `BookingRepository.findByIdempotencyKey()` - trả booking cũ nếu yêu cầu
      được gửi lại
    - `UserRepository.findSummaryById()` - lấy thông tin hành khách để gán vào
      booking
    - `BookingRepository.findActiveHoldByUserAndScheduledTrip()` - chặn tạo
      nhiều active hold cho cùng trip
    - `ScheduledTripRepository.findById()` - xác nhận scheduled trip tồn tại
    - `RouteTemplateRepository.findById()` - lấy giá cơ sở của tuyến
    - `BookingRepository.save()` - lưu booking mới ở trạng thái `HELD`
    - `RouteSeatAvailabilityManager.holdSeatsWithBookingId()` - giữ chỗ tất cả
      ghế và gán `bookingId`, `priceAtBooking`
- **Phát sinh sự kiện**: `BookingCreated`

### 6.3 Repository: `BookingRepository`

- **Nhiệm vụ**: Truy xuất/lưu trữ aggregate `Booking` và các projection liên
  quan.
- **Phương thức liên quan đến UC**:
    - `findByIdempotencyKey(key): Optional<Booking>` - hỗ trợ idempotency cho
      yêu cầu tạo booking
    - `findActiveHoldByUserAndScheduledTrip(userId, scheduledTripId): Optional<Booking>` -
      kiểm tra active hold trùng lặp
    - `save(booking): Booking` - lưu booking mới ở trạng thái `HELD`

### 6.4 Port: `RouteSeatAvailabilityManager`

- **Lớp**: Application layer cross-module port, implemented bởi
  `RouteSeatAvailabilityManagerAdapter` trong infrastructure của module `train`.
- **Nhiệm vụ**: Giữ chỗ ghế cho booking theo cơ chế all-or-nothing, tránh
  deadlock bằng cách sort `seatIds`, và cập nhật `trip_seat_availability`.
- **Phương thức liên quan đến UC**:
    - `holdSeatsWithBookingId(scheduledTripId, seatIds, bookingId, price): Result<Void, RouteSeatAvailabilityError>` -
      chuyển tất cả ghế `AVAILABLE -> HELD` và lưu giá tại thời điểm đặt

### 6.5 Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-08-internal
title UC-08: Book Ticket - Internal Sequence

actor "Khách hàng" as Actor
participant "BookingController" as CTL
participant "CreateBookingUseCase" as UC
participant "BookingRepository" as BOOKING_REPO
participant "UserRepository" as USER_REPO
participant "ScheduledTripRepository" as TRIP_REPO
participant "RouteTemplateRepository" as ROUTE_REPO
participant "RouteSeatAvailabilityManager" as SEAT_PORT
database "DB" as DB

Actor -> CTL: POST /api/v1/bookings (scheduledTripId, seatIds, idempotencyKey)
CTL -> UC: execute(CreateBookingCommand)

UC -> BOOKING_REPO: findByIdempotencyKey(idempotencyKey)
BOOKING_REPO -> DB: SELECT * FROM bookings WHERE idempotency_key = ?
DB --> BOOKING_REPO: Optional<Booking>
BOOKING_REPO --> UC: Optional<Booking>

alt Đã tồn tại booking với idempotency key
    UC --> CTL: Result.success(BookingResponse cũ)
    CTL --> Actor: 201 + JsendResponse(BookingResponse)
else Chưa tồn tại
    UC -> USER_REPO: findSummaryById(userId)
    USER_REPO -> DB: SELECT user summary
    DB --> USER_REPO: Optional<UserSummary>
    USER_REPO --> UC: Optional<UserSummary>

    UC -> BOOKING_REPO: findActiveHoldByUserAndScheduledTrip(userId, scheduledTripId)
    BOOKING_REPO -> DB: SELECT active hold booking
    DB --> BOOKING_REPO: Optional<Booking>
    BOOKING_REPO --> UC: Optional<Booking>

    UC -> TRIP_REPO: findById(scheduledTripId)
    TRIP_REPO -> DB: SELECT * FROM scheduled_trips WHERE id = ?
    DB --> TRIP_REPO: Optional<ScheduledTrip>
    TRIP_REPO --> UC: Optional<ScheduledTrip>

    UC -> ROUTE_REPO: findById(routeTemplateId)
    ROUTE_REPO -> DB: SELECT * FROM route_templates WHERE id = ?
    DB --> ROUTE_REPO: Optional<RouteTemplate>
    ROUTE_REPO --> UC: Optional<RouteTemplate>

    UC -> UC: Tính totalPrice, tạo Booking(HELD)
    UC -> BOOKING_REPO: save(booking)
    BOOKING_REPO -> DB: INSERT INTO bookings ...
    DB --> BOOKING_REPO: Booking đã lưu
    BOOKING_REPO --> UC: Booking

    UC -> SEAT_PORT: holdSeatsWithBookingId(scheduledTripId, seatIds, bookingId, pricePerSeat)
    SEAT_PORT -> DB: UPDATE trip_seat_availability SET status = HELD, booking_id = ?, price_at_booking = ?
    DB --> SEAT_PORT: success/failure
    SEAT_PORT --> UC: Result<Void, RouteSeatAvailabilityError>

    alt Ghế không khả dụng
        note right of UC
          Booking đã được save trước bước hold seats.
          Vì use case return Result.failure thay vì throw,
          Spring transaction mặc định không rollback.
        end note
        UC --> CTL: Result.failure(SeatNotAvailable)
        CTL --> Actor: 409 + SEAT_NOT_AVAILABLE
    else Thành công
        UC -> UC: Publish BookingCreated event
        UC --> CTL: Result.success(BookingResponse)
        CTL --> Actor: 201 + JsendResponse(BookingResponse)
    end
end

@enduml
```

### 6.6 Giao diện

#### 6.6.1 Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Xác nhận đặt vé
  ..
  {^"Thông tin chuyến tàu"
    Chuyến      | SE1 - Sài Gòn → Đà Nẵng
    Khởi hành   | 06:00, 15/04/2026
    Đến         | 12:30, 15/04/2026
  }
  {^"Ghế đã chọn"
    {#
      Toa | Ghế  | Giá
      1   | 01A  | 500,000đ
      1   | 01B  | 500,000đ
    }
    --
    Tổng cộng: 1,000,000đ
  }
  {^"Thông tin hành khách"
    Họ tên    | Nguyễn Văn A
    Email     | example@email.com
    SĐT       | 0901234567
    CMND/CCCD | 012345678901
  }
  ..
  Thời gian giữ chỗ: 15:00
  ==
  [Quay lại] | [Thanh toán]
}
@endsalt
```

#### 6.6.2 Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

## 7. Bảng tham chiếu dò vết

| Use Case | Controller              | Endpoint                       | UseCase                                                                                               | Repository / Port                                                                     | Table                                      |
| -------- | ----------------------- | ------------------------------ | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------ |
| UC-08    | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | BookingRepository.findByIdempotencyKey()                                              | bookings                                   |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | UserRepository.findSummaryById()                                                      | users                                      |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | BookingRepository.findActiveHoldByUserAndScheduledTrip()                              | bookings                                   |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | ScheduledTripRepository.findById()                                                    | scheduled_trips                            |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | RouteTemplateRepository.findById()                                                    | route_templates                            |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | BookingRepository.save()                                                              | bookings                                   |
|          | BookingController       | `POST /api/v1/bookings`        | CreateBookingUseCase                                                                                  | RouteSeatAvailabilityManager.holdSeatsWithBookingId()                                 | trip_seat_availability                     |
|          | StripeWebhookController | `POST /api/v1/webhooks/stripe` | HandlePaymentSuccessUseCase / HandlePaymentFailedByPaymentIntentUseCase / CancelPendingPaymentUseCase | PaymentRepository, BookingRepository, RouteSeatAvailabilityManager, StripeGatewayPort | payments, bookings, trip_seat_availability |

## 8. Tiêu chí kiểm thử

| Tiêu chí                    | Phép thử                                                                              | Kết quả mong đợi                                                            | Ghi chú                                                                           |
| --------------------------- | ------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| Toàn diện (coverage)        | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện            | Không bỏ sót luồng chính lẫn ngoại lệ                                       | Rà soát chéo giữa mục 2 và mục 3                                                  |
| Nhất quán                   | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC                       | Không mâu thuẫn giữa các mục 2-6                                            | Đặc biệt kiểm tra BookingStatus, PaymentStatus, SeatStatus                        |
| Truy vết                    | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.5)                | Mọi tương tác trong sequence đều có entry                                   | Kiểm tra không thiếu repository/port                                              |

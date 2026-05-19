# UC-10: Hủy đặt vé

# Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập, UC-08: Đặt vé tàu, UC-09: Xem đặt vé                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Mục đích                       | Khách hàng cần hủy đặt vé khi thay đổi kế hoạch. Hệ thống giải phóng ghế đã giữ/đặt để người khác có thể sử dụng, và phát sinh sự kiện `BookingCancelled` mang cờ `requiresRefund` để hỗ trợ luồng hoàn tiền (nếu đã thanh toán).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Mô tả                          | Khách hàng hủy một đặt vé đang ở trạng thái `HELD` (chưa thanh toán) hoặc `CONFIRMED` (đã thanh toán). Hệ thống giải phóng ghế và phát sinh sự kiện `BookingCancelled`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Actor chính                    | Khách hàng (Customer)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Actor liên quan                | Không (Ghi chú: sự kiện `BookingCancelled(requiresRefund=true)` được publish nhưng hiện chưa có listener tự động nối đến `RefundPaymentUseCase`. Luồng hoàn tiền qua Stripe chưa được wiring.)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ. Đặt vé đang ở trạng thái `HELD` hoặc `CONFIRMED`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Dãy lệnh thực hiện bình thường | **Hủy đặt vé HELD (chưa thanh toán):** <br> 1. Khách hàng gửi yêu cầu hủy đặt vé theo `bookingId`. <br> 2. Hệ thống xác thực quyền: `booking.userId == requestingUserId`. <br> 3. Hệ thống gọi `booking.cancel()` → chuyển `HELD → CANCELLED`, register `BookingCancelled(requiresRefund=false)`. <br> 4. Hệ thống lấy danh sách seatIds qua `RouteSeatAvailabilityManager.findSeatIdsByBookingId()`. <br> 5. Hệ thống gọi `releaseHeldSeats(scheduledTripId, seatIds)` → ghế `HELD → AVAILABLE`. <br> 6. Hệ thống lưu booking, publish domain events + `SeatStatusChangedEvent` (SSE). <br> 7. Hệ thống trả về 200 OK. <br><br> **Hủy đặt vé CONFIRMED (đã thanh toán):** <br> 1–2. Như trên. <br> 3. `booking.cancel()` → `CONFIRMED → CANCELLED`, register `BookingCancelled(requiresRefund=true)`. <br> 4. Lấy seatIds. <br> 5. Gọi `cancelBookedSeats(scheduledTripId, seatIds)` → ghế `BOOKED → CANCELLED`. <br> 6–7. Như trên. |
| Hậu điều kiện (thành công)     | Đặt vé ở trạng thái `CANCELLED`. Ghế được giải phóng (`HELD → AVAILABLE`) hoặc hủy (`BOOKED → CANCELLED`). Sự kiện `BookingCancelled` được publish. `SeatStatusChangedEvent` được publish (SSE push).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Hậu điều kiện (thất bại)       | Không có thay đổi trạng thái. Transaction rollback nếu có lỗi.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Xử lý ngoại lệ                 | Chưa xác thực → 401 Unauthorized. <br> Đặt vé không tồn tại → 404 + `BOOKING_NOT_FOUND`. <br> Hủy đặt vé của người khác → 403 + `ACCESS_DENIED`. <br> Đặt vé đã ở trạng thái `CANCELLED` → 409 + `BOOKING_ALREADY_CANCELLED` (do `InvalidStatusTransition`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

# Lược đồ tuần tự

```plantuml
@startuml UC-10
title UC-10: Hủy đặt vé

actor "Khách hàng" as Actor
participant "Hệ thống" as API

Actor -> API: CancelBooking(bookingId)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Đặt vé không tồn tại
    API --> Actor: 404 + BOOKING_NOT_FOUND
else booking.userId != requestingUserId
    API --> Actor: 403 + ACCESS_DENIED
else Đặt vé đã CANCELLED
    API --> Actor: 409 + BOOKING_ALREADY_CANCELLED
else Hủy HELD (chưa thanh toán)
    API -> API: booking.cancel() → HELD → CANCELLED\n  register BookingCancelled(requiresRefund=false)
    API -> API: releaseHeldSeats() → HELD → AVAILABLE
    API -> API: save booking, publish events\n  + SeatStatusChangedEvent (SSE)
    API --> Actor: 200 OK
else Hủy CONFIRMED (đã thanh toán)
    API -> API: booking.cancel() → CONFIRMED → CANCELLED\n  register BookingCancelled(requiresRefund=true)
    API -> API: cancelBookedSeats() → BOOKED → CANCELLED
    API -> API: save booking, publish events\n  + SeatStatusChangedEvent (SSE)
    API --> Actor: 200 OK
end
@enduml
```

# Lược đồ hoạt động

```plantuml
@startuml UC-10-activity
title UC-10: Hủy đặt vé - Activity Diagram

start

:Khách hàng gửi yêu cầu hủy đặt vé;

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

:Ghi nhớ previousStatus;

if (booking.cancel() thành công?) then (không — đã CANCELLED)
  :Trả 409 BOOKING_ALREADY_CANCELLED;
  stop
else (có)
endif

:Lấy seatIds qua findSeatIdsByBookingId();

if (seatIds không rỗng?) then (có)
  if (previousStatus == HELD?) then (có)
    :releaseHeldSeats() → HELD → AVAILABLE;
  else (previousStatus == CONFIRMED)
    :cancelBookedSeats() → BOOKED → CANCELLED;
  endif
else (không — rỗng)
endif

:bookingRepository.save(booking);
:Publish BookingCancelled domain event;

if (seatIds không rỗng?) then (có)
  :Lấy affected seats;
  :Publish SeatStatusChangedEvent (SSE);
else (không)
endif

:Trả 200 OK;

stop
@enduml
```

# Lược đồ trạng thái

```plantuml
@startuml UC-10-state
title UC-10: Hủy đặt vé - State Diagram

state "Booking" as booking {
  [*] --> HELD
  [*] --> CONFIRMED

  HELD --> CANCELLED: cancel()\n[requiresRefund=false]
  CONFIRMED --> CANCELLED: cancel()\n[requiresRefund=true]
  CANCELLED -[dashed]-> CANCELLED: cancel()\n→ InvalidStatusTransition
}

state "Ghế (RouteSeatAvailability)" as seat {
  [*] --> sHELD: (từ UC-08)
  [*] --> sBOOKED: (từ payment success)

  sHELD --> sAVAILABLE: releaseHeldSeats()
  sBOOKED --> sCANCELLED: cancelBookedSeats()
}

note right of booking
  BookingCancelled event được publish
  với requiresRefund flag.
  Hiện chưa có listener tự động
  kết nối đến RefundPaymentUseCase.
end note

note right of seat
  SeatStatusChangedEvent (SSE)
  được publish sau khi ghế
  thay đổi trạng thái.
end note
@enduml
```

# Lược đồ lớp ý niệm

```plantuml
@startuml UC-10-class
title UC-10: Hủy đặt vé - Conceptual Class Diagram

class "Booking" as Booking {
  - bookingId: UUID
  - userId: UUID
  - scheduledTripId: UUID
  - status: BookingStatus
  + cancel(): Result<Void, BookingError>
  + getDomainEvents(): List<DomainEvent>
  + clearDomainEvents(): void
}

enum "BookingStatus" as BookingStatus {
  HELD
  CONFIRMED
  CANCELLED
}

class "BookingCancelled" as BCEvent <<DomainEvent>> {
  + bookingId: BookingId
  + userId: UserId
  + scheduledTripId: ScheduledTripId
  + requiresRefund: boolean
  + occurredAt: Instant
}

class "SeatStatusChangedEvent" as SSEEvent <<ApplicationEvent>> {
  + scheduledTripId: UUID
  + changes: List<SeatChange>
  + occurredAt: Instant
}

class "SeatStatusChangedEvent.SeatChange" as SeatChange {
  + seatId: UUID
  + status: String
  + bookingId: UUID
}

class "CancelBookingCommand" as Cmd {
  + bookingId: UUID
  + requestingUserId: UUID
}

class "RouteSeatAvailability" as SeatAvail {
  - scheduledTripId: UUID
  - seatId: UUID
  - status: RouteSeatAvailabilityStatus
  - bookingId: UUID
}

enum "RouteSeatAvailabilityStatus" as SeatStatus {
  AVAILABLE
  HELD
  BOOKED
  CANCELLED
}

Booking --> BookingStatus
Booking ..> BCEvent: register on cancel()
SeatAvail --> SeatStatus
SSEEvent *-- SeatChange
@enduml
```

# Phân rã thành phần PM

## Controller: `BookingController`

- **Nhiệm vụ**: Nhận HTTP request hủy đặt vé, lấy `requestingUserId` từ
  `Authentication`, ủy thác cho `CancelBookingUseCase`.
- **Endpoint**: `POST /api/v1/bookings/{id}/cancel`
- **Input**: Path `id: UUID`
- **Output thành công**: `200` + `JsendResponse.success()` (body trống, không có
  data payload)
- **Output lỗi**: `403` + `ACCESS_DENIED` | `404` + `BOOKING_NOT_FOUND` |
  `409` + `BOOKING_ALREADY_CANCELLED`
- **Metadata**: `@SuccessPayload` (không có value — response body trống)
- **Error mapping**:
    - `BookingError.BookingNotFound` → `404` + `BOOKING_NOT_FOUND`
    - `BookingError.Forbidden` → `403` + `ACCESS_DENIED`
    - `BookingError.InvalidStatusTransition` → `409` +
      `BOOKING_ALREADY_CANCELLED`

## UseCase: `CancelBookingUseCase`

- **Nhiệm vụ**: Orchestrate luồng hủy đặt vé đồng bộ, bao gồm giải phóng ghế và
  phát sinh sự kiện.
- **Input**: `CancelBookingCommand` — `{ bookingId, requestingUserId }`
- **Output**: `Result<Void, BookingError>`
- **Annotation**: `@Transactional`
- **Gọi đến**:
    - `BookingRepository.findById(bookingId)` — lấy booking entity
    - `Booking.cancel()` — chuyển trạng thái và register `BookingCancelled`
      event
    - `RouteSeatAvailabilityManager.findSeatIdsByBookingId(bookingId)` — lấy
      danh sách ghế liên quan
    - `RouteSeatAvailabilityManager.releaseHeldSeats(scheduledTripId, seatIds)`
      — giải phóng ghế HELD → AVAILABLE (nếu previousStatus == HELD)
    - `RouteSeatAvailabilityManager.cancelBookedSeats(scheduledTripId, seatIds)`
      — hủy ghế BOOKED → CANCELLED (nếu previousStatus == CONFIRMED)
    - `BookingRepository.save(booking)` — lưu booking đã hủy
    - `ApplicationEventPublisher.publishEvent(DomainEvent)` — publish
      `BookingCancelled`
    - `RouteSeatAvailabilityManager.findByScheduledTripIdAndSeatIds(...)` — lấy
      trạng thái ghế sau cập nhật
    - `ApplicationEventPublisher.publishEvent(SeatStatusChangedEvent)` — SSE
      push
- **Phát sinh sự kiện**:
    - `BookingCancelled(bookingId, userId, scheduledTripId, requiresRefund, occurredAt)`
      — `requiresRefund=false` nếu HELD, `true` nếu CONFIRMED
    - `SeatStatusChangedEvent(scheduledTripId, changes, occurredAt)` — SSE push
      cho real-time seat map update

## Repository

**BookingRepository:**

- **Nhiệm vụ**: Truy xuất và lưu trữ domain entity `Booking`.
- **Phương thức liên quan đến UC**:
    - `findById(BookingId): Optional<Booking>` — lấy booking entity
    - `save(Booking): Booking` — lưu booking đã hủy
- **Table**: `bookings`

## Port: `RouteSeatAvailabilityManager`

- **Nhiệm vụ**: Cross-module port cho phép booking module thao tác trạng thái
  ghế trên scheduled trip.
- **Phương thức liên quan đến UC**:
    - `findSeatIdsByBookingId(UUID): List<SeatId>` — tìm ghế liên quan đến
      booking
    - `releaseHeldSeats(ScheduledTripId, List<SeatId>): Result<Void, Error>` —
      giải phóng ghế `HELD → AVAILABLE`
    - `cancelBookedSeats(ScheduledTripId, List<SeatId>): Result<Void, Error>` —
      hủy ghế `BOOKED → CANCELLED`
    - `findByScheduledTripIdAndSeatIds(ScheduledTripId, List<SeatId>): List<RouteSeatAvailability>`
      — lấy trạng thái ghế sau cập nhật (cho SSE event)
- **Implementation**: `RouteSeatAvailabilityManagerAdapter` (train BC)

## Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-10-internal
title UC-10: Hủy đặt vé - Internal Sequence

actor "Khách hàng" as Actor
participant "BookingController" as CTL
participant "CancelBookingUseCase" as UC
participant "BookingRepository" as REPO
participant "RouteSeatAvailabilityManager" as PORT
database "DB" as DB

Actor -> CTL: POST /api/v1/bookings/{id}/cancel
CTL -> UC: execute(CancelBookingCommand(bookingId, requestingUserId))

UC -> REPO: findById(bookingId)
REPO -> DB: SELECT * FROM bookings WHERE id = ?
DB --> REPO: Optional<Booking>
REPO --> UC: Optional<Booking>

alt Booking không tồn tại
    UC --> CTL: Result.failure(BookingNotFound)
    CTL --> Actor: 404 + BOOKING_NOT_FOUND
else booking.userId != requestingUserId
    UC --> CTL: Result.failure(Forbidden)
    CTL --> Actor: 403 + ACCESS_DENIED
else Booking tồn tại và thuộc về user
    UC -> UC: ghi nhớ previousStatus
    UC -> UC: booking.cancel()

    alt Đã CANCELLED
        UC --> CTL: Result.failure(InvalidStatusTransition)
        CTL --> Actor: 409 + BOOKING_ALREADY_CANCELLED
    else Cancel thành công
        UC -> PORT: findSeatIdsByBookingId(bookingId)
        PORT -> DB: SELECT seat_id FROM trip_seat_availability\nWHERE booking_id = ?
        DB --> PORT: List<SeatId>
        PORT --> UC: List<SeatId>

        alt previousStatus == HELD && seatIds not empty
            UC -> PORT: releaseHeldSeats(scheduledTripId, seatIds)
            PORT -> DB: UPDATE trip_seat_availability\nSET status='AVAILABLE', booking_id=NULL\nWHERE ...
            DB --> PORT: OK
            PORT --> UC: Result.success()
        else previousStatus == CONFIRMED && seatIds not empty
            UC -> PORT: cancelBookedSeats(scheduledTripId, seatIds)
            PORT -> DB: UPDATE trip_seat_availability\nSET status='CANCELLED'\nWHERE ...
            DB --> PORT: OK
            PORT --> UC: Result.success()
        end

        UC -> REPO: save(booking)
        REPO -> DB: UPDATE bookings SET status='CANCELLED' WHERE id = ?
        DB --> REPO: OK
        REPO --> UC: Booking

        UC -> UC: publishEvent(BookingCancelled)

        opt seatIds not empty
            UC -> PORT: findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds)
            PORT -> DB: SELECT ... FROM trip_seat_availability WHERE ...
            DB --> PORT: List<RouteSeatAvailability>
            PORT --> UC: List<RouteSeatAvailability>
            UC -> UC: publishEvent(SeatStatusChangedEvent) — SSE push
        end

        UC --> CTL: Result.success()
        CTL --> Actor: 200 OK
    end
end
@enduml
```

## Giao diện

### Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Xác nhận hủy đặt vé
  ..
  {SI
    Bạn có chắc chắn muốn hủy đặt vé #BK001?

    Thông tin đặt vé:
    • Chuyến: SE1 (SGN → DNA)
    • Ngày: 15/04/2026
    • Ghế: Toa 1 - 01A, 01B
    • Tổng tiền: 1,000,000đ

    Lưu ý:
    • Nếu đã thanh toán, bạn sẽ được hoàn tiền
    • Bạn có thể đặt lại ghế sau khi hủy
  }
  ==
  [Quay lại] | [<color:Red>Xác nhận hủy]
}
@endsalt
```

### Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

# Bảng tham chiếu dò vết

| Use Case | Controller        | Endpoint                            | UseCase              | Repository / Port                                              | Table                  |
| -------- | ----------------- | ----------------------------------- | -------------------- | -------------------------------------------------------------- | ---------------------- |
| UC-10    | BookingController | `POST /api/v1/bookings/{id}/cancel` | CancelBookingUseCase | BookingRepository.findById()                                   | bookings               |
|          |                   |                                     |                      | BookingRepository.save()                                       | bookings               |
|          |                   |                                     |                      | RouteSeatAvailabilityManager.findSeatIdsByBookingId()          | trip_seat_availability |
|          |                   |                                     |                      | RouteSeatAvailabilityManager.releaseHeldSeats()                | trip_seat_availability |
|          |                   |                                     |                      | RouteSeatAvailabilityManager.cancelBookedSeats()               | trip_seat_availability |
|          |                   |                                     |                      | RouteSeatAvailabilityManager.findByScheduledTripIdAndSeatIds() | trip_seat_availability |

# Tiêu chí kiểm thử

| Tiêu chí                      | Phép thử                                                                   | Kết quả mong đợi                                                                                       | Ghi chú                                                   |
| ----------------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | --------------------------------------------------------- |
| Toàn diện (coverage)          | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ                                                                  | Rà soát chéo giữa mục 2 và mục 3                          |
| Nhất quán                     | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–6                                                                       | Đặc biệt kiểm tra tên trong mục 5–6                       |
| Truy vết                      | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.5)     | Mọi tương tác trong sequence đều có entry                                                              | Kiểm tra không thiếu endpoint/method                      |

# UC-08: Xem sơ đồ ghế chuyến tàu

## 1. Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập, UC-07: Tra cứu chuyến tàu                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Mục đích                       | Khách hàng đã chọn chuyến tàu từ UC-07 và muốn xem tình trạng ghế trước khi đặt vé. PM cung cấp hai góc nhìn: danh sách ghế trống đơn giản (flat list) để chọn nhanh, và sơ đồ ghế theo toa chi tiết (coach seat map) để biết vị trí và trạng thái từng ghế trong toa.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Mô tả                          | Khách hàng xem sơ đồ ghế của một chuyến tàu cụ thể để biết trạng thái từng ghế (trống, đang giữ, đã đặt) trước khi đặt vé. Hỗ trợ hai góc nhìn: danh sách ghế trống đơn giản và sơ đồ ghế theo toa chi tiết.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Actor chính                    | Khách hàng đã đăng nhập                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Actor liên quan                | Không                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ. Khách hàng đã biết mã chuyến tàu (`scheduledTripId`) từ UC-07.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Dãy lệnh thực hiện bình thường | **Xem danh sách ghế trống:** <br> 1. Khách hàng gửi yêu cầu xem ghế trống của chuyến tàu với `scheduledTripId` và tham số phân trang (`page`, `size`). <br> 2. Hệ thống trả về danh sách ghế có trạng thái `AVAILABLE` (bao gồm cả ghế `HELD` đã quá hạn thanh toán), sắp xếp theo số ghế. <br> **Lưu ý:** Endpoint này không kiểm tra chuyến tàu tồn tại; nếu `scheduledTripId` không hợp lệ, hệ thống trả về trang rỗng. <br><br> **Xem sơ đồ ghế theo toa:** <br> 1. Khách hàng gửi yêu cầu xem sơ đồ ghế theo toa của chuyến tàu với `scheduledTripId` và tham số phân trang (`page`, `size`). <br> 2. Hệ thống trả về danh sách toa phân trang, mỗi toa kèm danh sách ghế với trạng thái hiện tại (`AVAILABLE`, `HELD`, `BOOKED`) (kết quả được cache). <br> **Lưu ý:** Endpoint này kiểm tra chuyến tàu tồn tại; nếu không tìm thấy, trả lỗi 404. |
| Hậu điều kiện (thành công)     | Không có thay đổi trạng thái. Đây là thao tác chỉ đọc.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Hậu điều kiện (thất bại)       | Không có thay đổi trạng thái. Dữ liệu trong hệ thống không bị ảnh hưởng.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Xử lý ngoại lệ                 | Chưa xác thực (thiếu hoặc sai access token) → Hệ thống trả về lỗi 401. <br> Chuyến tàu không tồn tại (sơ đồ ghế theo toa) → Hệ thống trả về lỗi `SCHEDULED_TRIP_NOT_FOUND`. <br> Chuyến tàu không tồn tại (danh sách ghế trống) → Hệ thống trả về trang rỗng (không kiểm tra tồn tại). <br> Chuyến tàu tồn tại nhưng chưa có toa → Hệ thống trả về trang rỗng. <br> Tham số phân trang không hợp lệ → Hệ thống trả về lỗi `VALIDATION_ERROR`.                                                                                                                                                                                                                                                                                                                                                                                                           |

## 2. Lược đồ tuần tự

```plantuml
@startuml UC-08
title UC-08: View Seat Map

actor "Khách hàng" as Actor
participant "Hệ thống" as API

== Danh sách ghế trống (flat list) ==

Actor -> API: GetAvailableSeats(scheduledTripId, page, size)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Phân trang không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Thành công
    API -> API: Truy vấn ghế AVAILABLE (bao gồm HELD quá hạn)
    API --> Actor: 200 + PageResponse<SeatResponse>(id, coachId, seatNumber, createdAt)
end

== Sơ đồ ghế theo toa (coach seat map) ==

Actor -> API: GetCoachSeatMap(scheduledTripId, page, size)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Phân trang không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Chuyến tàu không tồn tại
    API --> Actor: 404 + SCHEDULED_TRIP_NOT_FOUND
else Chuyến tàu tồn tại nhưng chưa có toa
    API --> Actor: 200 + PageResponse rỗng
else Thành công
    API -> API: Truy vấn toa và ghế, cache kết quả
    API --> Actor: 200 + PageResponse<CoachSeatMapResponse>(id, carNumber, totalSeats, seats[](id, seatNumber, status))
end
@enduml
```

## 3. Lược đồ hoạt động

```plantuml
@startuml UC-08-activity
title UC-08: View Seat Map - Activity Diagram

start

:Khách hàng gửi yêu cầu xem ghế;

if (Access token hợp lệ?) then (không)
  :Trả lỗi 401 Unauthorized;
  stop
else (có)
endif

switch (Loại yêu cầu?)
case (Available Seats)
  if (Tham số phân trang hợp lệ?) then (không)
    :Trả lỗi 400 VALIDATION_ERROR;
    stop
  else (có)
  endif

  :Truy vấn ghế AVAILABLE theo scheduledTripId;
  note right
    Bao gồm ghế HELD đã quá hạn.
    Không kiểm tra trip tồn tại —
    nếu trip không có, trả trang rỗng.
  end note

  :Trả 200 + PageResponse<SeatResponse>;

case (Coach Seat Map)
  if (Tham số phân trang hợp lệ?) then (không)
    :Trả lỗi 400 VALIDATION_ERROR;
    stop
  else (có)
  endif

  :Truy vấn toa theo scheduledTripId;

  if (Có toa nào?) then (không)
    if (Chuyến tàu tồn tại?) then (không)
      :Trả lỗi 404 SCHEDULED_TRIP_NOT_FOUND;
      stop
    else (có)
      :Trả 200 + PageResponse rỗng;
      stop
    endif
  else (có)
  endif

  :Truy vấn ghế cho các toa trả về;
  :Nhóm ghế theo toa;
  :Cache kết quả;
  :Trả 200 + PageResponse<CoachSeatMapResponse>;

endswitch

stop
@enduml
```

## 5. Lược đồ lớp ý niệm

```plantuml
@startuml UC-08-class
title UC-08: View Seat Map - Conceptual Class Diagram

class "Seat" as Seat {
  - id: UUID
  - coachId: UUID
  - seatNumber: String
  - createdAt: Instant
}

class "Coach" as Coach {
  - id: UUID
  - trainId: UUID
  - carNumber: int
  - totalSeats: int
}

class "RouteSeatAvailability" as RSA {
  - scheduledTripId: UUID
  - seatId: UUID
  - status: RouteSeatAvailabilityStatus
  - bookingId: UUID
  - priceAtBooking: Money
  - version: Integer
}

enum "RouteSeatAvailabilityStatus" as RSAS {
  AVAILABLE
  HELD
  BOOKED
  CANCELLED
}

class "SeatResponse" as SeatRes {
  + id: UUID
  + coachId: UUID
  + seatNumber: String
  + createdAt: Instant
}

class "CoachSeatMapResponse" as CoachRes {
  + id: UUID
  + carNumber: int
  + totalSeats: int
  + seats: List<Seat>
}

class "CoachSeatMapResponse.Seat" as CoachSeat {
  + id: UUID
  + seatNumber: String
  + status: RouteSeatAvailabilityStatus
}

Coach "1" *-- "many" Seat
RSA --> RSAS
RSA --> Seat : tracks availability of >
CoachRes *-- CoachSeat
@enduml
```

## 6. Phân rã thành phần PM

### 6.1 Controller: `SeatController`

-  **Nhiệm vụ**: Nhận HTTP request từ khách hàng, xác thực đầu vào, ủy thác cho
  UseCase tương ứng.

**Endpoint 1 — Danh sách ghế trống:**

-  **Endpoint**: `GET /api/v1/scheduled-trips/{scheduledTripId}/seats/available`
-  **Input**: `scheduledTripId` (UUID path variable) + `GetAvailableSeatsRequest`
  — `{ page, size }`
-  **Output thành công**: `200` + `PageResponse<SeatResponse>`
-  **Output lỗi**: `400` + `JsendResponse` —
  `{ errorCode: VALIDATION_ERROR, message }`
-  **Ghi chú metadata**: Controller annotation hiện còn khai báo `404` cho
  endpoint này, nhưng runtime code hiện chỉ trả `200` hoặc lỗi validation.

**Endpoint 2 — Sơ đồ ghế theo toa:**

-  **Endpoint**: `GET /api/v1/scheduled-trips/{scheduledTripId}/coach-seats`
-  **Input**: `scheduledTripId` (UUID path variable) + `GetCoachSeatMapRequest` —
  `{ page, size }`
-  **Output thành công**: `200` + `PageResponse<CoachSeatMapResponse>`
-  **Output lỗi**: `404` + `JsendResponse` —
  `{ errorCode: SCHEDULED_TRIP_NOT_FOUND, message }` hoặc `400` +
  `VALIDATION_ERROR`
-  **Ghi chú metadata**: Runtime trả `PageResponse<CoachSeatMapResponse>`, nhưng
  `@SuccessPayload` hiện dùng mặc định object metadata thay vì page metadata.

### 6.2 UseCase

**GetAvailableSeatsForScheduledTripUseCase:**

-  **Nhiệm vụ**: Truy vấn danh sách ghế trống cho chuyến tàu, sắp xếp theo số
  ghế.
-  **Input**: `GetAvailableSeatsQuery` — `{ page, size, scheduledTripId }`
-  **Output**: `PageResponse<SeatResponse>`
-  **Gọi đến**:
    -  `SeatRepository.findAllAvailableSummaries(page, size, sort, scheduledTripId)`
      — truy vấn ghế AVAILABLE (bao gồm HELD quá hạn)
-  **Lưu ý**: Không kiểm tra chuyến tàu tồn tại. Nếu scheduledTripId không hợp
  lệ, trả trang rỗng. Không cache.

**GetCoachSeatMapByScheduledTripUseCase:**

-  **Nhiệm vụ**: Truy vấn sơ đồ ghế theo toa cho chuyến tàu, cache kết quả.
-  **Input**: `GetCoachSeatMapQuery` — `{ page, size, scheduledTripId }`
-  **Output**: `Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>`
-  **Gọi đến**:
    -  `ScheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(page, size, scheduledTripId)`
      — lấy danh sách toa phân trang
    -  `ScheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(scheduledTripId, coachIds)`
      — lấy ghế cho các toa
    -  `ScheduledTripRepository.existsById(scheduledTripId)` — kiểm tra tồn tại
      (chỉ khi không có toa)
-  **Cache**: `coachSeatMap`, key = `st-coach:{scheduledTripId}:{page}:{size}`,
  trừ khi failure hoặc content rỗng

### 6.3 Repository

**SeatRepository:**

-  **Nhiệm vụ**: Truy xuất domain entity `Seat` và các projection summary.
-  **Phương thức liên quan đến UC**:
    -  `findAllAvailableSummaries(page, size, sort, scheduledTripId): PageResponse<SeatSummary>`
      — danh sách ghế trống (AVAILABLE + HELD quá hạn)

**ScheduledTripSeatMapRepository:**

-  **Nhiệm vụ**: Truy xuất sơ đồ ghế theo toa cho chuyến tàu.
-  **Phương thức liên quan đến UC**:
    -  `findCoachSummariesByScheduledTripId(page, size, scheduledTripId): PageResponse<CoachSeatMapCoachSummary>`
      — danh sách toa phân trang
    -  `findSeatSummariesByScheduledTripIdAndCoachIds(scheduledTripId, coachIds): List<CoachSeatMapSeatSummary>`
      — ghế cho các toa kèm trạng thái

### 6.4 Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-08-internal
title UC-08: View Seat Map - Internal Sequence

actor "Khách hàng" as Actor
participant "SeatController" as CTL
participant "GetAvailableSeatsForScheduledTripUseCase" as AVAIL_UC
participant "GetCoachSeatMapByScheduledTripUseCase" as COACH_UC
participant "SeatRepository" as SEAT_REPO
participant "ScheduledTripSeatMapRepository" as MAP_REPO
participant "ScheduledTripRepository" as TRIP_REPO
database "DB" as DB

== Available Seats: GET /api/v1/scheduled-trips/{id}/seats/available ==

Actor -> CTL: GET /api/v1/scheduled-trips/{scheduledTripId}/seats/available (page, size)
CTL -> AVAIL_UC: execute(GetAvailableSeatsQuery)
AVAIL_UC -> SEAT_REPO: findAllAvailableSummaries(page, size, sort, scheduledTripId)
SEAT_REPO -> DB: SELECT seats JOIN trip_seat_availability LEFT JOIN bookings\nWHERE status=AVAILABLE OR (status=HELD AND payment_deadline < NOW())
DB --> SEAT_REPO: PageResponse<SeatSummary>
SEAT_REPO --> AVAIL_UC: PageResponse<SeatSummary>
AVAIL_UC --> CTL: PageResponse<SeatResponse>
CTL --> Actor: 200 + JsendResponse(PageResponse)

== Coach Seat Map: GET /api/v1/scheduled-trips/{id}/coach-seats ==

Actor -> CTL: GET /api/v1/scheduled-trips/{scheduledTripId}/coach-seats (page, size)
CTL -> COACH_UC: execute(GetCoachSeatMapQuery)
COACH_UC -> MAP_REPO: findCoachSummariesByScheduledTripId(page, size, scheduledTripId)
MAP_REPO -> DB: SELECT DISTINCT coaches FROM trip_seat_availability JOIN seats JOIN coaches WHERE scheduled_trip_id = ?
DB --> MAP_REPO: PageResponse<CoachSeatMapCoachSummary>
MAP_REPO --> COACH_UC: PageResponse<CoachSeatMapCoachSummary>

alt Không có toa
    COACH_UC -> TRIP_REPO: existsById(scheduledTripId)
    TRIP_REPO -> DB: SELECT 1 FROM scheduled_trips WHERE id = ?
    DB --> TRIP_REPO: boolean
    TRIP_REPO --> COACH_UC: boolean

    alt Chuyến tàu không tồn tại
        COACH_UC --> CTL: Result.failure(ScheduledTripNotFound)
        CTL --> Actor: 404 + SCHEDULED_TRIP_NOT_FOUND
    else Chuyến tàu tồn tại, chưa có toa
        COACH_UC --> CTL: Result.success(PageResponse rỗng)
        CTL --> Actor: 200 + JsendResponse(PageResponse rỗng)
    end
else Có toa
    COACH_UC -> MAP_REPO: findSeatSummariesByScheduledTripIdAndCoachIds(scheduledTripId, coachIds)
    MAP_REPO -> DB: SELECT seats JOIN trip_seat_availability WHERE coach_id IN (...)
    DB --> MAP_REPO: List<CoachSeatMapSeatSummary>
    MAP_REPO --> COACH_UC: List<CoachSeatMapSeatSummary>
    COACH_UC -> COACH_UC: Nhóm ghế theo toa
    COACH_UC --> CTL: Result.success(PageResponse<CoachSeatMapResponse>)
    CTL --> Actor: 200 + JsendResponse(PageResponse)
end
@enduml
```

## 7. Bảng tham chiếu dò vết

| Use Case | Controller     | Endpoint                                                        | UseCase                                  | Repository / Port                                                                                                                                           | Table                                                   |
| -------- | -------------- | --------------------------------------------------------------- | ---------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| UC-08    | SeatController | `GET /api/v1/scheduled-trips/{scheduledTripId}/seats/available` | GetAvailableSeatsForScheduledTripUseCase | SeatRepository.findAllAvailableSummaries()                                                                                                                  | seats, trip_seat_availability, bookings                 |
|          | SeatController | `GET /api/v1/scheduled-trips/{scheduledTripId}/coach-seats`     | GetCoachSeatMapByScheduledTripUseCase    | ScheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(), findSeatSummariesByScheduledTripIdAndCoachIds(); ScheduledTripRepository.existsById() | coaches, seats, trip_seat_availability, scheduled_trips |

## 8. Tiêu chí kiểm thử

| Tiêu chí                              | Phép thử                                                                   | Kết quả mong đợi                                                                 | Ghi chú                                 |
| ------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | --------------------------------------- |
| Toàn diện (coverage)                  | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ                                            | Rà soát chéo giữa mục 2 và mục 3        |
| Nhất quán                             | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–6                                                 | Đặc biệt kiểm tra tên DTO trong mục 5–6 |
| Truy vết                              | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.4)     | Mọi tương tác trong sequence đều có entry                                        | Kiểm tra không thiếu endpoint/method    |
| Available — happy path                | Gửi request với scheduledTripId hợp lệ, page=0, size=20                    | 200 + PageResponse<SeatResponse> chứa ghế AVAILABLE, sắp xếp theo seatNumber ASC |                                         |
| Available — trip không tồn tại        | Gửi request với scheduledTripId không tồn tại                              | 200 + PageResponse rỗng (content=[], total=0)                                    | Không trả 404 — behavior by design      |
| Available — bad pagination            | Gửi request với size=0 hoặc size > 100                                     | 400 + VALIDATION_ERROR                                                           |                                         |
| Coach map — happy path                | Gửi request với scheduledTripId hợp lệ có toa và ghế                       | 200 + PageResponse<CoachSeatMapResponse> chứa toa kèm ghế với trạng thái         | Kiểm tra cache hit lần gọi thứ 2        |
| Coach map — trip không tồn tại        | Gửi request với scheduledTripId không tồn tại                              | 404 + SCHEDULED_TRIP_NOT_FOUND                                                   |                                         |
| Coach map — trip có nhưng chưa có toa | Gửi request với scheduledTripId tồn tại nhưng chưa gán toa                 | 200 + PageResponse rỗng                                                          | Kết quả rỗng không được cache           |
| Coach map — bad pagination            | Gửi request với size < 1 hoặc size > 100                                   | 400 + VALIDATION_ERROR                                                           |                                         |
| Unauthenticated                       | Gửi bất kỳ request nào không có access token                               | 401 Unauthorized                                                                 | Xử lý bởi Spring Security filter chain  |

# UC-06: Tra cứu chuyến tàu

# Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập, UC-05: Tra cứu ga tàu                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Mục đích                       | Khách hàng đã đăng nhập muốn tìm chuyến tàu phù hợp để đặt vé. PM cung cấp ba cách tra cứu: tìm kiếm có bộ lọc (cursor-based), duyệt danh sách phân trang (offset-based) và xem chi tiết một chuyến tàu cụ thể, giúp khách hàng đánh giá chuyến tàu trước khi chuyển sang đặt vé.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Mô tả                          | Khách hàng tra cứu chuyến tàu theo nhiều bộ lọc, duyệt danh sách phân trang, hoặc xem chi tiết một chuyến tàu cụ thể. Kết quả tìm kiếm bao gồm thông tin tàu, tuyến đường, giá vé và số ghế trống.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Actor chính                    | Khách hàng đã đăng nhập                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Actor liên quan                | Không                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Dãy lệnh thực hiện bình thường | **Tìm kiếm có bộ lọc (cursor-based):** <br> 1. Khách hàng gửi yêu cầu tìm kiếm chuyến tàu với các bộ lọc tùy chọn: ga đi (`originStationId`), ga đến (`destinationStationId`), ngày khởi hành (`departureDate`), trạng thái (`status`), chỉ chuyến còn ghế (`availableOnly`), khoảng giá (`minPrice`, `maxPrice`). <br> 2. Khách hàng có thể chọn sắp xếp theo: thời gian khởi hành (`DEPARTURE_TIME`), giá (`PRICE`), thời lượng (`DURATION`), hoặc số ghế trống (`AVAILABLE_SEATS`); hướng sắp xếp `ASC` hoặc `DESC`. <br> 3. Hệ thống trả về kết quả phân trang dạng cursor (kết quả được cache). <br> 4. Khách hàng có thể gửi `cursor` để lấy trang tiếp theo. <br><br> **Duyệt danh sách (offset-based):** <br> 1. Khách hàng gửi yêu cầu xem danh sách chuyến tàu với tham số phân trang (`page`, `size`). <br> 2. Hệ thống trả về danh sách chuyến tàu phân trang, sắp xếp theo `departureTime` tăng dần (kết quả được cache). <br><br> **Xem chi tiết:** <br> 1. Khách hàng gửi yêu cầu xem chi tiết chuyến tàu theo `id`. <br> 2. Hệ thống trả về thông tin chi tiết chuyến tàu bao gồm tàu, tuyến đường và ga (kết quả được cache). |
| Hậu điều kiện (thành công)     | Không có thay đổi trạng thái. Đây là thao tác chỉ đọc.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Hậu điều kiện (thất bại)       | Không có thay đổi trạng thái. Dữ liệu trong hệ thống không bị ảnh hưởng.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Xử lý ngoại lệ                 | Chưa xác thực (thiếu hoặc sai access token) → Hệ thống trả về lỗi 401. <br> Tìm kiếm không có kết quả → Hệ thống trả về danh sách rỗng. <br> Chuyến tàu không tồn tại (xem chi tiết) → Hệ thống trả về lỗi `SCHEDULED_TRIP_NOT_FOUND`. <br> Cursor không hợp lệ (sai định dạng) → Hệ thống trả về lỗi `CURSOR_INVALID`. <br> Tham số không hợp lệ (phân trang, bộ lọc, giá trị sắp xếp) → Hệ thống trả về lỗi `VALIDATION_ERROR`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |

# Lược đồ tuần tự

```plantuml
@startuml UC-06
title UC-06: Search Scheduled Trips

actor "Khách hàng" as Actor
participant "Hệ thống" as API

== Tìm kiếm có bộ lọc (cursor-based) ==

Actor -> API: Filter(originStationId, destinationStationId, departureDate, status, availableOnly, minPrice, maxPrice, sortBy, sortDirection, cursor, size)
alt Chưa xác thực
    API --> Actor: 401 Unauthorized
else Tham số không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Cursor không hợp lệ
    API --> Actor: 400 + CURSOR_INVALID
else Không có kết quả
    API --> Actor: 200 + SliceResponse rỗng
else Có kết quả
    API -> API: Tìm kiếm và cache kết quả
    API --> Actor: 200 + SliceResponse<SearchScheduledTripsResponse>(id, departureTime, arrivalTime, status, durationMinutes, availableSeatCount, occupancyPercentage, train, route)
    opt Còn trang tiếp
        Actor -> API: Filter(cursor từ response trước)
        API --> Actor: 200 + trang tiếp theo
    end
end

== Duyệt danh sách (offset-based) ==

Actor -> API: List(page, size)
alt Phân trang không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Thành công
    API -> API: Truy vấn và cache kết quả
    API --> Actor: 200 + PageResponse<ScheduledTripResponse>(id, routeTemplateId, trainId, departureTime, arrivalTime, status, createdAt)
end

== Xem chi tiết ==

Actor -> API: GetById(id)
alt Chuyến tàu không tồn tại
    API --> Actor: 404 + SCHEDULED_TRIP_NOT_FOUND
else Tìm thấy
    API -> API: Truy vấn enriched và cache kết quả
    API --> Actor: 200 + ScheduledTripDetailResponse(id, routeTemplateId, trainId, departureTime, arrivalTime, status, createdAt, train, route)
end
@enduml
```

# Lược đồ hoạt động

```plantuml
@startuml UC-06-activity
title UC-06: Search Scheduled Trips - Activity Diagram

start

:Khách hàng gửi yêu cầu tra cứu;

if (Access token hợp lệ?) then (không)
  :Trả lỗi 401 Unauthorized;
  stop
else (có)
endif

switch (Loại tra cứu?)
case (Filter)
  if (Tham số bộ lọc hợp lệ?) then (không)
    :Trả lỗi 400 VALIDATION_ERROR;
    stop
  else (có)
  endif

  if (Có cursor?) then (có)
    if (Cursor hợp lệ?) then (không)
      :Trả lỗi 400 CURSOR_INVALID;
      stop
    else (có)
      :Giải mã cursor;
    endif
  else (không)
  endif

  :Tìm kiếm qua ScheduledTripSearchPort (cache);

  if (Có kết quả?) then (không)
    :Trả 200 + SliceResponse rỗng;
  else (có)
    :Mã hóa cursor tiếp theo (nếu hasNext);
    :Trả 200 + SliceResponse<SearchScheduledTripsResponse>;
  endif

case (List)
  if (Tham số phân trang hợp lệ?) then (không)
    :Trả lỗi 400 VALIDATION_ERROR;
    stop
  else (có)
  endif

  :Truy vấn danh sách từ repository (cache);
  :Trả 200 + PageResponse<ScheduledTripResponse>;

case (Detail)
  :Truy vấn enriched từ repository (cache);

  if (Chuyến tàu tồn tại?) then (không)
    :Trả lỗi 404 SCHEDULED_TRIP_NOT_FOUND;
    stop
  else (có)
    :Trả 200 + ScheduledTripDetailResponse;
  endif

endswitch

stop
@enduml
```

# Lược đồ lớp ý niệm

```plantuml
@startuml UC-06-class
title UC-06: Search Scheduled Trips - Conceptual Class Diagram

class "ScheduledTrip" as ST {
  - id: UUID
  - routeTemplateId: UUID
  - trainId: UUID
  - departureTime: Instant
  - arrivalTime: Instant
  - status: ScheduledTripStatus
  - createdAt: Instant
  - deletedAt: Instant
}

enum "ScheduledTripStatus" as STS {
  SCHEDULED
  BOARDING
  DEPARTED
  ARRIVED
  CANCELLED
}

class "SearchScheduledTripsRequest" as FilterReq {
  + originStationId: UUID
  + destinationStationId: UUID
  + departureDate: LocalDate
  + status: String
  + availableOnly: Boolean
  + minPrice: Long
  + maxPrice: Long
  + sortBy: String
  + sortDirection: String
  + cursor: String
  + size: int
}

class "GetScheduledTripsRequest" as ListReq {
  + page: int
  + size: int
}

class "SearchScheduledTripsResponse" as FilterRes {
  + id: UUID
  + departureTime: Instant
  + arrivalTime: Instant
  + status: ScheduledTripStatus
  + durationMinutes: long
  + availableSeatCount: long
  + occupancyPercentage: int
  + train: Train
  + route: Route
}

class "SearchScheduledTripsResponse.Train" as FilterTrain {
  + id: UUID
  + trainNumber: String
  + name: String
  + totalSeats: Integer
}

class "SearchScheduledTripsResponse.Route" as FilterRoute {
  + id: UUID
  + basePrice: long
  + currency: String
  + origin: Station
  + destination: Station
}

class "SearchScheduledTripsResponse.Station" as FilterStation {
  + id: UUID
  + code: String
  + name: String
  + city: String
}

class "ScheduledTripResponse" as ListRes {
  + id: UUID
  + routeTemplateId: UUID
  + trainId: UUID
  + departureTime: Instant
  + arrivalTime: Instant
  + status: ScheduledTripStatus
  + createdAt: Instant
}

class "ScheduledTripDetailResponse" as DetailRes {
  + id: UUID
  + routeTemplateId: UUID
  + trainId: UUID
  + departureTime: Instant
  + arrivalTime: Instant
  + status: ScheduledTripStatus
  + createdAt: Instant
  + train: Train
  + route: Route
}

class "ScheduledTripDetailResponse.Train" as DetailTrain {
  + id: UUID
  + trainNumber: String
  + name: String
  + totalSeats: int
}

class "ScheduledTripDetailResponse.Route" as DetailRoute {
  + id: UUID
  + basePrice: long
  + currency: String
  + origin: Station
  + destination: Station
}

class "ScheduledTripDetailResponse.Station" as DetailStation {
  + id: UUID
  + code: String
  + name: String
  + city: String
}

ST --> STS

FilterRes *-- FilterTrain
FilterRes *-- FilterRoute
FilterRoute *-- FilterStation

DetailRes *-- DetailTrain
DetailRes *-- DetailRoute
DetailRoute *-- DetailStation
@enduml
```

# Phân rã thành phần PM

## Controller: `ScheduledTripController`

- **Nhiệm vụ**: Nhận HTTP request từ khách hàng, xác thực đầu vào, ủy thác cho
  UseCase tương ứng.

**Endpoint 1 — Tìm kiếm có bộ lọc:**

- **Endpoint**: `GET /api/v1/scheduled-trips:filter`
- **Input**: `SearchScheduledTripsRequest` —
  `{ originStationId?, destinationStationId?, departureDate?, status?, availableOnly?, minPrice?, maxPrice?, sortBy?, sortDirection?, cursor?, size }`
- **Output thành công**: `200` + `SliceResponse<SearchScheduledTripsResponse>`
- **Output lỗi**: `400` + `JsendResponse` —
  `{ errorCode: VALIDATION_ERROR | CURSOR_INVALID, message }`

**Endpoint 2 — Duyệt danh sách:**

- **Endpoint**: `GET /api/v1/scheduled-trips`
- **Input**: `GetScheduledTripsRequest` — `{ page, size }`
- **Output thành công**: `200` + `PageResponse<ScheduledTripResponse>`
- **Output lỗi**: `400` + `JsendResponse` —
  `{ errorCode: VALIDATION_ERROR, message }`

**Endpoint 3 — Xem chi tiết:**

- **Endpoint**: `GET /api/v1/scheduled-trips/{id}`
- **Input**: `id` (UUID path variable)
- **Output thành công**: `200` + `ScheduledTripDetailResponse`
- **Output lỗi**: `404` + `JsendResponse` —
  `{ errorCode: SCHEDULED_TRIP_NOT_FOUND, message }`

## UseCase

**SearchScheduledTripsUseCase:**

- **Nhiệm vụ**: Tìm kiếm chuyến tàu với bộ lọc, phân trang cursor-based, cache
  kết quả.
- **Input**: `SearchScheduledTripsQuery` —
  `{ originStationId?, destinationStationId?, departureDate?, status?, availableOnly, minPrice?, maxPrice?, sortBy, sortDirection, cursor?, size }`
- **Output**: `SliceResponse<SearchScheduledTripsResponse>`
- **Gọi đến**:
    - `ScheduledTripSearchPort.search(query, cursor)` — tìm kiếm JDBC với bộ lọc
    - `CursorCodec.decode/encode()` — giải mã/mã hóa cursor
- **Cache**: `scheduledTripFilter`, key = `st-filter:{cacheKey}`

**GetScheduledTripsUseCase:**

- **Nhiệm vụ**: Truy vấn danh sách chuyến tàu phân trang offset-based, cache kết
  quả.
- **Input**: `GetScheduledTripsQuery` — `{ page, size }`
- **Output**: `PageResponse<ScheduledTripResponse>`
- **Gọi đến**:
    - `ScheduledTripRepository.findAllSummaries(page, size, sort)` — truy vấn
      danh sách
- **Cache**: `scheduledTripList`, key = `st-list:{page}:{size}`

**GetScheduledTripByIdUseCase:**

- **Nhiệm vụ**: Truy vấn chi tiết chuyến tàu theo ID, cache kết quả.
- **Input**: `GetScheduledTripByIdQuery` — `{ scheduledTripId }`
- **Output**: `Result<ScheduledTripDetailResponse, ScheduledTripError>`
- **Gọi đến**:
    - `ScheduledTripRepository.findEnrichedById(id)` — truy vấn enriched summary
- **Cache**: `scheduledTripById`, key = `st:{scheduledTripId}`, trừ khi failure

## Repository: `ScheduledTripRepository`

- **Nhiệm vụ**: Truy xuất domain entity `ScheduledTrip` và các projection
  summary.
- **Phương thức liên quan đến UC**:
    - `findAllSummaries(page, size, sort): PageResponse<ScheduledTripSummary>` —
      danh sách chuyến tàu phân trang
    - `findEnrichedById(id): Optional<ScheduledTripEnrichedSummary>` — chi tiết
      chuyến tàu kèm thông tin tàu, tuyến đường, ga

## Port: `ScheduledTripSearchPort`

- **Lớp**: Application layer (port interface), implemented bởi
  `ScheduledTripSearchReader` (infrastructure, JDBC).
- **Nhiệm vụ**: Tìm kiếm chuyến tàu với bộ lọc phức tạp, hỗ trợ cursor-based
  pagination và sắp xếp đa trường.
- **Phương thức liên quan đến UC**:
    - `search(query, cursor): SliceResponse<ScheduledTripEnrichedSummary>` — tìm
      kiếm JDBC với các bộ lọc (ga đi/đến, ngày, trạng thái, giá, ghế trống) và
      cursor

## Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-06-internal
title UC-06: Search Scheduled Trips - Internal Sequence

actor "Khách hàng" as Actor
participant "ScheduledTripController" as CTL
participant "SearchScheduledTripsUseCase" as SEARCH_UC
participant "GetScheduledTripsUseCase" as LIST_UC
participant "GetScheduledTripByIdUseCase" as DETAIL_UC
participant "ScheduledTripSearchPort" as SEARCH_PORT
participant "ScheduledTripRepository" as REPO
participant "CursorCodec" as CURSOR
database "DB" as DB

== Filter: GET /api/v1/scheduled-trips:filter ==

Actor -> CTL: GET /api/v1/scheduled-trips:filter (params)
CTL -> SEARCH_UC: execute(SearchScheduledTripsQuery)

opt cursor != null
    SEARCH_UC -> CURSOR: decode(cursor)
    CURSOR --> SEARCH_UC: SearchScheduledTripsCursor
end

SEARCH_UC -> SEARCH_PORT: search(query, cursor)
SEARCH_PORT -> DB: SELECT ... JOIN ... WHERE filters ORDER BY sort LIMIT size+1
DB --> SEARCH_PORT: List<ScheduledTripEnrichedSummary>
SEARCH_PORT --> SEARCH_UC: SliceResponse<ScheduledTripEnrichedSummary>

opt hasNext
    SEARCH_UC -> CURSOR: encode(nextCursor)
    CURSOR --> SEARCH_UC: cursorString
end

SEARCH_UC --> CTL: SliceResponse<SearchScheduledTripsResponse>
CTL --> Actor: 200 + JsendResponse(SliceResponse)

== List: GET /api/v1/scheduled-trips ==

Actor -> CTL: GET /api/v1/scheduled-trips (page, size)
CTL -> LIST_UC: execute(GetScheduledTripsQuery)
LIST_UC -> REPO: findAllSummaries(page, size, sort)
REPO -> DB: SELECT ... FROM scheduled_trips ORDER BY departure_time, id
DB --> REPO: PageResponse<ScheduledTripSummary>
REPO --> LIST_UC: PageResponse<ScheduledTripSummary>
LIST_UC --> CTL: PageResponse<ScheduledTripResponse>
CTL --> Actor: 200 + JsendResponse(PageResponse)

== Detail: GET /api/v1/scheduled-trips/{id} ==

Actor -> CTL: GET /api/v1/scheduled-trips/{id}
CTL -> DETAIL_UC: execute(GetScheduledTripByIdQuery)
DETAIL_UC -> REPO: findEnrichedById(id)
REPO -> DB: SELECT ... JOIN trains, route_templates, stations WHERE id = ?
DB --> REPO: Optional<ScheduledTripEnrichedSummary>
REPO --> DETAIL_UC: Optional<ScheduledTripEnrichedSummary>

alt Không tìm thấy
    DETAIL_UC --> CTL: Result.failure(ScheduledTripNotFound)
    CTL --> Actor: 404 + SCHEDULED_TRIP_NOT_FOUND
else Tìm thấy
    DETAIL_UC --> CTL: Result.success(ScheduledTripDetailResponse)
    CTL --> Actor: 200 + JsendResponse(ScheduledTripDetailResponse)
end
@enduml
```

## Giao diện

### Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Tra cứu chuyến tàu
  ..
  {
    Ga đi          | ^Chọn ga đi^
    Ga đến         | ^Chọn ga đến^
    Ngày khởi hành | "dd/mm/yyyy"
    Chỉ còn ghế    | [X]
  }
  [Tìm chuyến]
  ==
  {#
    Chuyến       | Khởi hành     | Đến           | Thời gian | Giá       | Ghế trống
    SE1          | 06:00 SGN     | 12:30 DNA     | 6h30      | 500,000đ  | 45
    SE3          | 19:00 SGN     | 07:00+1 HAN   | 12h       | 850,000đ  | 12
    SE5          | 22:00 SGN     | 04:30+1 DNA   | 6h30      | 480,000đ  | 0
  }
  ..
  [< Trước] | Trang 1/3 | [Tiếp >]
}
@endsalt
```

### Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

# Bảng tham chiếu dò vết

| Use Case | Controller              | Endpoint                             | UseCase                     | Repository / Port                          | Table                                              |
| -------- | ----------------------- | ------------------------------------ | --------------------------- | ------------------------------------------ | -------------------------------------------------- |
| UC-06    | ScheduledTripController | `GET /api/v1/scheduled-trips:filter` | SearchScheduledTripsUseCase | ScheduledTripSearchPort.search()           | scheduled_trips, route_templates, trains, stations |
|          | ScheduledTripController | `GET /api/v1/scheduled-trips`        | GetScheduledTripsUseCase    | ScheduledTripRepository.findAllSummaries() | scheduled_trips                                    |
|          | ScheduledTripController | `GET /api/v1/scheduled-trips/{id}`   | GetScheduledTripByIdUseCase | ScheduledTripRepository.findEnrichedById() | scheduled_trips, route_templates, trains, stations |

# Tiêu chí kiểm thử

| Tiêu chí              | Phép thử                                                                   | Kết quả mong đợi                                                  | Ghi chú                                              |
| --------------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------- | ---------------------------------------------------- |
| Toàn diện (coverage)  | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ                             | Rà soát chéo giữa mục 2 và mục 3                     |
| Nhất quán             | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–6                                  | Đặc biệt kiểm tra tên DTO trong mục 5–6              |
| Truy vết              | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.5)     | Mọi tương tác trong sequence đều có entry                         | Kiểm tra không thiếu endpoint/method                 |
| Filter — happy path   | Gửi request filter với bộ lọc hợp lệ (origin, destination, date)           | 200 + SliceResponse chứa kết quả, nextCursor nếu hasNext          | Kiểm tra cache hit lần gọi thứ 2                     |
| Filter — empty        | Gửi request filter với bộ lọc không khớp chuyến nào                        | 200 + SliceResponse rỗng (content=[], hasNext=false)              | Không lỗi, message = "No scheduled trips matched..." |
| Filter — bad cursor   | Gửi request filter với cursor sai định dạng                                | 400 + CURSOR_INVALID                                              |                                                      |
| Filter — bad params   | Gửi request filter với sortBy không hợp lệ hoặc minPrice < 0               | 400 + VALIDATION_ERROR                                            |                                                      |
| List — happy path     | Gửi request list với page=0, size=20                                       | 200 + PageResponse chứa danh sách, sắp xếp theo departureTime ASC | Kiểm tra cache hit                                   |
| List — bad pagination | Gửi request list với size=0 hoặc size > 100                                | 400 + VALIDATION_ERROR                                            |                                                      |
| Detail — found        | Gửi request detail với id chuyến tàu tồn tại                               | 200 + ScheduledTripDetailResponse kèm train, route, stations      | Kiểm tra cache hit (trừ khi failure)                 |
| Detail — not found    | Gửi request detail với id không tồn tại                                    | 404 + SCHEDULED_TRIP_NOT_FOUND                                    | Kết quả failure không được cache                     |
| Unauthenticated       | Gửi bất kỳ request nào không có access token                               | 401 Unauthorized                                                  | Xử lý bởi Spring Security filter chain               |

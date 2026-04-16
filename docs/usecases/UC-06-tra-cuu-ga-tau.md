## UC-06: Tra cứu ga tàu

### 1. Mô tả use case

| Mục                            | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phụ thuộc                      | UC-02: Đăng nhập                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Mục đích                       | Khách hàng cần tra cứu thông tin ga tàu để phục vụ các bước tiếp theo như chọn tuyến, xem lịch chuyến hoặc đặt vé. PM cung cấp ba cách tra cứu: tìm kiếm theo từ khóa (auto-complete, có cache), duyệt danh sách phân trang và xem chi tiết một ga cụ thể.                                                                                                                                                                                                                                                                                                                                                                                                     |
| Mô tả                          | Khách hàng tra cứu thông tin ga tàu bằng nhiều cách: tìm kiếm theo từ khóa, duyệt danh sách phân trang, hoặc xem chi tiết một ga cụ thể.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Actor chính                    | Khách hàng đã đăng nhập                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Actor liên quan                | Không                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Tiền điều kiện                 | Khách hàng đã đăng nhập và có access token hợp lệ.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Dãy lệnh thực hiện bình thường | **Tìm kiếm theo từ khóa:** <br> 1. Khách hàng gửi từ khóa tìm kiếm (tên ga, mã ga, hoặc thành phố) kèm giới hạn số kết quả (`limit`, tối đa 20). <br> 2. Hệ thống tra cứu bằng ILIKE trên tổ hợp mã ga, tên ga, thành phố, ưu tiên kết quả khớp prefix. <br> 3. Hệ thống trả về danh sách ga phù hợp (kết quả được cache). <br><br> **Duyệt danh sách:** <br> 1. Khách hàng gửi yêu cầu xem danh sách ga với tham số phân trang (`page`, `size`). <br> 2. Hệ thống trả về danh sách ga phân trang, sắp xếp theo mã ga. <br><br> **Xem chi tiết:** <br> 1. Khách hàng gửi yêu cầu xem chi tiết một ga theo `id`. <br> 2. Hệ thống trả về thông tin chi tiết ga. |
| Hậu điều kiện (thành công)     | Không có thay đổi trạng thái. Đây là thao tác chỉ đọc.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Hậu điều kiện (thất bại)       | Không có thay đổi trạng thái. Hệ thống trả về lỗi hoặc danh sách rỗng tùy trường hợp.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Xử lý ngoại lệ                 | Chưa xác thực (thiếu hoặc sai access token) → Hệ thống trả về lỗi 401. <br> Tìm kiếm không có kết quả → Hệ thống trả về danh sách rỗng kèm thông báo "No stations matched your search.". <br> Ga không tồn tại (xem chi tiết) → Hệ thống trả về lỗi `STATION_NOT_FOUND`. <br> Tham số phân trang không hợp lệ → Hệ thống trả về lỗi `VALIDATION_ERROR`.                                                                                                                                                                                                                                                                                                        |

### 2. Lược đồ tuần tự

```plantuml
@startuml UC-06
title UC-06: Search Stations

actor "Customer" as Actor
participant "System" as API

== Search by keyword ==

Actor -> API: SearchStations(q, limit)
alt No results
    API --> Actor: 200 + empty list + "No stations matched your search."
else Results found
    API --> Actor: 200 + StationSearchResponse[](id, code, name, city)
end

== Browse list (paginated) ==

Actor -> API: GetStations(page, size)
alt Invalid pagination
    API --> Actor: 400 + VALIDATION_ERROR
else Success
    API --> Actor: 200 + PageResponse<StationResponse>(id, code, name, city, createdAt)
end

== View detail ==

Actor -> API: GetStationById(id)
alt Station not found
    API --> Actor: 404 + STATION_NOT_FOUND
else Station found
    API --> Actor: 200 + StationResponse(id, code, name, city, createdAt)
end
@enduml
```

### 3. Lược đồ hoạt động

```plantuml
@startuml UC-06-activity
title UC-06: Search Stations - Activity Diagram

start

if (Operation type?) then (Search)
  :Customer submits keyword (q) and limit;
  :Search stations via ILIKE on code||name||city;
  note right: Cached by (q, limit)
  if (Results found?) then (no)
    :Return 200 + empty list + message;
    stop
  else (yes)
  endif
  :Return 200 + StationSearchResponse[];
  stop

else if (List) then
  :Customer submits page, size;
  if (Valid pagination?) then (no)
    :Return 400 VALIDATION_ERROR;
    stop
  else (yes)
  endif
  :Query stations sorted by code;
  :Return 200 + PageResponse<StationResponse>;
  stop

else (Detail)
  :Customer submits station id;
  if (Station found?) then (no)
    :Return 404 STATION_NOT_FOUND;
    stop
  else (yes)
  endif
  :Return 200 + StationResponse;
  stop
endif

@enduml
```

### 5. Lược đồ lớp ý niệm

```plantuml
@startuml UC-06-class
title UC-06: Search Stations - Conceptual Class Diagram

class "Station" as Station {
  - id: StationId
  - code: StationCode
  - name: String
  - city: String
  - createdAt: Instant
}

class "StationCode" as StationCode {
  - value: String
}

class "SearchStationsRequest" as SearchReq {
  + q: String
  + limit: int
}

class "GetStationsRequest" as ListReq {
  + page: int
  + size: int
}

class "StationSearchResponse" as SearchRes {
  + id: UUID
  + code: String
  + name: String
  + city: String
}

class "StationResponse" as DetailRes {
  + id: UUID
  + code: String
  + name: String
  + city: String
  + createdAt: Instant
}

Station *-- StationCode
@enduml
```

### 6. Phân rã thành phần PM

#### 6.1 Controller: `StationController`

- **Nhiệm vụ**: Nhận yêu cầu tra cứu ga tàu qua ba endpoint và ủy thác cho use
  case tương ứng.
- **Endpoint tìm kiếm**: `GET /api/v1/stations/search`
    - Input: `SearchStationsRequest` —
      `{ q: String, limit: int (1–20, default 10) }`
    - Output thành công: `200 OK` + `StationSearchResponse[]`
    - Output rỗng: `200 OK` + `[]` + message "No stations matched your search."
- **Endpoint danh sách**: `GET /api/v1/stations`
    - Input: `GetStationsRequest` —
      `{ page: int (≥0), size: int (1–100, default 20) }`
    - Output thành công: `200 OK` + `PageResponse<StationResponse>`
    - Output lỗi: `400` + `JsendResponse`
- **Endpoint chi tiết**: `GET /api/v1/stations/{id}`
    - Input: `id` (UUID path variable)
    - Output thành công: `200 OK` + `StationResponse`
    - Output lỗi: `404` + `JsendResponse`

#### 6.2 UseCase: `SearchStationsUseCase` (tìm kiếm)

- **Nhiệm vụ**: Tra cứu ga tàu theo từ khóa thông qua port tìm kiếm chuyên dụng.
  Kết quả được cache.
- **Input**: `SearchStationsQuery` — `{ keyword: String, limit: int }`
- **Output**: `List<StationSearchResponse>`
- **Gọi đến**:
    - `StationSearchPort.search(query)` — tra cứu ILIKE trên tổ hợp code, name,
      city
- **Cache**:
  `@Cacheable(cacheNames = "stationSearch", key = "'station-search:' + #query.cacheKey()")`
- **Phát sinh sự kiện**: Không

#### 6.3 UseCase: `GetStationsUseCase` (danh sách)

- **Nhiệm vụ**: Trả về danh sách ga phân trang, sắp xếp theo mã ga rồi theo id.
- **Input**: `GetStationsQuery` — `{ page: int, size: int }`
- **Output**: `PageResponse<StationResponse>`
- **Gọi đến**:
    - `StationRepository.findAllSummaries(page, size, sort)` — truy vấn phân
      trang
- **Phát sinh sự kiện**: Không

#### 6.4 UseCase: `GetStationByIdUseCase` (chi tiết)

- **Nhiệm vụ**: Trả về chi tiết một ga theo ID.
- **Input**: `GetStationByIdQuery` — `{ stationId: UUID }`
- **Output**: `Result<StationResponse, StationError>`
- **Gọi đến**:
    - `StationRepository.findSummaryById(stationId)` — truy vấn projection chi
      tiết
- **Phát sinh sự kiện**: Không

#### 6.5 Repository: `StationRepository`

- **Nhiệm vụ**: Truy xuất dữ liệu ga tàu từ cơ sở dữ liệu.
- **Phương thức liên quan đến UC**:
    - `findAllSummaries(page, size, sort): PageResponse<StationSummary>` — phân
      trang danh sách ga
    - `findSummaryById(stationId): Optional<StationSummary>` — chi tiết một ga
- **Table**: `stations`

#### 6.6 Port: `StationSearchPort`

- **Nhiệm vụ**: Định nghĩa hợp đồng tìm kiếm ga tàu ở tầng application. Cài đặt
  nằm trong tầng infrastructure, sử dụng JDBC trực tiếp với ILIKE trên tổ hợp
  `code || name || city`, ưu tiên kết quả khớp prefix.
- **Phương thức liên quan đến UC**:
    - `search(query): List<StationSummary>` — trả về danh sách ga phù hợp từ
      khóa
- **Implementation**: `StationSearchReader` (JDBC `NamedParameterJdbcTemplate`)

#### 6.7 Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-06-internal
title UC-06: Search Stations - Internal Sequence

actor "Customer" as Actor
participant "StationController" as CTL
participant "SearchStationsUseCase" as SEARCH_UC
participant "GetStationsUseCase" as LIST_UC
participant "GetStationByIdUseCase" as DETAIL_UC
participant "StationSearchPort" as SEARCH_PORT
participant "StationRepository" as REPO
database "DB" as DB

== Search by keyword ==

Actor -> CTL: GET /api/v1/stations/search?q=&limit=
CTL -> SEARCH_UC: execute(SearchStationsQuery(keyword, limit))
SEARCH_UC -> SEARCH_PORT: search(query)
note right: @Cacheable("stationSearch")
SEARCH_PORT -> DB: SELECT ... WHERE code||name||city ILIKE ? LIMIT ?
DB --> SEARCH_PORT: List<StationSummary>
SEARCH_PORT --> SEARCH_UC: List<StationSummary>
SEARCH_UC --> CTL: List<StationSearchResponse>
CTL --> Actor: 200 + JsendResponse(StationSearchResponse[])

== Browse list ==

Actor -> CTL: GET /api/v1/stations?page=&size=
CTL -> LIST_UC: execute(GetStationsQuery(page, size))
LIST_UC -> REPO: findAllSummaries(page, size, sort=[code ASC, id ASC])
REPO -> DB: SELECT stations paged
DB --> REPO: PageResponse<StationSummary>
REPO --> LIST_UC: PageResponse<StationSummary>
LIST_UC --> CTL: PageResponse<StationResponse>
CTL --> Actor: 200 + JsendResponse(PageResponse)

== View detail ==

Actor -> CTL: GET /api/v1/stations/{id}
CTL -> DETAIL_UC: execute(GetStationByIdQuery(stationId))
DETAIL_UC -> REPO: findSummaryById(stationId)
REPO -> DB: SELECT station by id
DB --> REPO: Optional<StationSummary>
REPO --> DETAIL_UC: Optional<StationSummary>
alt Station not found
    DETAIL_UC --> CTL: Result.failure(StationNotFound)
else Station found
    DETAIL_UC --> CTL: Result.success(StationResponse)
end
CTL --> Actor: 200 + JsendResponse(StationResponse)
@enduml
```

#### 6.8 Giao diện

##### 6.8.1 Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Tra cứu ga tàu
  ..
  Tìm kiếm | "Nhập tên ga, mã ga hoặc thành phố..." | [Tìm]
  ==
  {#
    Mã ga | Tên ga            | Thành phố
    SGN   | Ga Sài Gòn        | TP. Hồ Chí Minh
    HAN   | Ga Hà Nội         | Hà Nội
    DNA   | Ga Đà Nẵng        | Đà Nẵng
    NTR   | Ga Nha Trang      | Nha Trang
    HUE   | Ga Huế            | Huế
  }
  ..
  [< Trước] | Trang 1/5 | [Tiếp >]
}
@endsalt
```

##### 6.8.2 Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

### 7. Bảng tham chiếu dò vết

| Use Case | Controller        | Endpoint                      | UseCase               | Repository / Port                      | Table      |
| -------- | ----------------- | ----------------------------- | --------------------- | -------------------------------------- | ---------- |
| UC-06    | StationController | `GET /api/v1/stations/search` | SearchStationsUseCase | `StationSearchPort.search()`           | `stations` |
| UC-06    | StationController | `GET /api/v1/stations`        | GetStationsUseCase    | `StationRepository.findAllSummaries()` | `stations` |
| UC-06    | StationController | `GET /api/v1/stations/{id}`   | GetStationByIdUseCase | `StationRepository.findSummaryById()`  | `stations` |

### 8. Tiêu chí kiểm thử

| Tiêu chí             | Phép thử                                                                   | Kết quả mong đợi                          | Ghi chú                              |
| -------------------- | -------------------------------------------------------------------------- | ----------------------------------------- | ------------------------------------ |
| Toàn diện (coverage) | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ     | Rà soát chéo giữa mục 2 và mục 3     |
| Nhất quán            | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–6          | Đặc biệt kiểm tra tên trong mục 5–6  |
| Truy vết             | Đối chiếu bảng tham chiếu (mục 7) với lược đồ tuần tự nội bộ (mục 6.5)     | Mọi tương tác trong sequence đều có entry | Kiểm tra không thiếu endpoint/method |

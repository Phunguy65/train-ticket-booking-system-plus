# UC-02: Đăng nhập

## 1. Mô tả use case

| Mục                         | Nội dung                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Quan hệ UC                  | **`<<includes>>` (bắt buộc)**: Không <br> **`<<extends>>` (tùy chọn)**: Không <br> **Generalization**: Không |
| Mục đích                    | Khách hàng đã có tài khoản cần xác thực để truy cập các chức năng yêu cầu đăng nhập như đặt vé, xem đặt vé và quản lý hồ sơ. PM xác minh thông tin đăng nhập, tạo cặp token truy cập phiên làm việc và trả về hồ sơ người dùng cơ bản. |
| Mô tả                       | Khách hàng đăng nhập bằng email và mật khẩu để nhận cặp token truy cập phiên làm việc và thông tin hồ sơ cơ bản. |
| Actor chính                 | Khách hàng đã có tài khoản |
| Actor liên quan             | Không |
| Tiền điều kiện              | Khách hàng đã có tài khoản đang hoạt động trong hệ thống (UC-01 đã hoàn thành). |
| Luồng chính                 | 1. Khách hàng gửi yêu cầu đăng nhập gồm `email` và `password`. <br> 2. Hệ thống kiểm tra dữ liệu đầu vào hợp lệ. <br> 3. Hệ thống tìm tài khoản theo email. <br> 4. Hệ thống đối chiếu mật khẩu gửi lên với mật khẩu đã băm trong hệ thống. <br> 5. Hệ thống tạo `accessToken` và `refreshToken` cho phiên đăng nhập. <br> 6. Hệ thống lưu refresh token mới. <br> 7. Hệ thống trả về token cùng thông tin người dùng. |
| Hậu điều kiện (thành công)  | Khách hàng nhận được cặp token hợp lệ để sử dụng các API yêu cầu xác thực. Một bản ghi refresh token mới được lưu trong hệ thống. |
| Hậu điều kiện (thất bại)    | Không có token nào được tạo hoặc lưu. Dữ liệu người dùng không thay đổi. |
| Luồng ngoại lệ              | Email không tồn tại hoặc mật khẩu không đúng → Hệ thống trả về lỗi `USER_INVALID_CREDENTIALS`. <br> Dữ liệu đầu vào không hợp lệ, thiếu trường bắt buộc hoặc email sai định dạng → Hệ thống trả về lỗi `VALIDATION_ERROR`. |

## 2. Lược đồ Use Case

```plantuml
@startuml UC-02-usecase
title UC-02: Đăng nhập - Use Case Diagram

left to right direction

actor "Khách hàng\nđã có tài khoản" as Customer

rectangle "Hệ thống đặt vé tàu" {
  usecase "UC-02\nĐăng nhập" as UC02
}

Customer --> UC02
@enduml
```

## 3. Lược đồ tuần tự

```plantuml
@startuml UC-02
title UC-02: Đăng nhập

actor "Khách hàng" as Actor
participant "Hệ thống" as API

Actor -> API: Login(email, password)
alt Dữ liệu đầu vào không hợp lệ
    API --> Actor: 400 + VALIDATION_ERROR
else Email không tồn tại hoặc mật khẩu sai
    API --> Actor: 401 + USER_INVALID_CREDENTIALS
else Đăng nhập thành công
    API -> API: Xác thực thông tin đăng nhập
    API -> API: Tạo cặp token (accessToken, refreshToken)
    API --> Actor: 200 + LoginResultResponse(accessToken, refreshToken, user)
end
@enduml
```

## 4. Lược đồ hoạt động

```plantuml
@startuml UC-02-activity
title UC-02: Đăng nhập - Activity Diagram

start

:Khách hàng gửi email và mật khẩu;

if (Dữ liệu đầu vào hợp lệ?) then (không)
  :Trả 400 VALIDATION_ERROR;
  stop
else (có)
endif

if (Tìm thấy tài khoản theo email?) then (không)
  :Trả 401 USER_INVALID_CREDENTIALS;
  stop
else (có)
endif

if (Mật khẩu khớp với hash?) then (không)
  :Trả 401 USER_INVALID_CREDENTIALS;
  stop
else (có)
endif

:Tạo accessToken và refreshToken;
:Lưu refresh token hash vào DB;
:Trả 200 + LoginResultResponse;

stop
@enduml
```

## 5. Lược đồ trạng thái

```plantuml
@startuml UC-02-state
title UC-02: Đăng nhập - State Diagram

[*] --> ChuaXacThuc
ChuaXacThuc --> PhienHoatDong: Đăng nhập thành công\n(token pair được tạo)
PhienHoatDong --> [*]
@enduml
```

## 6. Lược đồ lớp ý niệm

```plantuml
@startuml UC-02-class
title UC-02: Đăng nhập - Conceptual Class Diagram

class "User" as User {
  - id: UUID
  - email: EmailAddress
  - passwordHash: PasswordHash
  - fullName: PersonName
  - role: UserRole
}

class "EmailAddress" as EmailAddress {
  - value: String
}

class "PasswordHash" as PasswordHash {
  - value: String
}

class "LoginRequest" as LoginRequest {
  + email: String
  + password: String
}

class "LoginResultResponse" as LoginResultResponse {
  + accessToken: String
  + refreshToken: String
  + user: UserResponse
}

class "UserResponse" as UserResponse {
  + id: UUID
  + email: String
  + fullName: String
  + phone: String
  + dateOfBirth: LocalDate
  + gender: String
  + idDocumentNumber: String
  + addressLine: String
  + role: String
  + createdAt: Instant
}

User *-- EmailAddress
User *-- PasswordHash
LoginRequest ..> User : xác thực
User ..> LoginResultResponse : trả kết quả
LoginResultResponse o-- UserResponse
@enduml
```

## 7. Phân rã thành phần PM

### 7.1 Controller: `AuthController`

- **Nhiệm vụ**: Nhận yêu cầu đăng nhập, kiểm tra định dạng payload và chuyển
  tiếp sang lớp nghiệp vụ để xác thực.
- **Endpoint**: `POST /api/v1/auth/login`
- **Input**: `LoginRequest` — `{ email: String, password: String }`
- **Output thành công**: `200 OK` + `LoginResultResponse` —
  `{ accessToken, refreshToken, user }`
- **Output lỗi**: `400/401` + `JsendResponse` — `{ errorCode, message }`

### 7.2 UseCase: `LoginUserUseCase`

- **Nhiệm vụ**: Tìm người dùng theo email, đối chiếu mật khẩu và tạo cặp token
  mới khi xác thực thành công.
- **Input**: `LoginCommand` — `{ email: String, password: String }`
- **Output**: `Result<LoginResultResponse, UserError>`
- **Gọi đến**:
    - `UserRepository.findByEmail(email)` — tìm tài khoản theo email
    - `PasswordEncoder.matches(rawPassword, passwordHash)` — xác thực mật khẩu
    - `RefreshTokenManager.generateAndSaveTokens(user)` — tạo và lưu cặp token
      cho phiên đăng nhập
- **Phát sinh sự kiện**: Không

### 7.3 Repository: `UserRepository`

- **Nhiệm vụ**: Truy xuất domain entity `User` để phục vụ xác thực.
- **Phương thức liên quan đến UC**:
    - `findByEmail(email): Optional<User>` — tìm người dùng theo email đang hoạt
      động
- **Table**: `users`

### 7.4 Thiết kế cơ sở dữ liệu

#### 7.4.1 ERD

- **Tham chiếu ERD**: Bảng `users` và `refresh_tokens` trong schema chung của hệ thống
- **Bảng/View liên quan**: `users`, `refresh_tokens`

#### 7.4.2 Stored Procedure

Không sử dụng Stored Procedure cho UC này.

#### 7.4.3 Trigger

Không sử dụng Trigger cho UC này.

### 7.5 Port: `PasswordEncoder`, `RefreshTokenManager`

- **Nhiệm vụ**: Hỗ trợ xác thực mật khẩu và quản lý vòng đời token cho phiên
  đăng nhập.
- **Phương thức liên quan đến UC**:
    - `PasswordEncoder.matches(rawPassword, passwordHash): boolean` — kiểm tra
      mật khẩu hợp lệ
    - `RefreshTokenManager.generateAndSaveTokens(user): TokenPair` — tạo
      accessToken + refreshToken, lưu hash của refreshToken vào DB

### 7.6 Lược đồ tuần tự nội bộ PM

```plantuml
@startuml UC-02-internal
title UC-02: Đăng nhập - Internal Sequence

actor "Khách hàng" as Actor
participant "AuthController" as CTL
participant "LoginUserUseCase" as UC
participant "UserRepository" as REPO
database "DB" as DB
participant "PasswordEncoder" as ENCODER
participant "RefreshTokenManager" as TOKEN

Actor -> CTL: POST /api/v1/auth/login (email, password)
CTL -> UC: execute(LoginCommand(email, password))
UC -> REPO: findByEmail(email)
REPO -> DB: SELECT user by email
DB --> REPO: Optional<User>
REPO --> UC: Optional<User>

alt User not found
    UC --> CTL: Result.failure(InvalidCredentials)
else User found
    UC -> ENCODER: matches(password, user.passwordHash)
    ENCODER --> UC: boolean
    alt Password mismatch
        UC --> CTL: Result.failure(InvalidCredentials)
    else Password matches
        UC -> TOKEN: generateAndSaveTokens(user)
        TOKEN -> DB: INSERT INTO refresh_tokens (...)
        DB --> TOKEN: token record saved
        TOKEN --> UC: TokenPair(accessToken, refreshToken)
        UC --> CTL: Result.success(LoginResultResponse)
    end
end

CTL --> Actor: 200 + JsendResponse(LoginResultResponse)
@enduml
```

### 7.7 Giao diện

#### 7.7.1 Giao diện mẫu

```plantuml
@startsalt
{+
  <b>Đăng nhập
  ..
  Email      | "                              "
  Mật khẩu   | "                              "
  ..
  Chưa có tài khoản? <color:Blue>Đăng ký
  ==
  [Đăng nhập]
}
@endsalt
```

| Control            | Nhiệm vụ                                          | Inputs                  | Outputs                                    | Gọi API                      |
| ------------------ | ------------------------------------------------- | ----------------------- | ------------------------------------------ | ---------------------------- |
| `LoginForm`        | Thu thập thông tin đăng nhập và validate phía client | `email`, `password`     | `LoginResultResponse` hoặc `Error`         | `POST /api/v1/auth/login`    |
| `[Đăng nhập]` Button | Gửi form đăng nhập đến hệ thống                   | `LoginRequest(email, password)` | `200 + LoginResultResponse` hoặc lỗi | `POST /api/v1/auth/login`    |

#### 7.7.2 Giao diện ứng dụng

Chưa hiện thực. Sẽ bổ sung ảnh chụp màn hình khi hoàn thành.

## 8. Bảng tham chiếu dò vết

| Use Case | Controller     | Endpoint                  | UseCase          | Repository                     | SP    | Table                     |
| -------- | -------------- | ------------------------- | ---------------- | ------------------------------ | ----- | ------------------------- |
| UC-02    | AuthController | `POST /api/v1/auth/login` | LoginUserUseCase | `UserRepository.findByEmail()` | Không | `users`, `refresh_tokens` |

## 9. Tiêu chí kiểm thử

### 9.1 Mức phân tích

| Tiêu chí             | Phép thử                                                                   | Kết quả mong đợi                          | Ghi chú                              |
| -------------------- | -------------------------------------------------------------------------- | ----------------------------------------- | ------------------------------------ |
| Toàn diện (coverage) | Đối chiếu Activity Diagram ↔ Sequence Diagram: mọi luồng đều được thể hiện | Không bỏ sót luồng chính lẫn ngoại lệ     | Rà soát chéo giữa mục 3 và mục 4     |
| Nhất quán            | Rà soát tên lớp, trạng thái, API giữa các lược đồ trong cùng UC            | Không mâu thuẫn giữa các mục 2–7          | Đặc biệt kiểm tra tên trong mục 6–7  |
| Truy vết             | Đối chiếu bảng tham chiếu (mục 8) với lược đồ tuần tự nội bộ (mục 7.6)     | Mọi tương tác trong sequence đều có entry | Kiểm tra không thiếu endpoint/method |

### 9.2 Mức thiết kế

| Tiêu chí      | Phép thử                                                                                | Kết quả mong đợi                                       | Ghi chú                                |
| ------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------ | -------------------------------------- |
| Chuẩn hóa     | Rà soát thiết kế AuthController, LoginUserUseCase, UserRepository, PasswordEncoder, RefreshTokenManager | Tuân thủ Clean Architecture, quy ước đặt tên và hợp đồng | Walkthrough/inspection                 |
| Testability   | Rà soát khả năng mock PasswordEncoder, RefreshTokenManager, UserRepository trong unit test | Có thể kiểm thử UseCase độc lập không cần DB thật       | Tất cả dependency là port/interface    |
| Modularity    | Rà soát ranh giới trách nhiệm: Controller chỉ validate + route, UseCase chỉ orchestrate, Repository chỉ persistence | Không trùng lặp trách nhiệm, coupling thấp             | Kiểm tra không có logic nghiệp vụ trong Controller |

### 9.3 Mức hiện thực

| Tiêu chí          | Phép thử                                                                                  | Kết quả mong đợi                                                    | Ghi chú                                    |
| ----------------- | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------------------ |
| Xử lý chính xác   | Test luồng chính (login thành công), luồng lỗi (email không tồn tại, password sai, validation fail) | 200 + LoginResultResponse đúng fields; 401 + USER_INVALID_CREDENTIALS; 400 + VALIDATION_ERROR | Unit test UseCase + integration test endpoint |
| Hiệu năng         | Benchmark endpoint POST /api/v1/auth/login với 100 concurrent requests                     | Response time p95 < 500ms trong điều kiện tải bình thường            | Ghi rõ môi trường test                     |
| Bảo mật           | Kiểm tra password không trả về trong response, timing-safe comparison, identical error message cho email sai và password sai | Không lộ password/hash, không phân biệt được email sai vs password sai từ response | Chống timing attack và user enumeration |

### 9.4 Bảng tiêu chí chất lượng theo chức năng

| Chức năng trong UC          | Tiêu chí mức Ý niệm                                                        | Tiêu chí mức Thiết kế                                                          | Tiêu chí mức Hiện thực                                                              |
| --------------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| Xác thực đăng nhập          | Đúng nhu cầu: khách hàng xác thực được bằng email/password đã đăng ký       | Luồng xử lý chuẩn hóa qua Controller→UseCase→Repository→Port, dễ test với mock | Unit test UseCase (3 cases: success, wrong email, wrong password), integration test endpoint |
| Tạo cặp token               | Phiên đăng nhập được thiết lập qua accessToken + refreshToken                | RefreshTokenManager port tách biệt, dễ thay đổi thuật toán JWT                  | Verify token pair trả về đúng format, refresh token hash được lưu DB                 |
| Chống user enumeration      | Không cho phép phân biệt email tồn tại hay không từ response                 | UseCase trả cùng error code cho cả 2 trường hợp (email sai, password sai)       | Test response body identical cho unknown email vs wrong password                      |

## 10. Yêu cầu phi chức năng

| Loại yêu cầu  | Nội dung                                                                                          | Nguồn gốc                                          |
| -------------- | ------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| Business       | Mỗi lần đăng nhập thành công tạo phiên mới; refresh token cũ bị vô hiệu hóa khi rotate           | Quy tắc bảo mật phiên làm việc                      |
| Operation      | Mật khẩu xác thực bằng BCrypt timing-safe comparison; endpoint cần rate limiting chống brute-force; error message không phân biệt email sai vs password sai | Chính sách bảo mật hệ thống                         |
| Development    | Email validate theo RFC 5322; response tuân thủ JSend format; refresh token lưu dạng SHA-256 hash  | Quy ước kỹ thuật nhóm phát triển                    |

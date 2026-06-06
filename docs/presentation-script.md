# Script Báo cáo Đồ án — Đảm bảo Chất lượng Phần mềm

> **Thời lượng mục tiêu:** 15–20 phút  
> **Người B (Phú):** Mở đầu + UC-07 → UC-12 + Kết luận  
> **Người A (Nguyên):** UC-01 → UC-06  
> **Quy ước:** 🎤 = Thuyết trình | 💻 = Thao tác demo/chạy test trên máy

---

## Cấu trúc trình bày mỗi Use Case

Mỗi UC trình bày theo 3 phần:

1. **Giới thiệu chức năng** (~30%): Mô tả luồng chính, actor, tiền điều kiện
2. **Chiến lược kiểm thử** (~30%): Kỹ thuật test áp dụng (EP, BVA, State Transition...), phân tầng test (unit/integration/stress/security)
3. **Demo kiểm thử** (~40%): Chạy test suite thực tế hoặc demo app + show kết quả

---

## Phần 1: Mở đầu (~3 phút) — Người B thuyết trình

### Slide 1: Trang bìa

🎤 **Người B:**

> Kính chào thầy và các bạn. Nhóm chúng em xin trình bày đồ án môn Đảm bảo chất lượng phần mềm với đề tài: **Ứng dụng đặt vé tàu trực tuyến**.
>
> Thành viên nhóm: Nguyễn Ngọc Phú — N22DCCN159 và Y Cao Nguyên Byă — N22DCCN200.

*(Chuyển slide)*

### Slide 2: Kiến trúc & Lược đồ CSDL

🎤 **Người B:**

> Hệ thống được xây dựng theo kiến trúc **Clean Architecture kết hợp DDD**, chia thành 3 tầng: Domain, Application, và Infrastructure — Dependency Rule đảm bảo tầng ngoài phụ thuộc tầng trong, không ngược lại.
>
> Gồm **5 module nghiệp vụ** độc lập: User, Station, Train, Booking, Payment. Mỗi module đầy đủ 3 tầng, giao tiếp qua Port interface — đảm bảo **testability** cao vì mọi dependency đều có thể mock.
>
> **Tech stack:** Spring Boot 4.0 / Java 25, Next.js 16 / React 19, PostgreSQL 18 + Flyway, Valkey cache, Stripe payment, SSE real-time.
>
> Về **lược đồ CSDL**: 11 bảng, 5 phân hệ. UUID v7 primary key, soft delete, UTC timezone, optimistic locking trên trip_seat_availability.
>
> **Chiến lược kiểm thử tổng thể** của dự án:
> - **3 mức kiểm thử**: phân tích (đối chiếu diagram), thiết kế (walkthrough/inspection), hiện thực (automated test)
> - **Test pyramid**: Unit test (UseCase độc lập, mock dependencies) → Integration test (full HTTP endpoint + TestContainers PostgreSQL) → Stress test (50 concurrent threads)
> - **Kỹ thuật áp dụng**: Equivalence Partitioning, Boundary Value Analysis, State Transition Testing, Security Testing
> - Tổng cộng hơn **160 test cases** cho 12 use cases
>
> Bây giờ em nhường lời cho bạn Nguyên trình bày 6 UC đầu tiên.

---

## Phần 2: UC-01 → UC-06 (~7 phút) — Người A thuyết trình

### Slide 3: UC-01 — Đăng ký tài khoản (~1.5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-01: Đăng ký tài khoản. Actor là khách hàng chưa có tài khoản.
>
> **Luồng chính:** Nhập email, password, fullName → validate → kiểm tra email unique → hash BCrypt → tạo user CUSTOMER → phát sự kiện UserRegistered.
>
> **Chiến lược kiểm thử — 12 test cases, 4 mức:**
> - **Unit test** (3 cases): UseCase tạo user thành công, phát event UserRegistered, trả lỗi khi email tồn tại. Mock UserRepository và PasswordEncoder.
> - **Integration test** (4 cases): Full HTTP endpoint — đăng ký thành công trả 201, email trùng trả 409, validation email sai format, password < 8 ký tự.
> - **Security test** (3 cases): SQL injection trong email bị reject, XSS payload trong fullName trả về dạng data thuần, response không chứa field password.
> - **Stress test** (1 case): 50 threads đăng ký cùng email — chỉ 1 thành công nhờ DB unique constraint.
>
> Kỹ thuật test: **Equivalence Partitioning** cho email (valid/invalid format, existing/new), **Boundary Value** cho password (7 vs 8 ký tự).

💻 **Người B:** *(Demo app: đăng ký thành công → demo email trùng → 409. Sau đó mở terminal chạy test)*

```bash
./gradlew test --tests '*RegisterUser*' --tests '*AuthControllerRegisterTest*'
```

💻 **Người B:** *(Show kết quả: 12 tests passed, highlight stress test output "1 success, 49 conflict")*

---

### Slide 4: UC-02 — Đăng nhập (~1.5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-02: Đăng nhập. Tiền điều kiện: đã đăng ký.
>
> **Luồng chính:** Nhập email + password → tìm user → so sánh timing-safe BCrypt → tạo accessToken JWT (15 phút) + refreshToken (hash SHA-256 lưu DB).
>
> **Chiến lược kiểm thử — 13 BE + 7 FE tests:**
> - **Unit test**: UseCase authenticate thành công trả token pair, password sai trả lỗi, user not found trả cùng lỗi (chống user enumeration).
> - **Integration test**: Full endpoint — login thành công 200 + tokens, credentials sai 401, missing fields 400.
> - **Security test**: Timing-safe comparison — response time cho "email tồn tại + sai pass" ≈ "email không tồn tại" (chống timing attack). Refresh token được hash trước khi lưu.
> - **Stress test**: 50 concurrent logins — tất cả phải thành công và nhận token khác nhau.
> - **Frontend test** (7 cases): Form render, validation, mutation call, error display, redirect on success.
>
> Kỹ thuật: **State Transition** (token lifecycle: active → revoked), **Security Testing** (timing attack, user enumeration).

💻 **Người B:** *(Demo: login thành công → login sai pass → show 401. Chạy test)*

```bash
./gradlew test --tests '*LoginUser*' --tests '*AuthControllerLoginTest*'
```

💻 **Người B:** *(Show 13 tests passed)*

---

### Slide 5: UC-03 — Đăng xuất (~40 giây)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-03: Đăng xuất — thiết kế **idempotent**, luôn trả 200.
>
> **Kiểm thử (12 tests):** Kỹ thuật **State Transition Testing** — token chuyển từ ACTIVE → REVOKED. Test idempotency: token đã revoke hoặc unknown vẫn trả 200. SQL injection trong body bị reject. Stress 50 concurrent logouts cùng token.

💻 **Người B:** *(Demo logout nhanh trên app)*

---

### Slide 6: UC-04 — Quản lý thông tin cá nhân (~1 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-04: Quản lý thông tin cá nhân. 2 thao tác: xem (GET /me) và cập nhật (PUT /me).
>
> **Kiểm thử:** Kỹ thuật **Equivalence Partitioning** trên từng field: phone (valid/invalid format), DOB (past/future date), gender (MALE/FEMALE/OTHER/invalid), identityNumber (9/12 digits/invalid). **Access control test**: JWT userId phải khớp — không cho xem/sửa profile người khác.

💻 **Người B:** *(Demo: cập nhật profile thành công)*

---

### Slide 7: UC-05 — Tra cứu ga tàu (~1 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-05: Tra cứu ga tàu — 3 cách: search ILIKE, browse cursor pagination, detail by ID.
>
> **Kiểm thử:** Kỹ thuật **Boundary Value** cho search query: empty string, 1 char, keyword match, no match. Cursor pagination: first page, middle page, last page (no more items). Cache verification: request 1 = cache miss (DB hit), request 2 = cache hit (no DB). Ga không tồn tại → 404.

💻 **Người B:** *(Demo combobox: gõ "Hà N" → suggestion list)*

---

### Slide 8: UC-06 — Tra cứu chuyến tàu (~1.5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> UC-06: Tra cứu chuyến tàu — bộ lọc đa tiêu chí, sort, phân trang.
>
> **Kiểm thử:**
> - **Equivalence Partitioning**: Filter combinations — chỉ ga đi, ga đi + đến, ga đi + đến + ngày, full filter. Sort: by time ASC/DESC, by price, by available seats.
> - **Boundary Value**: minPrice = 0, maxPrice = MAX, date = today, date = past (empty result).
> - **Integration test**: Cursor pagination không bỏ sót/trùng record khi data thay đổi giữa các page.
> - **Stress test**: 50 concurrent searches — tất cả trả kết quả đúng, response time < 500ms p95.
>
> Kỹ thuật đặc biệt: test **enriched response** — mỗi trip phải chứa đúng thông tin tàu, ga, số ghế trống tính toán real-time.

💻 **Người B:** *(Demo: search Hà Nội → Đà Nẵng → hiện kết quả. Chạy test)*

```bash
./gradlew test --tests '*SearchScheduledTrip*'
```

🎤 **Người A:**

> Bây giờ em nhường lại cho bạn Phú trình bày phần đặt vé và thanh toán — phần phức tạp nhất về mặt đảm bảo chất lượng.

---

## Phần 3: UC-07 → UC-12 (~8 phút) — Người B thuyết trình

### Slide 9: UC-07 — Xem sơ đồ ghế (~1.5 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-07: Xem sơ đồ ghế — 2 view: flat list phân trang và sơ đồ toa. 3 trạng thái ghế: AVAILABLE, HELD, BOOKED. Real-time update qua SSE.
>
> **Chiến lược kiểm thử:**
> - **State Transition Testing**: Ghế có 4 trạng thái (AVAILABLE → HELD → BOOKED, HELD → AVAILABLE khi hủy/hết hạn). Test mỗi transition hiển thị đúng màu/icon trên UI.
> - **Integration test (SSE)**: Mở 2 client cùng xem 1 chuyến → client A đặt ghế → client B nhận SeatStatusChangedEvent trong < 1 giây mà không cần refresh.
> - **Boundary Value**: Trip có 0 ghế trống (all BOOKED), trip có tất cả ghế trống, trip không tồn tại → 404.
> - **Cache test**: Sơ đồ ghế cache hit/miss, cache invalidation khi trạng thái ghế thay đổi.

💻 **Người A:** *(Mở 2 tab cùng chuyến → tab 1 chọn ghế → tab 2 thấy ghế đổi màu real-time)*

🎤 **Người B:**

> Đây là demo trực tiếp SSE real-time — khi tab 1 hold ghế, tab 2 nhận event ngay lập tức.

---

### Slide 10: UC-08 — Đặt vé tàu (~2 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-08: Đặt vé — use case phức tạp nhất, **23 test cases backend + 10 test cases frontend**.
>
> **Luồng chính:** Gửi tripId, seatIds[], passengers[], idempotencyKey → validate → kiểm tra seats AVAILABLE → all-or-nothing hold → tạo booking HELD (15 phút) → publish events.
>
> **Chiến lược kiểm thử theo 5 tiêu chí:**
>
> **1. Xử lý chính xác (7 unit tests):**
> - Happy path: tạo booking thành công, status = HELD, totalPrice = price × seats
> - Idempotency: cùng idempotencyKey trả cùng booking, không tạo mới
> - Error paths: user not found, trip not found, active hold exists, seat unavailable
>
> **2. Concurrency (2 stress tests):**
> - 50 threads đặt cùng ghế → chỉ 1 thành công, 49 nhận SEAT_NOT_AVAILABLE
> - Cơ chế: optimistic lock trên trip_seat_availability + DB constraint
>
> **3. All-or-nothing (1 test):**
> - Chọn 3 ghế, 1 đã HELD → tất cả bị reject, không partial hold
>
> **4. Event publishing (2 tests):**
> - BookingCreated event cho booking service
> - SeatStatusChangedEvent cho SSE broadcast
>
> **5. Security (integration tests):**
> - 401 khi không có token, userId luôn lấy từ JWT (chống impersonation)

💻 **Người A:** *(Demo: chọn 2 ghế → điền info → submit → thành công. Sau đó chạy stress test)*

```bash
./gradlew test --tests '*CreateBookingStressTest*'
```

💻 **Người A:** *(Show output: "1 success, 49 seat_not_available" — chứng minh concurrent safety)*

🎤 **Người B:**

> Stress test này chứng minh hệ thống đảm bảo data consistency dưới tải đồng thời 50 requests — không bao giờ xảy ra double-booking.

---

### Slide 11: UC-09 — Xem đặt vé (~1 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-09: Xem đặt vé — danh sách phân trang + chi tiết aggregate.
>
> **Kiểm thử:**
> - **Access control test (Security)**: Gọi API với bookingId của user khác → 403 Forbidden. Đây là test quan trọng vì IDOR (Insecure Direct Object Reference) là vulnerability phổ biến.
> - **Pagination test**: Verify thứ tự createdAt DESC, metadata (totalItems, hasNext) chính xác.
> - **Aggregate test**: Chi tiết booking phải chứa đầy đủ trip info, seat info, payment info — không thiếu field.

💻 **Người A:** *(Demo: vào "Đặt vé của tôi" → xem danh sách → click chi tiết)*

---

### Slide 12: UC-10 — Hủy đặt vé (~1 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-10: Hủy đặt vé — cho phép hủy HELD hoặc CONFIRMED.
>
> **Kiểm thử — kỹ thuật State Transition:**
> - Valid transitions: HELD → CANCELLED (release seats), CONFIRMED → CANCELLED (release seats + flag refund)
> - Invalid transition: CANCELLED → CANCELLED → 409 Conflict (idempotent check)
> - **Event test**: BookingCancelledEvent chứa flag requiresRefund = true khi hủy CONFIRMED
> - **SSE test**: SeatStatusChangedEvent broadcast khi ghế release → các client khác thấy ghế trống
> - **Access control**: Hủy booking người khác → 403

💻 **Người A:** *(Demo: click hủy booking → xác nhận → status chuyển CANCELLED → ghế trở lại available trên sơ đồ)*

---

### Slide 13: UC-11 — Xem thanh toán (~40 giây)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-11: Xem thanh toán — tra cứu theo paymentId hoặc bookingId.
>
> **Kiểm thử:** Access control 403 khi xem payment người khác, 404 khi không tồn tại, verify checkoutUrl chỉ có giá trị khi status = PENDING (Equivalence Partitioning trên PaymentStatus: PENDING/PAID/FAILED/CANCELLED/REFUNDED).

💻 **Người A:** *(Demo nhanh: xem payment detail với status PENDING)*

---

### Slide 14: UC-12 — Thanh toán (~2 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> UC-12: Thanh toán Stripe — UC phức tạp nhất về mặt **state management** và **external integration**.
>
> **Luồng:** Tạo Stripe Checkout Session → redirect user → user thanh toán → Stripe gửi webhook → hệ thống confirm booking.
>
> **Chiến lược kiểm thử — 5 nhóm:**
>
> **1. Webhook handling (State Transition Testing):**
> - `checkout.session.completed`: HELD → CONFIRMED, HELD → BOOKED, PENDING → PAID
> - `checkout.session.expired`: PENDING → FAILED
> - `payment_intent.payment_failed`: PENDING → CANCELLED
>
> **2. Idempotency test:**
> - Stripe có thể gửi webhook trùng lặp → test gửi event 2 lần → lần 2 no-op, không exception
>
> **3. Late payment edge case:**
> - User thanh toán SAU khi booking đã bị hủy (hết 15 phút) → hệ thống tự động refund qua Stripe API
> - Test: webhook completed + booking.status = CANCELLED → trigger RefundPaymentUseCase → payment = REFUNDED
>
> **4. Signature verification (Security):**
> - Webhook payload phải verify Stripe signature → reject nếu signature invalid (chống giả mạo webhook)
>
> **5. Integration test:**
> - Full flow với mock Stripe API: create session → verify checkoutUrl format → simulate webhook → verify final state

💻 **Người A:** *(Demo full flow: click thanh toán → redirect Stripe → nhập card test 4242... → thành công → redirect về app → status CONFIRMED)*

🎤 **Người B:**

> Card test `4242 4242 4242 4242` là card test của Stripe cho môi trường sandbox — không charge tiền thật.

💻 **Người A:** *(Chạy test)*

```bash
./gradlew test --tests '*Payment*UseCase*'
```

💻 **Người A:** *(Show kết quả: HandlePaymentSuccess, HandlePaymentFailed, CancelPendingPayment, RefundPayment — all passed)*

---

## Phần 4: Kết luận (~1.5 phút) — Người B thuyết trình

### Slide 15: Kết luận

🎤 **Người B:**

> Tổng kết, nhóm đã hoàn thành **12 use cases** với hơn **160 test cases** tự động hóa, áp dụng đầy đủ các kỹ thuật kiểm thử phần mềm:
>
> **Kỹ thuật kiểm thử đã áp dụng:**
> - **Equivalence Partitioning**: Phân lớp input cho validation (email format, password length, payment status...)
> - **Boundary Value Analysis**: Giá trị biên cho password (7/8 chars), search query (empty/1 char), pagination (first/last page)
> - **State Transition Testing**: Booking lifecycle (HELD→CONFIRMED→CANCELLED), Seat lifecycle (AVAILABLE→HELD→BOOKED), Payment lifecycle (PENDING→PAID/FAILED/REFUNDED)
> - **Concurrency/Stress Testing**: 50 concurrent threads cho các chức năng critical (đăng ký, đặt vé, thanh toán)
> - **Security Testing**: SQL injection, XSS, timing attack, user enumeration, IDOR, webhook signature verification
>
> **Điểm nổi bật QA:**
> - Kiến trúc Clean Architecture đảm bảo **testability** — mọi dependency đều mock được
> - Test pyramid đầy đủ: unit → integration → stress → security
> - Concurrent test chứng minh **data consistency** dưới tải
> - Idempotency test đảm bảo **reliability** khi network retry
>
> Hướng phát triển: mở rộng test coverage với mutation testing, thêm E2E test với Playwright, performance benchmark dưới production-like load.
>
> Em xin cảm ơn thầy và các bạn đã lắng nghe. Nhóm sẵn sàng trả lời câu hỏi ạ.

---

## Tổng kết thời gian

| Phần | Nội dung | Thời lượng | Người thuyết trình |
|------|----------|-----------|-------------------|
| 1 | Mở đầu + Kiến trúc + Chiến lược test | ~3 phút | Người B (Phú) |
| 2 | UC-01 → UC-06 | ~7 phút | Người A (Nguyên) |
| 3 | UC-07 → UC-12 | ~8 phút | Người B (Phú) |
| 4 | Kết luận | ~1.5 phút | Người B (Phú) |
| | **Tổng** | **~19.5 phút** | |

---

## Chuẩn bị trước buổi báo cáo

### Môi trường

- Docker containers running (PostgreSQL + Valkey + backend + frontend)
- Browser mở sẵn app tại localhost:3000, đã có data seed
- Terminal mở sẵn tại thư mục project, đã build xong
- 2 tab browser cho demo SSE real-time (UC-07)

### Demo test nhanh (nếu thầy yêu cầu chạy thêm)

```bash
# Chạy toàn bộ test suite
./gradlew test

# Chạy test theo module
./gradlew test --tests '*booking*'
./gradlew test --tests '*payment*'

# Chạy chỉ stress tests
./gradlew test --tests '*StressTest*'

# Frontend tests
cd frontend/customer && bun run test
```

### Stripe test mode

- Card: `4242 4242 4242 4242`, expiry bất kỳ trong tương lai, CVC bất kỳ 3 số

### Điều chỉnh thời gian

- **Thiếu thời gian**: Rút ngắn UC-03, UC-04, UC-11 (chỉ nói 1-2 câu mỗi UC, không demo)
- **Thừa thời gian**: Demo concurrent booking conflict ở UC-08 (mở 2 browser, đặt cùng ghế), demo SSE chi tiết hơn ở UC-07
- **Thầy hỏi về test**: Sẵn sàng chạy bất kỳ test class nào, show coverage report

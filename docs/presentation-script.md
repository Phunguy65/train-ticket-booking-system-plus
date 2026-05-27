# Văn bản thuyết trình — Đồ án Đảm bảo chất lượng phần mềm

**Đề tài:** Ứng dụng đặt vé tàu trực tuyến
**GVHD:** Thầy Nguyễn Anh Hào
**Sinh viên trình bày:**
- **SV1** — Nguyễn Ngọc Phú (N22DCCN159)
- **SV2** — Y Cao Nguyên Byă (N22DCCN200)

**Thời lượng dự kiến:** ~15 phút · 15 slide

---

## Slide 1 — Trang bìa (~30 giây)

**SV1 (mở đầu):**

> Kính chào Thầy và các bạn. Chúng em là Nguyễn Ngọc Phú và Y Cao Nguyên Byă, sinh viên lớp N22DCCN. Hôm nay nhóm chúng em xin trình bày báo cáo đồ án môn **Đảm bảo chất lượng phần mềm** với đề tài **"Ứng dụng đặt vé tàu trực tuyến"**, dưới sự hướng dẫn của thầy Nguyễn Anh Hào.

> Trong vòng 15 phút, chúng em sẽ trình bày kiến trúc hệ thống, 12 use case nghiệp vụ chính cùng chiến lược kiểm thử cho từng use case, và cuối cùng là kết quả đạt được.

---

## Slide 2 — Kiến trúc & Lược đồ CSDL (~2 phút)

**SV1:**

> Hệ thống được thiết kế theo **Clean Architecture** kết hợp **Domain-Driven Design**, với 3 lớp đồng tâm như Thầy thấy ở sơ đồ bên trái:

> - **Lõi Domain** chứa Entity, Value Object và Aggregate Root — đây là phần thuần nghiệp vụ, không phụ thuộc framework.
> - **Application** ở giữa định nghĩa các Use Case, Port và Command/Query — điều phối nghiệp vụ.
> - **Infrastructure** vòng ngoài chứa JPA, REST controller, Stripe gateway, cache và SSE — chỉ phục vụ kỹ thuật.

> Quy tắc cốt lõi là **Dependency Rule**: tầng ngoài phụ thuộc tầng trong, không được phép ngược lại. Điều này giúp domain giữ thuần khiết và dễ kiểm thử.

> Hệ thống được chia thành **6 bounded context** độc lập: user, station, train, booking, payment và shared. Các module giao tiếp với nhau qua Port theo nguyên tắc DIP.

> **Tech stack:** Backend dùng Spring Boot 4.0 trên Java 25, Frontend Next.js 16 với React 19. Database PostgreSQL 18 quản lý migration bằng Flyway, cache Valkey 8.0, thanh toán tích hợp Stripe và push event real-time qua Server-Sent Events.

**SV2:**

> Bên phải là sơ đồ ERD gồm **11 bảng** chia theo 5 phân hệ. Phân hệ User có `users` và `refresh_tokens` cho xác thực. Train là phân hệ phức tạp nhất với 5 bảng: `trains`, `coaches`, `seats`, `route_templates`, `scheduled_trips`. Booking gồm `bookings` và `trip_seat_availability` — bảng `trip_seat_availability` cực kỳ quan trọng vì đây là nơi xử lý concurrency khi nhiều người cùng đặt một ghế.

> Một số quy ước thiết kế quan trọng nhóm em áp dụng: **UUID v7** làm primary key thay vì auto-increment để tránh hot-spot khi insert; **soft delete** cho dữ liệu nghiệp vụ; toàn bộ timestamp dùng `TIMESTAMPTZ UTC`; và đặc biệt là **optimistic locking** trên `trip_seat_availability` để chống race condition khi 2 user cùng giữ một ghế.

---

## Slide 3 — UC-01: Đăng ký tài khoản (~50 giây)

**SV1:**

> UC đầu tiên là **Đăng ký tài khoản**. Tác nhân là khách chưa có tài khoản.

> **Luồng chính** gồm 5 bước: người dùng nhập email và mật khẩu, frontend validate phía client trước, sau đó gửi request lên backend. Backend kiểm tra email đã tồn tại chưa, nếu chưa thì hash mật khẩu bằng BCrypt và lưu user mới với role là `CUSTOMER`. Cuối cùng phát domain event `UserRegistered` để các module khác phản ứng.

> **Về kiểm thử**, nhóm em viết:
> - **Unit test** cho `RegisterUserUseCase` — kiểm tra hash password, validate email format, xử lý trường hợp email trùng.
> - **Integration test** trên `AuthController` với lớp database thật qua TestContainers.
> - **Stress test** mô phỏng hàng nghìn request đăng ký đồng thời để đo throughput và độ ổn định.

> Sequence diagram bên phải minh họa flow 4 lifeline: User, Frontend, API và Database.

---

## Slide 4 — UC-02: Đăng nhập (~50 giây)

**SV1:**

> UC-02 là **Đăng nhập**. Người dùng nhập email/password, hệ thống verify credentials và phát hành **cặp JWT token**: access token thời hạn ngắn và refresh token thời hạn dài.

> Điểm cần lưu ý là chúng em đảm bảo response thời gian **xử lý đồng đều** giữa trường hợp email không tồn tại và password sai — để chống tấn công **timing attack** giúp attacker dò username hợp lệ.

> **Kiểm thử** tập trung vào:
> - Validate happy path và các trường hợp credentials sai.
> - **Security test** đặc biệt cho timing attack — chúng em đo độ trễ giữa các trường hợp lỗi và verify chênh lệch dưới ngưỡng an toàn.
> - Stress test xác minh hệ thống chịu được tải lúc cao điểm bán vé.

---

## Slide 5 — UC-03: Đăng xuất (~40 giây)

**SV2:**

> UC-03 là **Đăng xuất**. Người dùng gửi refresh token, backend revoke token đó trong database. Đặc tính quan trọng nhất là **idempotent** — gọi logout 2 lần liên tiếp không gây lỗi, đảm bảo tính ổn định của UX.

> **Kiểm thử** kiểm tra: token bị revoke không còn dùng được; gọi logout với token đã revoke vẫn trả về thành công; và security test xác minh không bị rò rỉ thông tin về trạng thái token.

---

## Slide 6 — UC-04: Quản lý thông tin cá nhân (~50 giây)

**SV2:**

> UC-04 cho phép user **xem và cập nhật thông tin cá nhân**: họ tên, số điện thoại, ngày sinh, giới tính, CMND/CCCD và địa chỉ.

> Điểm đáng chú ý là chúng em sử dụng **Value Object** trong domain layer cho các trường có quy tắc nghiệp vụ — ví dụ `PhoneNumber`, `EmailAddress`, `IdDocumentNumber`. Mỗi Value Object tự validate trong constructor, đảm bảo dữ liệu lỗi không bao giờ tồn tại trong domain.

> **Kiểm thử:**
> - Unit test riêng cho từng Value Object.
> - Test authorization — user A không được xem hoặc sửa profile của user B.
> - Stress test cho update concurrent.

---

## Slide 7 — UC-05: Tra cứu ga tàu (~40 giây)

**SV2:**

> UC-05 là **Tra cứu ga tàu**. User nhập từ khóa, hệ thống search theo tên hoặc mã ga, trả về kết quả phân trang theo cursor.

> Vì sao dùng **cursor-based pagination** thay vì offset? Vì offset pagination chậm dần khi page sâu — query phải scan toàn bộ rows trước. Cursor pagination dùng index để jump trực tiếp, performance ổn định ở mọi vị trí.

> **Kiểm thử** xác minh: tính chính xác của filter, cursor encode/decode đúng, và pagination performance qua stress test.

---

## Slide 8 — UC-06: Tra cứu chuyến tàu (~50 giây)

**SV1:**

> UC-06 là **Tra cứu chuyến tàu** — UC quan trọng vì là điểm bắt đầu của flow đặt vé. User chọn ga đi, ga đến, ngày đi; hệ thống trả về danh sách chuyến với **số ghế còn trống** đã được tính toán sẵn.

> Để tối ưu, chúng em **cache kết quả search** vào Valkey với TTL ngắn — vì query này được gọi rất nhiều lần với cùng tham số. Cache invalidate khi có booking mới.

> **Kiểm thử** gồm:
> - Test filtering theo nhiều criteria.
> - Test tính toán availability đúng kể cả khi có booking đang HELD.
> - **Cache test** — xác minh cache hit/miss, invalidation đúng thời điểm.
> - Stress test 1000+ concurrent search.

---

## Slide 9 — UC-07: Xem sơ đồ ghế chuyến tàu (~50 giây)

**SV1:**

> UC-07 — **Xem sơ đồ ghế** — đây là UC kỹ thuật phức tạp nhất phía frontend.

> User chọn chuyến, hệ thống load layout toa và trạng thái từng ghế: trống, đang giữ, đã đặt. Quan trọng là sơ đồ này phải **cập nhật real-time** — khi user khác đặt ghế hoặc release, mình phải thấy ngay.

> Chúng em dùng **Server-Sent Events (SSE)**: client subscribe vào endpoint, backend push event mỗi khi seat status thay đổi. Pattern này nhẹ hơn WebSocket vì chỉ cần một chiều server → client.

> **Kiểm thử:**
> - Test layout render đúng với dữ liệu nhiều toa.
> - Test SSE connection — kết nối, reconnect khi mất mạng, broadcast đến đúng subscriber.
> - Test concurrent: 2 user cùng xem sơ đồ phải cùng thấy thay đổi khi user thứ 3 đặt ghế.

---

## Slide 10 — UC-08: Đặt vé tàu (~70 giây) ⭐

**SV1:**

> UC-08 — **Đặt vé tàu** — là use case cốt lõi và phức tạp nhất của hệ thống.

> Flow gồm 4 bước: user chọn ghế → nhập thông tin hành khách → backend giữ ghế (HOLD) trong **15 phút** → tạo booking với trạng thái pending payment.

> Vấn đề khó nhất là **race condition**: 2 user cùng click ghế #5 trong cùng một mili giây. Nếu không xử lý đúng, cả 2 đều thấy "thành công" nhưng database chỉ giữ được 1.

> Nhóm em giải quyết bằng **2 lớp bảo vệ**:
> 1. **Optimistic locking** trên bảng `trip_seat_availability` qua field `version`. User thua sẽ nhận về `OptimisticLockException` và frontend thông báo "ghế vừa được người khác đặt".
> 2. **Idempotency key** — nếu user click submit 2 lần do mạng chậm, request thứ 2 không tạo booking trùng.

> Sau 15 phút mà chưa thanh toán, **scheduler tự động release ghế** và set booking thành EXPIRED.

> **Kiểm thử:**
> - Domain test cho aggregate `Booking` và `RouteSeatAvailability`.
> - **Concurrency test** — 100 thread cùng đặt 1 ghế, đảm bảo chỉ 1 thành công.
> - Test idempotency key.
> - Test scheduler release đúng sau 15 phút.
> - Stress test 5000+ booking đồng thời.

---

## Slide 11 — UC-09: Xem đặt vé (~40 giây)

**SV2:**

> UC-09 — **Xem danh sách & chi tiết đặt vé**. User vào tài khoản, thấy lịch sử booking với pagination; click vào để xem chi tiết bao gồm thông tin chuyến, ghế, hành khách, trạng thái thanh toán.

> Điểm cần kiểm tra kỹ là **authorization** — đảm bảo user chỉ xem được booking của chính mình, không leak được booking ID của user khác qua URL.

> **Kiểm thử:**
> - Authorization test — user A truy cập booking của user B → 403 Forbidden.
> - Pagination accuracy.
> - Test với booking ở mọi trạng thái: HELD, CONFIRMED, CANCELLED, EXPIRED.

---

## Slide 12 — UC-10: Hủy đặt vé (~50 giây)

**SV2:**

> UC-10 — **Hủy đặt vé**. Có 2 nhánh xử lý theo trạng thái booking:

> - Nếu booking đang **HELD** (chưa thanh toán): chỉ cần release ghế, không phát sinh hoàn tiền.
> - Nếu booking đã **CONFIRMED** (đã thanh toán): release ghế VÀ gọi Stripe API để **refund** tiền cho khách.

> Điểm quan trọng là **transaction atomicity** — phải đảm bảo cả 3 thao tác (update booking, release seat, refund) hoặc cùng thành công, hoặc cùng rollback. Nhóm em xử lý bằng `@Transactional` và compensating action cho phần Stripe.

> **Kiểm thử:**
> - Test cả 2 nhánh HELD và CONFIRMED.
> - Test refund mock với Stripe SDK.
> - Test transaction rollback khi Stripe lỗi.
> - Stress test cancel concurrent.

---

## Slide 13 — UC-11: Xem thanh toán (~40 giây)

**SV2:**

> UC-11 cho phép user **xem lịch sử thanh toán**: amount, currency, status (PENDING/SUCCEEDED/FAILED/REFUNDED), timestamp, và link tới booking liên quan.

> Tương tự UC-09, **authorization** là yếu tố security trọng tâm.

> **Kiểm thử** bao gồm authorization, pagination, và test chính xác mapping giữa Stripe payment status và domain payment status.

---

## Slide 14 — UC-12: Thanh toán (~70 giây) ⭐

**SV1:**

> UC-12 — **Thanh toán** — là use case tích hợp với bên thứ ba phức tạp nhất.

> Flow gồm 4 bước:
> 1. Backend tạo **Stripe Checkout Session**, trả về URL.
> 2. Frontend redirect user sang trang Stripe để nhập thẻ.
> 3. Sau khi user thanh toán, Stripe **gửi webhook** về backend.
> 4. Backend xử lý webhook để xác nhận booking thành CONFIRMED.

> Hai vấn đề kỹ thuật then chốt:

> **Thứ nhất — bảo mật webhook**: Stripe ký mỗi webhook request bằng HMAC. Backend phải **verify chữ ký** với secret key trước khi xử lý — nếu không, attacker có thể giả webhook để confirm booking miễn phí.

> **Thứ hai — idempotency của webhook**: Stripe có thể retry webhook nhiều lần nếu nghi ngờ network issue. Mình phải đảm bảo xử lý cùng `event_id` nhiều lần không gây ra side effect kép như confirm booking 2 lần.

> **Kiểm thử:**
> - Mock Stripe SDK để test happy path và các trường hợp lỗi.
> - **Security test** — webhook signature invalid → reject; replay attack → reject lần thứ 2.
> - Test idempotency với cùng event_id.
> - Test webhook timeout & retry.
> - Test refund flow end-to-end.

---

## Slide 15 — Kết luận (~90 giây)

**SV2:**

> Để kết thúc, nhóm em xin tóm tắt những kết quả đạt được:

> **Về tính năng**: hoàn thành đầy đủ 12 use case nghiệp vụ, từ xác thực, tra cứu, đặt vé, đến thanh toán và hủy vé — tích hợp cổng thanh toán Stripe thật và push event real-time qua SSE.

> **Về chất lượng**: tổng cộng nhóm em viết hơn **400+ test case**, chia thành 3 mức:
> - Unit test cho domain logic và use case.
> - Integration test với database thật qua TestContainers.
> - Stress test cho các kịch bản cao điểm.
>
> Đặc biệt là test concurrency cho UC-08 với **optimistic locking**, đảm bảo không có ghế nào bị đặt 2 lần dưới tải cao.

**SV1:**

> **Về kiến trúc**, hệ thống tuân thủ Clean Architecture nghiêm ngặt — domain thuần khiết không phụ thuộc framework — nên dễ kiểm thử, dễ maintain. Việc chia 6 bounded context giúp các module phát triển song song mà không xung đột.

> **Hướng phát triển trong tương lai**:
> - Mở rộng cho **admin panel** quản lý chuyến tàu, tuyến và lịch chạy.
> - Tích hợp thêm cổng thanh toán nội địa như **VNPay**, **Momo**.
> - Triển khai **distributed tracing** với OpenTelemetry để monitoring chuyên sâu.
> - Bổ sung **mobile app** native cho iOS và Android.

> Nhóm em xin chân thành cảm ơn thầy Nguyễn Anh Hào đã tận tình hướng dẫn trong suốt quá trình làm đồ án. Phần trình bày của nhóm em đến đây kết thúc — rất mong nhận được câu hỏi và góp ý từ Thầy và các bạn.

> Em xin cảm ơn.

---

## Lưu ý khi trình bày

**Về phong cách:**
- Nói rõ ràng, chậm rãi — đặc biệt với các thuật ngữ kỹ thuật.
- Khi sang slide mới, dừng 1-2 giây cho khán giả nhìn slide trước khi nói.
- Dùng laser/pointer chỉ vào sơ đồ khi giải thích flow.

**Về timing:**
- Nếu vượt thời gian: rút ngắn các UC đơn giản (UC-03, UC-05, UC-09, UC-11) — chỉ giữ tên + 1 câu kiểm thử.
- Tập trung thời gian cho **UC-08** và **UC-12** — đây là phần technical highlight của đồ án.

**Khi Q&A:**
- Câu hỏi về concurrency → tập trung vào optimistic locking + idempotency key.
- Câu hỏi về testing → nhấn mạnh 3 mức (unit/integration/stress) và TestContainers.
- Câu hỏi về security → timing attack (UC-02), authorization (UC-09, UC-11), webhook signature (UC-12).
- Nếu không trả lời được: thành thật ghi nhận và cảm ơn câu hỏi.

**Phân chia trình bày:**
- **SV1 (Phú)**: Slide 1 (mở đầu), 2 (kiến trúc — phần Clean Arch), 3, 4, 8, 9, 10 (UC-08), 14 (UC-12), 15 (phần kết luận kiến trúc).
- **SV2 (Byă)**: Slide 2 (kiến trúc — phần ERD), 5, 6, 7, 11, 12, 13, 15 (phần kết quả test + cảm ơn).
- Tổng: mỗi bạn ~7-8 phút trình bày.

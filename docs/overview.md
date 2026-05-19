# Giới thiệu và Cơ sở Khoa học

## Giới thiệu đề tài

### Mục đích

Vận chuyển hành khách bằng đường sắt là một trong những phương thức di chuyển
phổ biến và có chi phí hợp lý tại Việt Nam, đặc biệt trên các hành trình dài.
Tuy nhiên, quy trình bán vé truyền thống tại các quầy giao dịch còn bộc lộ nhiều
hạn chế: hành khách phải trực tiếp đến ga, xếp hàng chờ đợi, nhân viên xử lý thủ
công, và việc kiểm soát tình trạng ghế theo thời gian thực gặp nhiều khó khăn,
đặc biệt vào các dịp cao điểm.

Đề tài xây dựng một ứng dụng đặt vé tàu hỏa trực tuyến (Train Ticket Booking
System) nhằm hiện đại hóa quy trình bán vé cho một công ty vận tải đường sắt.
Ứng dụng cung cấp nền tảng kỹ thuật số để:

- **Hành khách** tra cứu chuyến tàu, xem sơ đồ ghế, đặt vé và thanh toán trực
  tuyến mọi lúc mọi nơi mà không cần đến quầy giao dịch.
- **Công ty vận tải** kiểm soát tình trạng ghế theo thời gian thực, tự động hóa
  quy trình xử lý đặt vé và thanh toán, giảm thiểu sai sót thủ công, tăng khả
  năng phục vụ đồng thời nhiều khách hàng.

### Mục tiêu

Ứng dụng phải giải quyết được các vấn đề chính sau:

**Vấn đề 1 — Tra cứu thông tin chuyến tàu:** Hành khách cần tìm kiếm chuyến tàu
theo ga đi, ga đến, ngày khởi hành, khoảng giá, sắp xếp theo thời gian hoặc số
ghế trống. PM cung cấp chức năng tìm kiếm có bộ lọc linh hoạt với kết quả phân
trang (cursor-based và offset-based), cho phép xem chi tiết từng chuyến.

**Vấn đề 2 — Xem và chọn ghế:** Hành khách cần biết vị trí và trạng thái từng
ghế (trống, đang giữ, đã đặt) trong từng toa trước khi đặt. PM cung cấp sơ đồ
ghế theo toa với cập nhật trạng thái theo thời gian thực qua SSE (Server-Sent
Events).

**Vấn đề 3 — Đặt vé và giữ chỗ:** Hành khách cần đặt giữ ghế trong thời gian
thanh toán. PM tạo booking ở trạng thái `HELD` và khóa các ghế tương ứng trong
15 phút, đảm bảo không có hai hành khách cùng đặt một ghế (cơ chế
all-or-nothing, hỗ trợ idempotency).

**Vấn đề 4 — Thanh toán trực tuyến:** Hành khách cần thanh toán an toàn qua cổng
thanh toán quốc tế. PM tích hợp Stripe để tạo phiên thanh toán, xử lý xác
nhận/thất bại/hết hạn qua webhook, và tự động chuyển booking sang trạng thái
`CONFIRMED` khi thanh toán thành công.

**Vấn đề 5 — Quản lý đặt vé:** Hành khách cần xem lịch sử đặt vé, theo dõi trạng
thái, và hủy vé khi cần. PM cung cấp chức năng xem danh sách và chi tiết đặt vé,
cho phép hủy booking ở trạng thái `HELD` (chưa thanh toán) hoặc `CONFIRMED` (đã
thanh toán), kèm cơ chế giải phóng ghế và phát sự kiện hoàn tiền.

**Vấn đề 6 — Quản lý tài khoản:** Hành khách cần đăng ký, đăng nhập, quản lý
thông tin cá nhân và xóa tài khoản. PM cung cấp xác thực dựa trên cặp token
(access token + refresh token), hỗ trợ cập nhật hồ sơ từng phần và xóa mềm tài
khoản có kiểm tra ràng buộc đặt vé đang hoạt động.

### Phương pháp tiến hành

#### Tìm hiểu hiện trạng

Khảo sát quy trình bán vé tàu truyền thống tại quầy giao dịch và quy trình mua
vé trực tuyến hiện tại (ví dụ: cổng dsvn.vn), xác định các tình huống bất lợi
như: phạm vi phục vụ bị giới hạn bởi giờ mở cửa quầy, nguy cơ bán trùng ghế khi
xử lý thủ công, thiếu truy vết trạng thái đặt vé theo thời gian thực.

#### Tìm hiểu nghiệp vụ và quy định

Tìm hiểu quy trình nghiệp vụ đặt vé tàu: vòng đời vé (tìm kiếm → chọn ghế → giữ
chỗ → thanh toán → xác nhận/hủy/hoàn tiền), các quy tắc quản lý ghế (trạng thái
`AVAILABLE` → `HELD` → `BOOKED`/`AVAILABLE`/`CANCELLED`), chính sách thời gian
giữ chỗ, và quy trình xử lý vé chưa thanh toán hết hạn.

#### Tìm hiểu mô hình, phương pháp và công nghệ

Nghiên cứu và áp dụng các mô hình/phương pháp sau:

- **Domain-Driven Design (DDD)**: phân chia hệ thống thành các Bounded Context
  (người dùng, đặt vé, thanh toán, tàu/ga/tuyến), định nghĩa Aggregate, Domain
  Event và Application Service cho từng ngữ cảnh.
- **Clean Architecture**: tách biệt các tầng Domain, Application,
  Infrastructure; sử dụng Port/Adapter pattern để các tầng trong không phụ thuộc
  vào framework hoặc cơ sở dữ liệu.
- **RESTful API + OpenAPI 3.1**: đặc tả hợp đồng API trước (API-first), sinh
  client code tự động từ OpenAPI spec cho frontend.
- Các công nghệ cụ thể được trình bày trong Mục II.

#### Phân tích, thiết kế, hiện thực, đánh giá

Từ kết quả khảo sát và nghiên cứu, tiến hành: định nghĩa use case (12 UC) và đặc
tả tương tác; thiết kế kiến trúc hệ thống và cơ sở dữ liệu; hiện thực theo từng
module; đánh giá kết quả theo tiêu chí kiểm thử đã đặt ra ở mỗi giai đoạn.

---

## Cơ sở Khoa học của Đề tài

### Quy trình nghiệp vụ đặt vé tàu

#### Quy trình mua vé truyền thống

Trong mô hình bán vé truyền thống tại quầy giao dịch, quy trình diễn ra tuần tự
theo các bước: hành khách đến quầy → trình bày nhu cầu (tuyến, ngày, loại ghế) →
nhân viên tra cứu hệ thống nội bộ → xác nhận còn chỗ → hành khách thanh toán
tiền mặt hoặc chuyển khoản → nhân viên xuất vé giấy → hành khách nhận vé.

Các tình huống trong đó bao gồm:

- **Tra cứu chỗ trống**: nhân viên tra hệ thống, thông báo tình trạng ghế cho
  hành khách.
- **Xử lý đặt chỗ**: nhân viên giữ ghế và lập phiếu đặt.
- **Thu tiền và xuất vé**: thu ngân xử lý thanh toán, hệ thống in vé.
- **Hoàn/đổi vé**: hành khách quay lại quầy, nhân viên xử lý thủ công.

Hạn chế của mô hình này: phạm vi phục vụ bị giới hạn bởi giờ mở cửa, hành khách
phải đến trực tiếp, xử lý thủ công dễ sai sót (đặc biệt khi nhiều giao dịch viên
cùng thao tác trên cùng chuyến tàu), thiếu khả năng truy vết trạng thái theo
thời gian thực.

#### Quy trình mua vé trực tuyến

Với mô hình trực tuyến (được triển khai tại cổng dsvn.vn và tương tự tại nhiều
nền tảng đường sắt khu vực), hành khách thực hiện toàn bộ quy trình qua ứng dụng
web hoặc mobile:

1. **Đăng nhập / xác thực**: hành khách đăng nhập bằng tài khoản cá nhân, hệ
   thống xác thực và cấp phiên làm việc.
2. **Tra cứu chuyến tàu**: hành khách nhập ga đi, ga đến, ngày đi; hệ thống trả
   về danh sách chuyến phù hợp kèm giá và số ghế trống.
3. **Xem sơ đồ ghế và chọn ghế**: hành khách xem tình trạng từng ghế trong toa,
   chọn ghế mong muốn.
4. **Giữ chỗ tạm thời**: hệ thống khóa ghế trong một khoảng thời gian (thường
   10–15 phút) chờ hành khách thanh toán.
5. **Thanh toán**: hành khách thanh toán qua cổng thanh toán điện tử (thẻ ngân
   hàng, ví điện tử). Hệ thống nhận xác nhận từ cổng thanh toán và chuyển trạng
   thái đặt vé sang đã xác nhận.
6. **Nhận xác nhận và eTicket**: hành khách nhận email xác nhận kèm mã QR hoặc
   vé điện tử.
7. **Hủy vé**: hành khách hủy trực tuyến, hệ thống tự động giải phóng ghế và xử
   lý hoàn tiền theo quy định.

Ích lợi so với mô hình truyền thống: phục vụ 24/7 không phụ thuộc giờ mở cửa,
hành khách không cần đến quầy, kiểm soát ghế tự động ngăn bán trùng, có thể truy
vết toàn bộ lịch sử giao dịch.

#### Vòng đời vé tàu

Quy trình trực tuyến đặt ra các trạng thái cần quản lý chặt chẽ trong hệ thống.
Mỗi lần đặt vé (booking) trải qua các trạng thái:

- **HELD (Đang giữ chỗ)**: booking được tạo, ghế bị khóa, đang chờ hành khách
  thanh toán trong thời hạn (15 phút). Nếu hết hạn mà chưa thanh toán, hệ thống
  tự động hủy booking và giải phóng ghế.
- **CONFIRMED (Đã xác nhận)**: thanh toán thành công, ghế chuyển sang trạng thái
  đã đặt (`BOOKED`), vé có hiệu lực.
- **CANCELLED (Đã hủy)**: hành khách hủy thủ công (từ trạng thái `HELD` hoặc
  `CONFIRMED`) hoặc hệ thống tự động hủy khi hết thời hạn giữ chỗ. Ghế được giải
  phóng trở lại.

Song song đó, mỗi ghế (seat) cũng có vòng đời riêng:

- **AVAILABLE**: ghế trống, có thể đặt.
- **HELD**: ghế đang bị giữ bởi một booking. Trạng thái này có thể hết hạn và
  trở về `AVAILABLE` nếu booking bị hủy.
- **BOOKED**: ghế đã được đặt thành công sau khi thanh toán.
- **CANCELLED**: ghế thuộc một booking đã bị hủy sau khi đã xác nhận.

Việc quản lý đồng thời hai vòng đời này đòi hỏi tính nhất quán dữ liệu cao: mọi
thay đổi trạng thái booking và ghế phải được thực hiện trong cùng một giao dịch
(transaction), đồng thời cần cơ chế xử lý bất đồng bộ (expiry job) để tự động
thu hồi chỗ giữ đã hết hạn.

---

### Công nghệ sử dụng

#### Spring Boot 4.0 — Backend API

Spring Boot 4.0 (Java 25) được dùng để xây dựng backend REST API của hệ thống.
Framework này cung cấp cơ chế tự động cấu hình (auto-configuration), quản lý
dependency injection, và tích hợp sẵn với các thư viện cần thiết (Spring
Security, Spring Data JPA). Lý do lựa chọn Spring Boot: hệ sinh thái phong phú,
hỗ trợ tốt kiến trúc phân lớp theo DDD và Clean Architecture, có khả năng build
GraalVM Native Image để giảm thời gian khởi động.

Backend sử dụng thêm **Spring Virtual Threads** (Java 25) để xử lý đồng thời số
lượng lớn kết nối HTTP mà không tốn nhiều tài nguyên, và **Spring Integration**
để điều phối luồng xử lý sự kiện nội bộ.

#### Next.js 16 + React 19 — Frontend khách hàng

Next.js 16 với React 19 được dùng để xây dựng giao diện web cho khách hàng. Ứng
dụng sử dụng **App Router** của Next.js để tổ chức routing và hỗ trợ Server
Components, giúp tối ưu hiệu năng tải trang. Bun được dùng làm runtime và
package manager thay thế Node.js để tăng tốc độ cài đặt và build. Lý do chọn
Next.js: hỗ trợ SSR/SSG linh hoạt, React 19 cải thiện đáng kể hiệu năng render
phía client, tích hợp tốt với hệ thống codegen từ OpenAPI spec.

Client HTTP được sinh tự động từ OpenAPI spec qua **@hey-api/openapi-ts**, kết
hợp **TanStack React Query** để quản lý trạng thái server-side và cache dữ liệu.

#### PostgreSQL 18 — Cơ sở dữ liệu quan hệ

PostgreSQL 18 được dùng làm cơ sở dữ liệu chính lưu trữ toàn bộ dữ liệu nghiệp
vụ: tài khoản người dùng, thông tin tàu/ga/tuyến, lịch chạy tàu, booking, thanh
toán, trạng thái ghế. Lý do chọn PostgreSQL: hỗ trợ mạnh ACID transactions đảm
bảo tính nhất quán khi nhiều hành khách cùng đặt vé, hỗ trợ
`SELECT ... FOR UPDATE` để khóa dòng khi giữ ghế (tránh race condition), tích
hợp tốt với Hibernate ORM và Flyway migration.

Schema được quản lý bằng **Flyway** để đảm bảo tính nhất quán và khả năng
rollback giữa các môi trường.

#### Valkey 8.0 (Redis-compatible) — Cache phân tán

Valkey (fork mã nguồn mở của Redis) được dùng làm lớp cache phân tán. Các kết
quả truy vấn tốn kém hoặc đọc nhiều được cache vào Valkey: danh sách chuyến tàu
, sơ đồ ghế theo toa, kết quả tìm kiếm ga. Lý do dùng Valkey thay vì cache trong
bộ nhớ (in-memory): cache được chia sẻ giữa nhiều instance backend (horizontal
scaling), dữ liệu cache tồn tại khi backend restart, hỗ trợ TTL linh hoạt.

Spring Cache được cấu hình với Valkey thông qua Spring Data Redis và Lettuce
connection pool.

#### Stripe — Cổng thanh toán trực tuyến

Stripe được tích hợp để xử lý thanh toán thẻ tín dụng/ghi nợ quốc tế. Hệ thống
sử dụng **Stripe Checkout Sessions** để tạo phiên thanh toán có thời hạn, và
nhận xác nhận kết quả qua **Stripe Webhooks** (các sự kiện
`checkout.session.completed`, `payment_intent.payment_failed`,
`checkout.session.expired`). Lý do chọn Stripe: API đơn giản và tài liệu chi
tiết, hỗ trợ tốt cho môi trường sandbox/test, xử lý bảo mật PCI DSS bởi Stripe,
có SDK Java chính thức (`stripe-java 31.4.0`).

#### JWT (JJWT 0.13) — Xác thực stateless

JSON Web Token (JWT) được dùng cho cơ chế xác thực người dùng. Khi đăng nhập
thành công, hệ thống cấp một **access token** (thời hạn 15 phút) và một
**refresh token** (thời hạn 7 ngày). Access token được gửi kèm mọi request cần
xác thực; khi hết hạn, client dùng refresh token để lấy access token mới mà
không cần đăng nhập lại. Refresh token được lưu (dạng hash) trong cơ sở dữ liệu
để hỗ trợ thu hồi (revocation) khi đăng xuất.

Lý do dùng JWT stateless: không cần lưu session trên server, phù hợp với kiến
trúc REST và khả năng scale horizontal.

#### OpenAPI 3.1 — Đặc tả API

Hợp đồng API được đặc tả theo chuẩn OpenAPI 3. Đặc tả này đóng vai trò nguồn sự
thật duy nhất cho giao tiếp giữa backend và frontend: frontend sinh client code
tự động, Swagger UI được tích hợp vào backend để tài liệu hóa và kiểm thử API.
Lý do dùng API-first với OpenAPI: giảm lỗi tích hợp giữa backend và frontend,
tài liệu API luôn đồng bộ với code.

**springdoc-openapi 2.8.14** được dùng để tự động generate và serve Swagger UI
từ annotation trong code backend.

#### Docker Compose — Container hóa môi trường

Toàn bộ hạ tầng phát triển (backend, frontend, PostgreSQL, Valkey) được
container hóa bằng Docker và điều phối qua Docker Compose. Mỗi service có health
check riêng; backend chỉ khởi động sau khi PostgreSQL và Valkey đã sẵn sàng. Lý
do dùng Docker Compose: đảm bảo môi trường phát triển nhất quán giữa các thành
viên nhóm và môi trường CI/CD, khởi động toàn bộ hạ tầng bằng một lệnh duy nhất.

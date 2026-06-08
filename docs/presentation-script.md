# Kịch bản Báo cáo Đồ án — Đảm bảo Chất lượng Phần mềm

> **Thời lượng mục tiêu:** 15–20 phút  
> **Người B (Phú):** Mở đầu + UC-07 → UC-12 + Kết luận  
> **Người A (Nguyên):** UC-01 → UC-06  
> **Quy ước:** 🎤 = Thuyết trình | 💻 = Thao tác trình chiếu/chạy kiểm thử trên máy

---

## Cấu trúc trình bày mỗi ca sử dụng

Mỗi ca sử dụng trình bày theo 3 phần:

1. **Giới thiệu chức năng** (~30%): Mô tả luồng chính, tác nhân, tiền điều kiện
2. **Chiến lược kiểm thử** (~30%): Kỹ thuật kiểm thử áp dụng (phân lớp tương đương, phân tích giá trị biên, kiểm thử chuyển trạng thái...), phân tầng kiểm thử (đơn vị/tích hợp/tải/bảo mật)
3. **Trình diễn kiểm thử** (~40%): Chạy bộ kiểm thử thực tế hoặc trình diễn ứng dụng + hiển thị kết quả

---

## Phần 1: Mở đầu (~3 phút) — Người B thuyết trình

### Trang chiếu 1: Trang bìa

🎤 **Người B:**

> Kính chào thầy và các bạn. Nhóm chúng em xin trình bày đồ án môn Đảm bảo chất lượng phần mềm với đề tài: **Ứng dụng đặt vé tàu trực tuyến**.
>
> Thành viên nhóm: Nguyễn Ngọc Phú — N22DCCN159 và Y Cao Nguyên Byă — N22DCCN200.

*(Chuyển trang chiếu)*

### Trang chiếu 2: Giới thiệu đề tài

🎤 **Người B:**

> **Ứng dụng đặt vé tàu hoả trực tuyến** nhằm hiện đại hoá quy trình bán vé cho công ty vận tải đường sắt. Hành khách tra cứu chuyến tàu, xem sơ đồ ghế, đặt vé và thanh toán trực tuyến mọi lúc mọi nơi mà không cần đến quầy giao dịch.
>
> **Các vấn đề cần giải quyết:**
>
> - Tra cứu chuyến tàu theo ga đi/đến, ngày, bộ lọc linh hoạt
> - Xem sơ đồ ghế theo toa, chọn ghế trực quan
> - Giữ chỗ tạm thời 15 phút (giữ rồi thanh toán), chống đặt trùng
> - Thanh toán an toàn qua cổng thanh toán quốc tế (Stripe)
> - Quản lý đặt vé: xem lịch sử, huỷ vé, giải phóng ghế tự động
> - Quản lý tài khoản: đăng ký, đăng nhập, cập nhật thông tin

*(Chuyển trang chiếu)*

### Trang chiếu 3: Kiến trúc hệ thống và Lược đồ cơ sở dữ liệu

🎤 **Người B:**

> Hệ thống được xây dựng theo kiến trúc **Kiến trúc sạch kết hợp Thiết kế hướng miền**, chia thành 3 tầng: Miền, Ứng dụng, và Hạ tầng — Quy tắc phụ thuộc đảm bảo tầng ngoài phụ thuộc tầng trong, không ngược lại.
>
> Gồm **5 mô-đun nghiệp vụ** độc lập: Người dùng, Ga, Tàu, Đặt vé, Thanh toán. Mỗi mô-đun đầy đủ 3 tầng, giao tiếp qua cổng giao diện — đảm bảo **khả năng kiểm thử** cao vì mọi phụ thuộc đều có thể giả lập.
>
> **Công nghệ:** Spring Boot 4.0 / Java 25, Next.js 16 / React 19, PostgreSQL 18 + Flyway, Valkey (bộ nhớ đệm), Stripe (thanh toán), SSE (thời gian thực).
>
> Về **lược đồ cơ sở dữ liệu**: 11 bảng, 5 phân hệ. Khoá chính UUID v7, xoá mềm, múi giờ UTC, khoá lạc quan trên bảng tình trạng ghế.
>
> **Chiến lược kiểm thử tổng thể** của dự án:
> - **3 mức kiểm thử**: phân tích (đối chiếu lược đồ), thiết kế (duyệt/thanh tra), hiện thực (kiểm thử tự động)
> - **Kim tự tháp kiểm thử**: Kiểm thử đơn vị (ca sử dụng độc lập, giả lập phụ thuộc) → Kiểm thử tích hợp (toàn tuyến HTTP + TestContainers PostgreSQL) → Kiểm thử tải (50 luồng đồng thời)
> - **Kỹ thuật áp dụng**: Phân lớp tương đương, Phân tích giá trị biên, Kiểm thử chuyển trạng thái, Kiểm thử bảo mật
> - Tổng cộng hơn **160 trường hợp kiểm thử** cho 12 ca sử dụng
>
> Bây giờ em nhường lời cho bạn Nguyên trình bày 6 ca sử dụng đầu tiên.

---

## Phần 2: UC-01 → UC-06 (~7 phút) — Người A thuyết trình

### Trang chiếu 4: UC-01 — Đăng ký tài khoản (~1,5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 01: Đăng ký tài khoản. Tác nhân là khách hàng chưa có tài khoản.
>
> **Luồng chính:** Nhập email, mật khẩu, họ tên → kiểm tra hợp lệ → kiểm tra email duy nhất → băm mật khẩu BCrypt → tạo tài khoản vai trò KHÁCH HÀNG → trả thông tin (không có mật khẩu).
>
> **Chiến lược kiểm thử — 20 trường hợp (12 phía sau + 8 giao diện):**
> - **Kiểm thử đơn vị** (3): Ca sử dụng tạo tài khoản thành công, phát sự kiện đăng ký, trả lỗi khi email tồn tại. Giả lập kho dữ liệu và bộ băm mật khẩu.
> - **Kiểm thử tích hợp** (4): Toàn tuyến HTTP — đăng ký thành công trả mã 201, email trùng trả mã 409, email sai định dạng, mật khẩu dưới 8 ký tự.
> - **Kiểm thử bảo mật** (3): Chèn câu lệnh SQL trong email bị từ chối, mã XSS trong họ tên trả về dạng dữ liệu thuần, phản hồi không chứa trường mật khẩu.
> - **Kiểm thử tải** (1): 50 luồng đăng ký cùng email — chỉ 1 thành công nhờ ràng buộc duy nhất của cơ sở dữ liệu.
> - **Kiểm thử giao diện** (8): Biểu mẫu hiển thị đủ trường, lỗi biểu mẫu rỗng, mật khẩu tối thiểu, xác nhận mật khẩu không khớp, gọi giao diện lập trình đúng, lược đồ từ chối họ tên ngắn, xác nhận không khớp, nhận diện lỗi email tồn tại.
>
> Kỹ thuật kiểm thử: **Phân lớp tương đương** cho email (định dạng hợp lệ/không hợp lệ, tồn tại/mới), **Phân tích giá trị biên** cho mật khẩu (7 so với 8 ký tự).

💻 **Người B:** *(Trình diễn ứng dụng: đăng ký thành công → email trùng → mã 409. Sau đó mở đầu cuối chạy kiểm thử)*

```bash
./gradlew test --tests '*RegisterUser*' --tests '*AuthControllerRegisterTest*'
```

💻 **Người B:** *(Hiển thị kết quả: tất cả kiểm thử đều đạt, nhấn mạnh kiểm thử tải "1 thành công, 49 xung đột")*

---

### Trang chiếu 5: UC-02 — Đăng nhập (~1,5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 02: Đăng nhập. Tiền điều kiện: đã đăng ký.
>
> **Luồng chính:** Nhập email + mật khẩu → tìm người dùng → so sánh an toàn thời gian BCrypt → tạo mã truy cập JWT (15 phút) + mã làm mới (băm SHA-256 lưu cơ sở dữ liệu).
>
> **Chiến lược kiểm thử — 20 trường hợp (13 phía sau + 7 giao diện):**
> - **Kiểm thử đơn vị** (5): Xác thực thành công trả cặp mã thông báo, email không tồn tại trả lỗi, mật khẩu sai trả lỗi, gọi đối chiếu mật khẩu đúng tham số, tạo và lưu cặp mã thông báo.
> - **Kiểm thử tích hợp** (4): Toàn tuyến — đăng nhập thành công mã 200 + cặp mã, thông tin sai mã 401, thiếu trường bắt buộc, email sai định dạng.
> - **Kiểm thử bảo mật** (3): Chèn SQL trong email bị từ chối, phản hồi giống nhau cho email sai và mật khẩu sai (chống liệt kê tài khoản), phản hồi không chứa mật khẩu.
> - **Kiểm thử tải** (1): 50 luồng đăng nhập đồng thời — tất cả phải thành công và nhận mã khác nhau.
> - **Kiểm thử giao diện** (7): Biểu mẫu hiển thị đủ trường, lỗi xác thực biểu mẫu rỗng, email sai định dạng, gọi giao diện lập trình đúng, lược đồ chấp nhận hợp lệ, lược đồ từ chối rỗng, nhận diện lỗi thông tin sai.
>
> Kỹ thuật: **Kiểm thử chuyển trạng thái** (vòng đời mã thông báo: hoạt động → thu hồi), **Kiểm thử bảo mật** (tấn công thời gian, liệt kê tài khoản).

💻 **Người B:** *(Trình diễn: đăng nhập thành công → sai mật khẩu → mã 401. Chạy kiểm thử)*

```bash
./gradlew test --tests '*LoginUser*' --tests '*AuthControllerLoginTest*'
```

💻 **Người B:** *(Hiển thị tất cả kiểm thử đều đạt)*

---

### Trang chiếu 6: UC-03 — Đăng xuất (~40 giây)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 03: Đăng xuất — thiết kế **bình thường hoá** (luôn trả mã 200).
>
> **Luồng chính:** Gửi mã làm mới trong thân yêu cầu → băm SHA-256 → tìm mã đang hoạt động → đánh dấu THU HỒI. Mã đã thu hồi hoặc không tồn tại vẫn trả mã 200.
>
> **Kiểm thử (12 trường hợp):** Kỹ thuật **Kiểm thử chuyển trạng thái** — mã chuyển từ HOẠT ĐỘNG → THU HỒI. Kiểm thử bình thường hoá: mã đã thu hồi hoặc không xác định vẫn trả mã 200. Chèn SQL trong thân yêu cầu bị từ chối. Kiểm thử tải 50 luồng đăng xuất cùng mã.

💻 **Người B:** *(Trình diễn đăng xuất nhanh trên ứng dụng)*

---

### Trang chiếu 7: UC-04 — Quản lý thông tin cá nhân (~1 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 04: Quản lý thông tin cá nhân. 2 thao tác: xem (GET /me) và cập nhật (PUT /me).
>
> **Kiểm thử (27 trường hợp — 20 phía sau + 7 giao diện):** Kỹ thuật **Phân lớp tương đương** trên từng trường: điện thoại (định dạng hợp lệ/không hợp lệ), ngày sinh (quá khứ/tương lai), giới tính (NAM/NỮ/KHÁC/không hợp lệ), số định danh (9/12 chữ số/không hợp lệ). **Kiểm thử kiểm soát truy cập**: mã định danh JWT phải khớp — không cho xem/sửa hồ sơ người khác. Kiểm thử tải 50 luồng cập nhật đồng thời.

💻 **Người B:** *(Trình diễn: cập nhật hồ sơ thành công)*

---

### Trang chiếu 8: UC-05 — Tra cứu ga tàu (~1 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 05: Tra cứu ga tàu — 3 cách: tìm kiếm mờ, duyệt phân trang dựa con trỏ, xem chi tiết theo mã.
>
> **Kỹ thuật triển khai:**
> - **Phân trang dựa con trỏ:** Thay vì dùng OFFSET (chậm ở trang lớn, dễ trùng/sót khi dữ liệu thay đổi), dùng con trỏ — mỗi phản hồi trả `nextCursor`, yêu cầu tiếp gửi con trỏ đó để lấy trang kế. Hiệu năng O(1) không phụ thuộc số trang.
> - **Bộ nhớ đệm (Valkey):** Kết quả tìm kiếm được lưu đệm theo khoá = truy vấn + giới hạn. Chiến lược **đệm bên cạnh**: đọc bộ đệm trước → trượt thì truy vấn cơ sở dữ liệu → ghi bộ đệm.
>
> **Kiểm thử (15 trường hợp — 9 phía sau + 6 giao diện):** Kỹ thuật **Phân tích giá trị biên** cho truy vấn tìm kiếm: chuỗi rỗng, 1 ký tự, từ khoá khớp, không khớp. Phân trang con trỏ: trang đầu, trang giữa, trang cuối (hết dữ liệu). Kiểm tra bộ đệm: yêu cầu 1 = trượt đệm (truy cập cơ sở dữ liệu), yêu cầu 2 = trúng đệm (không truy cập). Ga không tồn tại → mã 404. Kiểm thử tải 50 luồng tìm kiếm đồng thời.

💻 **Người B:** *(Trình diễn hộp gợi ý: gõ "Hà N" → danh sách gợi ý)*

---

### Trang chiếu 9: UC-06 — Tra cứu chuyến tàu (~1,5 phút)

💻 **Người B thao tác** | 🎤 **Người A thuyết trình**

🎤 **Người A:**

> Ca sử dụng 06: Tra cứu chuyến tàu — bộ lọc đa tiêu chí, sắp xếp, phân trang.
>
> **Kỹ thuật triển khai:**
> - **Phân trang dựa con trỏ:** Hỗ trợ sắp xếp đa tiêu chí (giờ khởi hành, giá, ghế trống). Con trỏ mã hoá cặp `(giá_trị_sắp_xếp, mã)` — đảm bảo không trùng/sót ngay cả khi có đặt vé mới chen giữa 2 lần phân trang.
> - **Bộ nhớ đệm (Valkey):** Lưu đệm kết quả tìm kiếm theo khoá tổng hợp = tham số lọc + sắp xếp + con trỏ. Chiến lược **đệm bên cạnh** với thời gian sống ngắn (30–60 giây). Vô hiệu bộ đệm: khi ghế bị giữ/đặt → xoá các khoá đệm liên quan đến chuyến đó.
>
> **Kiểm thử (17 trường hợp — 14 phía sau + 3 giao diện):**
> - **Phân lớp tương đương**: Tổ hợp bộ lọc — chỉ ga đi, ga đi + đến, ga đi + đến + ngày, đầy đủ. Sắp xếp: theo thời gian tăng/giảm, theo giá, theo ghế trống.
> - **Phân tích giá trị biên**: giá tối thiểu = 0, giá tối đa = MAX, ngày = hôm nay, ngày = quá khứ (kết quả rỗng).
> - **Kiểm thử tích hợp**: Phân trang con trỏ không bỏ sót/trùng bản ghi khi dữ liệu thay đổi giữa các trang.
> - **Kiểm thử tải**: 50 luồng tìm kiếm đồng thời — tất cả trả kết quả đúng.
>
> Kỹ thuật đặc biệt: kiểm thử **phản hồi làm giàu** — mỗi chuyến phải chứa đúng thông tin tàu, ga, số ghế trống tính toán thời gian thực.

💻 **Người B:** *(Trình diễn: tìm kiếm Hà Nội → Đà Nẵng → hiển thị kết quả. Chạy kiểm thử)*

```bash
./gradlew test --tests '*SearchScheduledTrip*'
```

🎤 **Người A:**

> Bây giờ em nhường lại cho bạn Phú trình bày phần đặt vé và thanh toán — phần phức tạp nhất về mặt đảm bảo chất lượng.

---

## Phần 3: UC-07 → UC-12 (~8 phút) — Người B thuyết trình

### Trang chiếu 10: UC-07 — Xem sơ đồ ghế chuyến tàu (~1,5 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 07: Xem sơ đồ ghế — 2 góc nhìn: danh sách phẳng phân trang và sơ đồ theo toa. 3 trạng thái ghế: CÒN TRỐNG, ĐANG GIỮ, ĐÃ ĐẶT. Cập nhật thời gian thực qua SSE.
>
> **Kỹ thuật triển khai — Bộ nhớ đệm (Valkey):**
> - Sơ đồ ghế theo toa được lưu đệm trong Valkey theo khoá = `trip:{mã_chuyến}:coach-seat-map:page:{trang}`.
> - Chiến lược **đệm bên cạnh + vô hiệu hoá dựa sự kiện**: khi sự kiện ThayĐổiTrạngThaiGhế phát ra (ghế bị giữ/đặt/giải phóng), bộ đệm của chuyến tương ứng bị xoá ngay → yêu cầu tiếp theo sẽ truy vấn cơ sở dữ liệu và ghi đệm mới.
> - Giảm đáng kể tải cơ sở dữ liệu cho trang sơ đồ ghế — trang này được nhiều người dùng mở đồng thời khi chọn chuyến.
>
> **Chiến lược kiểm thử (16 trường hợp — 10 phía sau + 6 giao diện):**
> - **Kiểm thử chuyển trạng thái**: Ghế có 4 trạng thái (CÒN TRỐNG → ĐANG GIỮ → ĐÃ ĐẶT, ĐANG GIỮ → CÒN TRỐNG khi huỷ/hết hạn). Kiểm thử mỗi chuyển đổi hiển thị đúng trên giao diện.
> - **Kiểm thử tích hợp (SSE)**: Mở 2 máy khách cùng xem 1 chuyến → máy khách A đặt ghế → máy khách B nhận sự kiện ThayĐổiTrạngThaiGhế trong dưới 1 giây mà không cần tải lại.
> - **Phân tích giá trị biên**: Chuyến có 0 ghế trống (tất cả ĐÃ ĐẶT), chuyến tất cả ghế trống, chuyến không tồn tại → mã 404.
> - **Kiểm thử bộ đệm**: Trúng/trượt đệm sơ đồ ghế, vô hiệu đệm đúng khi trạng thái ghế thay đổi (dựa sự kiện).

💻 **Người A:** *(Mở 2 thẻ cùng chuyến → thẻ 1 chọn ghế → thẻ 2 thấy ghế đổi màu thời gian thực)*

🎤 **Người B:**

> Đây là trình diễn trực tiếp SSE thời gian thực — khi thẻ 1 giữ ghế, thẻ 2 nhận sự kiện ngay lập tức.

---

### Trang chiếu 11: UC-08 — Đặt vé tàu (~2 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 08: Đặt vé — ca sử dụng phức tạp nhất, **23 trường hợp kiểm thử phía sau + 4 giao diện**.
>
> **Luồng chính:** Gửi mã chuyến, danh sách mã ghế, danh sách hành khách, khoá bình thường hoá → kiểm tra hợp lệ → kiểm tra ghế CÒN TRỐNG → giữ tất cả hoặc không giữ gì → tạo đặt vé trạng thái ĐANG GIỮ (15 phút) → phát sự kiện.
>
> **Chiến lược kiểm thử theo 5 tiêu chí:**
>
> **1. Xử lý chính xác (7 kiểm thử đơn vị):**
> - Luồng thành công: tạo đặt vé, trạng thái = ĐANG GIỮ, tổng giá = giá × số ghế
> - Bình thường hoá: cùng khoá bình thường hoá trả cùng đặt vé, không tạo mới
> - Luồng lỗi: người dùng không tồn tại, chuyến không tồn tại, đã có giữ chỗ đang hoạt động, ghế không khả dụng, số hành khách khác số ghế
>
> **2. Đồng thời — Khoá lạc quan (2 kiểm thử tải):**
> - Bảng `trip_seat_availability` có cột `version` (số nguyên). Khi giữ ghế, hệ thống thực hiện: `UPDATE ... SET status='HELD', version=version+1 WHERE id=:id AND version=:expectedVersion`. Nếu phiên bản không khớp (yêu cầu khác đã chen) → cập nhật 0 dòng → trả `GHẾ_KHÔNG_KHẢ_DỤNG`.
> - Kết hợp **tất cả hoặc không gì**: đặt nhiều ghế trong 1 giao dịch — nếu bất kỳ ghế nào thất bại → hoàn tác toàn bộ, không giữ một phần.
> - Kiểm thử tải: 50 luồng đặt cùng ghế → chỉ 1 thành công, 49 nhận GHẾ_KHÔNG_KHẢ_DỤNG — chứng minh không bao giờ đặt trùng.
>
> **3. Tất cả hoặc không gì (1 kiểm thử):**
> - Chọn 3 ghế, 1 đã ĐANG GIỮ → tất cả bị từ chối, không giữ một phần
>
> **4. Phát sự kiện (2 kiểm thử):**
> - Sự kiện TạoĐặtVé cho dịch vụ đặt vé
> - Sự kiện ThayĐổiTrạngThaiGhế cho phát sóng SSE
>
> **5. Bảo mật (kiểm thử tích hợp):**
> - Mã 401 khi không có mã thông báo, mã định danh người dùng luôn lấy từ JWT (chống mạo danh)

💻 **Người A:** *(Trình diễn: chọn 2 ghế → điền thông tin → gửi → thành công. Sau đó chạy kiểm thử tải)*

```bash
./gradlew test --tests '*CreateBookingStressTest*'
```

💻 **Người A:** *(Hiển thị kết quả: "1 thành công, 49 ghế_không_khả_dụng" — chứng minh an toàn đồng thời)*

🎤 **Người B:**

> Kiểm thử tải này chứng minh hệ thống đảm bảo tính nhất quán dữ liệu dưới tải đồng thời 50 yêu cầu — không bao giờ xảy ra đặt trùng.

---

### Trang chiếu 12: UC-09 — Xem đặt vé (~1 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 09: Xem đặt vé — danh sách phân trang + chi tiết tổng hợp.
>
> **Kiểm thử (20 trường hợp — 16 phía sau + 4 giao diện):**
> - **Kiểm thử kiểm soát truy cập (Bảo mật)**: Gọi giao diện lập trình với mã đặt vé của người khác → mã 403 Cấm. Đây là kiểm thử quan trọng vì IDOR (Tham chiếu đối tượng trực tiếp không an toàn) là lỗ hổng phổ biến.
> - **Kiểm thử phân trang**: Xác minh thứ tự mới nhất trước, siêu dữ liệu (tổng mục, có trang sau) chính xác.
> - **Kiểm thử tổng hợp**: Chi tiết đặt vé phải chứa đầy đủ thông tin chuyến, ghế, thanh toán — không thiếu trường.
> - **Kiểm thử tải**: 50 luồng xem danh sách đồng thời, 50 luồng xem chi tiết đồng thời.

💻 **Người A:** *(Trình diễn: vào "Đặt vé của tôi" → xem danh sách → nhấn chi tiết)*

---

### Trang chiếu 13: UC-10 — Huỷ đặt vé (~1 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 10: Huỷ đặt vé — cho phép huỷ khi trạng thái ĐANG GIỮ hoặc ĐÃ XÁC NHẬN.
>
> **Kiểm thử (15 trường hợp — 12 phía sau + 3 giao diện) — kỹ thuật Kiểm thử chuyển trạng thái:**
> - Chuyển đổi hợp lệ: ĐANG GIỮ → ĐÃ HUỶ (giải phóng ghế), ĐÃ XÁC NHẬN → ĐÃ HUỶ (giải phóng ghế + đánh dấu hoàn tiền)
> - Chuyển đổi không hợp lệ: ĐÃ HUỶ → ĐÃ HUỶ → mã 409 Xung đột
> - **Kiểm thử sự kiện**: Sự kiện HuỷĐặtVé chứa cờ cầnHoànTiền = đúng khi huỷ ĐÃ XÁC NHẬN
> - **Kiểm thử SSE**: Sự kiện ThayĐổiTrạngThaiGhế phát sóng khi ghế giải phóng → các máy khách khác thấy ghế trống
> - **Kiểm soát truy cập**: Huỷ đặt vé người khác → mã 403
> - **Kiểm thử tải**: 50 luồng huỷ cùng đặt vé — chỉ 1 thành công

💻 **Người A:** *(Trình diễn: nhấn huỷ → xác nhận → trạng thái chuyển ĐÃ HUỶ → ghế trở lại trống trên sơ đồ)*

---

### Trang chiếu 14: UC-11 — Xem thanh toán (~40 giây)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 11: Xem thanh toán — tra cứu theo mã thanh toán hoặc mã đặt vé.
>
> **Kiểm thử (19 trường hợp — 13 phía sau + 6 giao diện):** Kiểm soát truy cập mã 403 khi xem thanh toán người khác, mã 404 khi không tồn tại, xác minh đường dẫn thanh toán chỉ có giá trị khi trạng thái = CHỜ XỬ LÝ (Phân lớp tương đương trên trạng thái thanh toán: CHỜ/ĐÃ THANH TOÁN/THẤT BẠI/ĐÃ HUỶ/ĐÃ HOÀN TIỀN). Kiểm thử tải 50 luồng xem đồng thời.

💻 **Người A:** *(Trình diễn nhanh: xem chi tiết thanh toán với trạng thái CHỜ XỬ LÝ)*

---

### Trang chiếu 15: UC-12 — Thanh toán (~2 phút)

💻 **Người A thao tác** | 🎤 **Người B thuyết trình**

🎤 **Người B:**

> Ca sử dụng 12: Thanh toán Stripe — ca sử dụng phức tạp nhất về mặt **quản lý trạng thái** và **tích hợp bên ngoài**.
>
> **Luồng:** Tạo phiên thanh toán Stripe → chuyển hướng người dùng → người dùng thanh toán → Stripe gửi webhook → hệ thống xác nhận đặt vé.
>
> **Chiến lược kiểm thử — 17 trường hợp (13 phía sau + 4 giao diện), 5 nhóm:**
>
> **1. Xử lý webhook (Kiểm thử chuyển trạng thái):**
> - `checkout.session.completed`: ĐANG GIỮ → ĐÃ XÁC NHẬN, ĐANG GIỮ → ĐÃ ĐẶT, CHỜ → ĐÃ THANH TOÁN
> - `checkout.session.expired`: CHỜ → ĐÃ HUỶ
> - `payment_intent.payment_failed`: CHỜ → THẤT BẠI
>
> **2. Kiểm thử bình thường hoá:**
> - Stripe có thể gửi webhook trùng lặp → kiểm thử gửi sự kiện 2 lần → lần 2 không thao tác, không ngoại lệ
>
> **3. Trường hợp biên thanh toán muộn:**
> - Người dùng thanh toán SAU khi đặt vé đã bị huỷ (hết 15 phút) → hệ thống tự động hoàn tiền qua Stripe
> - Kiểm thử: webhook hoàn thành + đặt vé trạng thái = ĐÃ HUỶ → kích hoạt hoàn tiền tự động → thanh toán = ĐÃ HOÀN TIỀN
>
> **4. Xác minh chữ ký (Bảo mật):**
> - Nội dung webhook phải xác minh chữ ký Stripe → từ chối nếu chữ ký không hợp lệ (chống giả mạo webhook)
>
> **5. Kiểm thử tích hợp:**
> - Luồng đầy đủ với giả lập Stripe: tạo phiên → xác minh định dạng đường dẫn → mô phỏng webhook → xác minh trạng thái cuối cùng

💻 **Người A:** *(Trình diễn luồng đầy đủ: nhấn thanh toán → chuyển hướng Stripe → nhập thẻ kiểm thử 4242... → thành công → chuyển hướng về ứng dụng → trạng thái ĐÃ XÁC NHẬN)*

🎤 **Người B:**

> Thẻ kiểm thử `4242 4242 4242 4242` là thẻ kiểm thử của Stripe cho môi trường thử nghiệm — không tính phí thật.

💻 **Người A:** *(Chạy kiểm thử)*

```bash
./gradlew test --tests '*Payment*UseCase*'
```

💻 **Người A:** *(Hiển thị kết quả: XửLýThanhToánThànhCông, XửLýThanhToánThấtBại, HuỷThanhToánChờ, HoànTiền — tất cả đều đạt)*

---

## Phần 4: Kết luận (~1,5 phút) — Người B thuyết trình

### Trang chiếu 16: Kết luận

🎤 **Người B:**

> Tổng kết, nhóm đã hoàn thành **12 ca sử dụng** với hơn **160 trường hợp kiểm thử** tự động hoá, áp dụng đầy đủ các kỹ thuật kiểm thử phần mềm:
>
> **Kỹ thuật kiểm thử đã áp dụng:**
> - **Phân lớp tương đương**: Phân lớp đầu vào cho kiểm tra hợp lệ (định dạng email, độ dài mật khẩu, trạng thái thanh toán...)
> - **Phân tích giá trị biên**: Giá trị biên cho mật khẩu (7/8 ký tự), truy vấn tìm kiếm (rỗng/1 ký tự), phân trang (trang đầu/cuối)
> - **Kiểm thử chuyển trạng thái**: Vòng đời đặt vé (ĐANG GIỮ→ĐÃ XÁC NHẬN→ĐÃ HUỶ), vòng đời ghế (CÒN TRỐNG→ĐANG GIỮ→ĐÃ ĐẶT), vòng đời thanh toán (CHỜ→ĐÃ THANH TOÁN/THẤT BẠI/ĐÃ HOÀN TIỀN)
> - **Kiểm thử đồng thời/tải**: 50 luồng đồng thời cho các chức năng then chốt (đăng ký, đặt vé, thanh toán)
> - **Kiểm thử bảo mật**: Chèn SQL, XSS, tấn công thời gian, liệt kê tài khoản, tham chiếu đối tượng trực tiếp không an toàn, xác minh chữ ký webhook
>
> **Điểm nổi bật đảm bảo chất lượng:**
> - Kiến trúc sạch đảm bảo **khả năng kiểm thử** — mọi phụ thuộc đều giả lập được
> - Kim tự tháp kiểm thử đầy đủ: đơn vị → tích hợp → tải → bảo mật
> - Kiểm thử đồng thời chứng minh **tính nhất quán dữ liệu** dưới tải
> - Kiểm thử bình thường hoá đảm bảo **độ tin cậy** khi thử lại mạng
>
> Hướng phát triển: mở rộng phạm vi kiểm thử với kiểm thử đột biến, thêm kiểm thử đầu cuối với Playwright, đánh giá hiệu năng dưới tải giống sản xuất.
>
> Em xin cảm ơn thầy và các bạn đã lắng nghe. Nhóm sẵn sàng trả lời câu hỏi ạ.

---

## Tổng kết thời gian

| Phần | Nội dung | Thời lượng | Người thuyết trình |
|------|----------|-----------|-------------------|
| 1 | Mở đầu + Giới thiệu đề tài + Kiến trúc | ~3 phút | Người B (Phú) |
| 2 | UC-01 → UC-06 | ~7 phút | Người A (Nguyên) |
| 3 | UC-07 → UC-12 | ~8 phút | Người B (Phú) |
| 4 | Kết luận | ~1,5 phút | Người B (Phú) |
| | **Tổng** | **~19,5 phút** | |

---

## Chuẩn bị trước buổi báo cáo

### Môi trường

- Các bộ chứa Docker đang chạy (PostgreSQL + Valkey + phía sau + phía trước)
- Trình duyệt mở sẵn ứng dụng tại localhost:3000, đã có dữ liệu gieo
- Đầu cuối mở sẵn tại thư mục dự án, đã biên dịch xong
- 2 thẻ trình duyệt cho trình diễn SSE thời gian thực (UC-07)

### Chạy kiểm thử nhanh (nếu thầy yêu cầu chạy thêm)

```bash
# Chạy toàn bộ bộ kiểm thử
./gradlew test

# Chạy kiểm thử theo mô-đun
./gradlew test --tests '*booking*'
./gradlew test --tests '*payment*'

# Chạy chỉ kiểm thử tải
./gradlew test --tests '*StressTest*'

# Kiểm thử giao diện
cd frontend/customer && bun run test
```

### Chế độ kiểm thử Stripe

- Thẻ: `4242 4242 4242 4242`, ngày hết hạn bất kỳ trong tương lai, CVC bất kỳ 3 số

### Điều chỉnh thời gian

- **Thiếu thời gian**: Rút ngắn UC-03, UC-04, UC-11 (chỉ nói 1–2 câu mỗi ca sử dụng, không trình diễn)
- **Thừa thời gian**: Trình diễn xung đột đặt vé đồng thời ở UC-08 (mở 2 trình duyệt, đặt cùng ghế), trình diễn SSE chi tiết hơn ở UC-07
- **Thầy hỏi về kiểm thử**: Sẵn sàng chạy bất kỳ lớp kiểm thử nào, hiển thị báo cáo phạm vi

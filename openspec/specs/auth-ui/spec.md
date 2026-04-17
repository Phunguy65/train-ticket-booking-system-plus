# ADDED Requirements

## Requirement: Login Form

Hệ thống SHALL cung cấp form đăng nhập với các field: email và password.

### Scenario: Successful login

- **WHEN** user nhập email và password hợp lệ và submit form
- **THEN** hệ thống gọi `loginMutation` với credentials
- **THEN** sau khi API trả về success, redirect user đến trang chủ
  (`/[locale]/`)

### Scenario: Invalid credentials

- **WHEN** user submit form với email hoặc password sai
- **THEN** hệ thống hiển thị Alert với message "Email hoặc mật khẩu không đúng"
  (translated)
- **THEN** password field được clear, email field giữ nguyên

### Scenario: Validation error - empty fields

- **WHEN** user submit form với field trống
- **THEN** hệ thống hiển thị inline error dưới mỗi field trống
- **THEN** form không được submit đến API

### Scenario: Validation error - invalid email format

- **WHEN** user nhập email không đúng format (không có @, v.v.)
- **THEN** hệ thống hiển thị inline error "Email không hợp lệ" (translated)

### Scenario: Loading state

- **WHEN** form đang submit (API call in progress)
- **THEN** submit button hiển thị spinner và text "Đang đăng nhập..."
  (translated)
- **THEN** tất cả form fields bị disabled

### Scenario: Network error

- **WHEN** API call thất bại do network issue
- **THEN** hệ thống hiển thị Toast notification "Không thể kết nối. Vui lòng thử
  lại." (translated)

## Requirement: Register Form

Hệ thống SHALL cung cấp form đăng ký với các field: họ tên, email, password, và
xác nhận password.

### Scenario: Successful registration

- **WHEN** user nhập thông tin hợp lệ và submit form
- **THEN** hệ thống gọi `registerMutation` với data
- **THEN** sau khi API trả về success, redirect đến login page với query param
  `?registered=true`
- **THEN** login page hiển thị success message "Đăng ký thành công! Vui lòng
  đăng nhập." (translated)

### Scenario: Email already exists

- **WHEN** user submit form với email đã được đăng ký
- **THEN** hệ thống hiển thị Alert với message "Email này đã được đăng ký"
  (translated)
- **THEN** hiển thị link "Đăng nhập" để user có thể navigate

### Scenario: Password mismatch

- **WHEN** password và confirm password không khớp
- **THEN** hệ thống hiển thị inline error dưới confirm password field "Mật khẩu
  xác nhận không khớp" (translated)

### Scenario: Password too short

- **WHEN** user nhập password ít hơn 8 ký tự
- **THEN** hệ thống hiển thị inline error "Mật khẩu phải có ít nhất 8 ký tự"
  (translated)

### Scenario: Full name validation

- **WHEN** user nhập họ tên ít hơn 2 ký tự
- **THEN** hệ thống hiển thị inline error "Họ và tên phải có ít nhất 2 ký tự"
  (translated)

### Scenario: Loading state

- **WHEN** form đang submit
- **THEN** submit button hiển thị spinner và text "Đang đăng ký..." (translated)
- **THEN** tất cả form fields bị disabled

## Requirement: Password Input Component

Hệ thống SHALL cung cấp password input với toggle show/hide password.

### Scenario: Toggle password visibility

- **WHEN** user click icon eye trong password field
- **THEN** password hiển thị dạng text (visible)
- **WHEN** user click icon eye lần nữa
- **THEN** password hiển thị dạng dots (hidden)

### Scenario: Default state

- **WHEN** password field được render
- **THEN** password mặc định ở trạng thái hidden (type="password")

## Requirement: Auth Card Layout

Hệ thống SHALL wrap login và register forms trong consistent card layout.

### Scenario: Card structure

- **WHEN** auth page được render
- **THEN** hiển thị card centered với max-width 448px
- **THEN** card chứa: title, subtitle, form, và link đến page còn lại

### Scenario: Responsive behavior

- **WHEN** viewport width < 640px (mobile)
- **THEN** card chiếm full width với padding
- **WHEN** viewport width >= 640px
- **THEN** card centered với max-width

## Requirement: Navigation Links

Hệ thống SHALL cung cấp navigation links giữa login và register pages.

### Scenario: Login to Register

- **WHEN** user ở login page
- **THEN** hiển thị text "Chưa có tài khoản?" với link "Đăng ký ngay"
  (translated)
- **WHEN** user click link
- **THEN** navigate đến `/[locale]/register`

### Scenario: Register to Login

- **WHEN** user ở register page
- **THEN** hiển thị text "Đã có tài khoản?" với link "Đăng nhập" (translated)
- **WHEN** user click link
- **THEN** navigate đến `/[locale]/login`

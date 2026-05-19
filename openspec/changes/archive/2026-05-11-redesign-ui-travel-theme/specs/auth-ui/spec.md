## MODIFIED Requirements

### Requirement: Login Form

Hệ thống SHALL cung cấp form đăng nhập với các field: email và password, rendered inside the new `AuthLayout` split-screen component instead of `AuthCard`.

#### Scenario: Login page uses AuthLayout with TrainJourney illustration

- **WHEN** user navigates to `/login`
- **THEN** the page renders using `AuthLayout` with `TrainJourney` illustration in the left panel
- **THEN** the login form (email, password, submit button) appears in the right panel
- **THEN** on mobile, only the form panel is visible (illustration hidden)

#### Scenario: Successful login

- **WHEN** user nhập email và password hợp lệ và submit form
- **THEN** hệ thống gọi `loginMutation` với credentials
- **THEN** sau khi API trả về success, redirect user đến trang chủ (`/[locale]/`)

#### Scenario: Invalid credentials

- **WHEN** user submit form với email hoặc password sai
- **THEN** hệ thống hiển thị Alert với message lỗi (translated)
- **THEN** password field bị clear

#### Scenario: Registration success banner

- **WHEN** user arrives at login page with `?registered=true` query param
- **THEN** a success Alert is displayed above the form

### Requirement: Register Form

Hệ thống SHALL cung cấp form đăng ký với các field: fullName, email, password, confirmPassword, rendered inside the new `AuthLayout` split-screen component.

#### Scenario: Register page uses AuthLayout with StationPlatform illustration

- **WHEN** user navigates to `/register`
- **THEN** the page renders using `AuthLayout` with `StationPlatform` illustration in the left panel
- **THEN** the register form appears in the right panel
- **THEN** on mobile, only the form panel is visible

#### Scenario: Successful registration

- **WHEN** user fills all fields correctly and submits
- **THEN** hệ thống gọi `registerMutation`
- **THEN** redirect to `/login?registered=true`

#### Scenario: Email already exists

- **WHEN** user submits with an email that already exists
- **THEN** inline Alert shows translated error message

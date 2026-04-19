# MODIFIED Requirements

## Requirement: Translation Loading

Hệ thống SHALL load translations based on current locale for both the existing
auth experience and the full customer booking experience.

### Scenario: Vietnamese translations

- **WHEN** locale là `vi`
- **THEN** load translations từ `messages/vi.json` for auth pages, homepage
  search, search results, seat selection, booking, account, profile, navigation,
  statuses, shared feedback messages, booking stepper labels, payment-state
  messages, countdown text, and price-breakdown copy

### Scenario: English translations

- **WHEN** locale là `en`
- **THEN** load translations từ `messages/en.json` for auth pages, homepage
  search, search results, seat selection, booking, account, profile, navigation,
  statuses, shared feedback messages, booking stepper labels, payment-state
  messages, countdown text, and price-breakdown copy

### Scenario: Missing translation key

- **WHEN** translation key không tồn tại trong file
- **THEN** display key name as fallback (for debugging)

## Requirement: Translated Form Labels

Hệ thống SHALL display form labels, placeholders, button text, and helper copy
in the current locale across auth and customer booking forms.

### Scenario: Vietnamese form labels

- **WHEN** locale là `vi`
- **THEN** login form hiển thị:
    - Title: "Đăng nhập"
    - Email label: "Email"
    - Password label: "Mật khẩu"
    - Submit button: "Đăng nhập"
- **THEN** customer booking forms hiển thị translated labels and actions for
  station selection, departure date, search submission, seat selection, booking
  confirmation, payment progression, retry/start-over payment actions, profile
  editing, and cancellation confirmation

### Scenario: English form labels

- **WHEN** locale là `en`
- **THEN** login form hiển thị:
    - Title: "Sign In"
    - Email label: "Email"
    - Password label: "Password"
    - Submit button: "Sign In"
- **THEN** customer booking forms hiển thị translated labels and actions for
  station selection, departure date, search submission, seat selection, booking
  confirmation, payment progression, retry/start-over payment actions, profile
  editing, and cancellation confirmation

## Requirement: Translated Error Messages

Hệ thống SHALL display validation, API, and booking-flow feedback messages in
the current locale.

### Scenario: Vietnamese error messages

- **WHEN** locale là `vi` và validation fails
- **THEN** hiển thị Vietnamese error messages (e.g., "Email không hợp lệ")
- **THEN** search, booking, payment countdown/status, cancellation, profile, and
  network feedback messages are shown in Vietnamese

### Scenario: English error messages

- **WHEN** locale là `en` và validation fails
- **THEN** hiển thị English error messages (e.g., "Invalid email address")
- **THEN** search, booking, payment countdown/status, cancellation, profile, and
  network feedback messages are shown in English

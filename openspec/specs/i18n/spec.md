## Purpose

Define locale routing, translation loading, and localized customer-facing copy
requirements across auth and booking-related experiences.

## Requirements

### ADDED Requirements

### Requirement: Locale Routing

Hệ thống SHALL route tất cả pages qua locale prefix (`/[locale]/...`).

#### Scenario: Valid locale access

- **WHEN** user truy cập `/vi/login`
- **THEN** render login page với Vietnamese translations

- **WHEN** user truy cập `/en/login`
- **THEN** render login page với English translations

#### Scenario: Root path redirect

- **WHEN** user truy cập `/login` (không có locale prefix)
- **THEN** redirect đến `/vi/login` hoặc `/en/login` based on detection

#### Scenario: Invalid locale redirect

- **WHEN** user truy cập `/fr/login` (unsupported locale)
- **THEN** redirect đến `/vi/login` (default locale)

### Requirement: Locale Detection

Hệ thống SHALL detect user's preferred locale từ multiple sources.

#### Scenario: Cookie exists

- **WHEN** user có cookie `NEXT_LOCALE` với value `en`
- **THEN** sử dụng `en` làm locale (ignore browser preference)

#### Scenario: No cookie - browser preference match

- **WHEN** không có cookie
- **THEN** check browser's Accept-Language header
- **WHEN** browser preference là `vi`, `vi-VN`, hoặc `en`, `en-US`
- **THEN** sử dụng matched locale

#### Scenario: No cookie - browser preference no match

- **WHEN** không có cookie và browser preference không match supported locales
- **THEN** sử dụng default locale (`vi`)

### Requirement: Locale Persistence

Hệ thống SHALL persist user's locale preference.

#### Scenario: User changes locale

- **WHEN** user navigate từ `/vi/login` đến `/en/login` (manual URL change hoặc
  language switcher)
- **THEN** set cookie `NEXT_LOCALE=en`
- **THEN** subsequent visits sẽ auto-detect `en`

### Requirement: Translation Loading

Hệ thống SHALL load translations based on current locale for both the existing
auth experience and the full customer booking experience.

#### Scenario: Vietnamese translations

- **WHEN** locale là `vi`
- **THEN** load translations từ `messages/vi.json` for auth pages, homepage
  search, search results, seat selection, booking, account, profile, navigation,
  statuses, shared feedback messages, booking stepper labels, payment-state
  messages, countdown text, price-breakdown copy, passenger-form labels,
  passenger section headings, and passenger duplicate-document validation
  messages
- **THEN** payment history, payment detail, payment-status badges, and ticket
  print/share copy are shown in Vietnamese

#### Scenario: English translations

- **WHEN** locale là `en`
- **THEN** load translations từ `messages/en.json` for auth pages, homepage
  search, search results, seat selection, booking, account, profile, navigation,
  statuses, shared feedback messages, booking stepper labels, payment-state
  messages, countdown text, price-breakdown copy, passenger-form labels,
  passenger section headings, and passenger duplicate-document validation
  messages
- **THEN** payment history, payment detail, payment-status badges, and ticket
  print/share copy are shown in English

#### Scenario: Missing translation key

- **WHEN** translation key không tồn tại trong file
- **THEN** display key name as fallback (for debugging)

### Requirement: Translated Form Labels

Hệ thống SHALL display form labels, placeholders, button text, and helper copy
in the current locale across auth and customer booking forms.

#### Scenario: Vietnamese form labels

- **WHEN** locale là `vi`
- **THEN** login form hiển thị:
    - Title: "Đăng nhập"
    - Email label: "Email"
    - Password label: "Mật khẩu"
    - Submit button: "Đăng nhập"
- **THEN** customer booking forms hiển thị translated labels and actions for
  station selection, departure date, search submission, seat selection, booking
  confirmation, passenger full name, passenger identity document number,
  passenger date of birth, passenger gender, payment progression,
  retry/start-over payment actions, profile editing, and cancellation
  confirmation

#### Scenario: English form labels

- **WHEN** locale là `en`
- **THEN** login form hiển thị:
    - Title: "Sign In"
    - Email label: "Email"
    - Password label: "Password"
    - Submit button: "Sign In"
- **THEN** customer booking forms hiển thị translated labels and actions for
  station selection, departure date, search submission, seat selection, booking
  confirmation, passenger full name, passenger identity document number,
  passenger date of birth, passenger gender, payment progression,
  retry/start-over payment actions, profile editing, and cancellation
  confirmation

### Requirement: Translated Error Messages

Hệ thống SHALL display validation, API, and booking-flow feedback messages in
the current locale.

#### Scenario: Vietnamese error messages

- **WHEN** locale là `vi` và validation fails
- **THEN** hiển thị Vietnamese error messages (e.g., "Email không hợp lệ")
- **THEN** search, booking, passenger duplicate-document, passenger required
  field, payment countdown/status, cancellation, profile, and network feedback
  messages are shown in Vietnamese

#### Scenario: English error messages

- **WHEN** locale là `en` và validation fails
- **THEN** hiển thị English error messages (e.g., "Invalid email address")
- **THEN** search, booking, passenger duplicate-document, passenger required
  field, payment countdown/status, cancellation, profile, and network feedback
  messages are shown in English

### Requirement: Locale-Aware Links

Hệ thống SHALL preserve locale trong navigation links across auth flows and the
customer booking experience.

#### Scenario: Internal navigation

- **WHEN** user ở `/vi/login` và click link đến register
- **THEN** navigate đến `/vi/register` (preserve locale)

#### Scenario: Redirect after action

- **WHEN** user register thành công ở `/en/register`
- **THEN** redirect đến `/en/login?registered=true` (preserve locale)

#### Scenario: Customer booking navigation preserves locale

- **WHEN** user navigate between homepage, search, seats, booking, account, and
  profile pages inside a locale
- **THEN** all internal links and redirects preserve the active locale prefix

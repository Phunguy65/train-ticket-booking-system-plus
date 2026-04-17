# Why

Customer frontend hiện tại chưa có giao diện đăng nhập/đăng ký và chưa hỗ trợ đa
ngôn ngữ. Cần triển khai auth UI với i18n để user có thể đăng ký tài khoản, đăng
nhập, và sử dụng ứng dụng bằng tiếng Việt (mặc định) hoặc tiếng Anh.

## What Changes

- Thêm trang đăng nhập (`/[locale]/login`) với form email + password
- Thêm trang đăng ký (`/[locale]/register`) với form fullName + email +
  password + confirmPassword
- Cài đặt và cấu hình shadcn/ui components (button, input, label, card, form,
  alert, sonner)
- Cài đặt và cấu hình next-intl cho i18n
- Thêm locale routing với middleware (`/vi/...`, `/en/...`)
- Tạo translation files cho Vietnamese và English
- Tích hợp với existing API mutations (`loginMutation`, `registerMutation`)

## Capabilities

### New Capabilities

- `auth-ui`: Giao diện đăng nhập và đăng ký cho customer, bao gồm form
  validation, error handling, loading states
- `i18n`: Hệ thống đa ngôn ngữ với next-intl, hỗ trợ Vietnamese (default) và
  English, locale detection từ browser + cookie persistence

### Modified Capabilities

<!-- Không có capability nào bị thay đổi requirement -->

## Impact

**Frontend (customer)**:

- Cấu trúc thư mục: thêm `app/[locale]/`, `components/ui/`, `components/auth/`,
  `i18n/`, `messages/`
- Dependencies mới: `next-intl`, `react-hook-form`, `@hookform/resolvers`,
  shadcn/ui components
- Middleware: thêm `middleware.ts` cho locale routing
- Existing files: cập nhật `layout.tsx`, `globals.css` để hỗ trợ shadcn/ui và
  i18n

**API Integration**:

- Sử dụng existing generated mutations: `loginMutation`, `registerMutation`
- Không thay đổi backend API

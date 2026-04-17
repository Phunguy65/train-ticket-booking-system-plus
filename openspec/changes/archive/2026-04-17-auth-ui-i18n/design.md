# Context

Customer frontend (`frontend/customer/`) là Next.js 16 App Router project với:

- React 19, TailwindCSS 4, TypeScript
- TanStack React Query cho API state management
- Zod validation (v4.1.11)
- Generated API client từ OpenAPI với mutations: `loginMutation`,
  `registerMutation`
- Bun package manager, Biome linter

Hiện tại chưa có:

- UI component library
- Auth pages (login/register)
- i18n support

## Goals / Non-Goals

**Goals:**

- Triển khai Login và Register pages với shadcn/ui
- Setup next-intl cho i18n với Vietnamese (default) và English
- Form validation với react-hook-form + Zod
- Tích hợp với existing API mutations
- Mobile-first responsive design
- Basic accessibility (contrast, focus states, labels)

**Non-Goals:**

- Social login (Google, Facebook, etc.) — future enhancement
- Password reset/forgot password flow — separate feature
- Email verification — backend handles separately
- Full WCAG 2.1 AA compliance — MVP focuses on basics
- Admin dashboard auth — different frontend

## Decisions

### 1. UI Component Library: shadcn/ui

**Decision**: Sử dụng shadcn/ui thay vì MUI, Chakra, hoặc custom components.

**Rationale**:

- Copy-paste components, không phải npm dependency — full control
- Tailwind-native, consistent với existing setup
- Excellent TypeScript support
- Small bundle size (chỉ import những gì cần)

**Alternatives considered**:

- MUI: Quá nặng cho MVP, Material Design không match Vietnamese travel aesthetic
- Chakra: Tốt nhưng less control, larger runtime
- Custom: Tốn thời gian, reinvent the wheel

### 2. i18n Library: next-intl

**Decision**: Sử dụng next-intl thay vì react-i18next hoặc next-international.

**Rationale**:

- Native App Router support (designed for RSC)
- Smallest bundle (~4KB vs 22KB của i18next)
- Type-safe translations out of box
- Built-in middleware cho locale routing

**Alternatives considered**:

- react-i18next: 5.5x larger bundle, overkill cho MVP
- next-international: Less mature community

### 3. URL Strategy: Locale Prefix Required

**Decision**: URL pattern `/[locale]/path` (e.g., `/vi/login`, `/en/login`).

**Rationale**:

- SEO friendly — mỗi locale có unique URL
- Shareable links — user có thể share link với đúng ngôn ngữ
- Simple implementation với next-intl middleware

**Alternatives considered**:

- Cookie-only (no URL prefix): Không SEO friendly, khó share
- Subdomain (`vi.ttbs.com`): Phức tạp deployment, overkill

### 4. Locale Detection: Browser + Cookie Persistence

**Decision**: Detect từ browser Accept-Language header, lưu preference vào
cookie.

**Rationale**:

- First visit: Auto-detect từ browser → good UX
- Return visit: Respect user's previous choice → consistent UX
- Manual change: Update cookie → persist preference

### 5. Form Library: react-hook-form + Zod

**Decision**: Sử dụng react-hook-form với @hookform/resolvers/zod.

**Rationale**:

- shadcn/ui Form component built on react-hook-form
- Zod already in project (v4.1.11)
- Excellent performance (uncontrolled inputs)
- Type-safe form values

### 6. Error Handling Strategy

**Decision**:

- Validation/API errors → Alert component trong form
- Network errors → Toast notification (sonner)

**Rationale**:

- Validation errors cần visible khi user đang sửa input
- Network errors không liên quan input, toast không block UI

### 7. Register Success Flow

**Decision**: Redirect về Login page với success message (query param).

**Rationale**:

- Explicit confirmation user đã register thành công
- User có thể login với credentials vừa tạo
- Simpler than auto-login (no token handling on register)

**Alternatives considered**:

- Auto-login after register: Requires calling login mutation, more complex

## Risks / Trade-offs

### Risk: shadcn/ui chưa được init trong project

**Mitigation**: Task đầu tiên là init shadcn/ui với proper config cho Tailwind
4 + Bun.

### Risk: next-intl middleware conflict với existing setup

**Mitigation**: Test middleware carefully, ensure không conflict với API routes
hoặc static files.

### Risk: Translation keys out of sync với code

**Mitigation**:

- Use TypeScript để type-check translation keys
- Unit tests cho form components verify translation keys exist

### Trade-off: Locale prefix required trong URL

**Accepted**: Tất cả URLs cần locale prefix (`/vi/...`). User vào `/login` sẽ
được redirect.

### Trade-off: No password strength indicator

**Accepted**: MVP chỉ validate min 8 chars. Strength indicator là future
enhancement.

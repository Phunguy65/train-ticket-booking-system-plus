# Tasks

## 1. Setup Dependencies

- [x] 1.1 Initialize shadcn/ui với `bunx shadcn@latest init` (style: default,
      base color: slate, CSS variables: yes)
- [x] 1.2 Install shadcn/ui components:
      `bunx shadcn@latest add button input label card form alert sonner separator`
- [x] 1.3 Install form dependencies:
      `bun add react-hook-form @hookform/resolvers`
- [x] 1.4 Install i18n: `bun add next-intl`
- [x] 1.5 Verify `src/lib/utils.ts` exists với `cn()` function (created by
      shadcn init)

## 2. i18n Configuration

- [x] 2.1 Create `src/i18n/routing.ts` với locales config (`vi`, `en`, default:
      `vi`)
- [x] 2.2 Create `src/i18n/request.ts` với getRequestConfig để load messages
- [x] 2.3 Create `src/middleware.ts` với createNavigationMiddleware từ next-intl
- [x] 2.4 Create `src/messages/vi.json` với translations cho Auth, Validation,
      Errors, Success
- [x] 2.5 Create `src/messages/en.json` với English translations
- [x] 2.6 Update `next.config.ts` để integrate next-intl plugin (nếu cần)

## 3. App Structure Migration

- [x] 3.1 Create `src/app/[locale]/layout.tsx` với NextIntlClientProvider
- [x] 3.2 Move/create `src/app/[locale]/page.tsx` (home page placeholder)
- [x] 3.3 Create route group `src/app/[locale]/(auth)/` cho auth pages
- [x] 3.4 Update root `src/app/layout.tsx` để redirect hoặc handle locale
      routing

## 4. Shared Auth Components

- [x] 4.1 Create `src/components/auth/password-input.tsx` với show/hide toggle
- [x] 4.2 Create `src/components/auth/auth-card.tsx` wrapper component
- [x] 4.3 Create `src/lib/validations/auth.ts` với Zod schemas (loginSchema,
      registerSchema)

## 5. Login Page

- [x] 5.1 Create `src/components/auth/login-form.tsx` với react-hook-form +
      useTranslations
- [x] 5.2 Create `src/app/[locale]/(auth)/login/page.tsx`
- [x] 5.3 Integrate loginMutation từ generated API
- [x] 5.4 Handle success redirect, error display (Alert), loading state

## 6. Register Page

- [x] 6.1 Create `src/components/auth/register-form.tsx` với react-hook-form +
      useTranslations
- [x] 6.2 Create `src/app/[locale]/(auth)/register/page.tsx`
- [x] 6.3 Integrate registerMutation từ generated API
- [x] 6.4 Handle success redirect với query param, error display, loading state
- [x] 6.5 Handle `?registered=true` query param trên login page để show success
      message

## 7. Toast Setup

- [x] 7.1 Add Toaster component từ sonner vào root layout
- [x] 7.2 Create toast utility cho network errors

## 8. Styling & Polish

- [x] 8.1 Update `src/app/globals.css` với shadcn/ui CSS variables (light/dark
      mode)
- [x] 8.2 Verify responsive behavior (mobile-first)
- [x] 8.3 Test dark mode appearance

## 9. Testing

- [x] 9.1 Create `src/lib/validations/auth.test.ts` cho Zod schemas
- [x] 9.2 Create `src/components/auth/login-form.test.tsx` (basic render +
      validation)
- [x] 9.3 Create `src/components/auth/register-form.test.tsx` (basic render +
      validation)
- [x] 9.4 Run `bun run test` để verify tất cả tests pass

## 10. Verification

- [x] 10.1 Run `bun run lint` và fix any issues
- [x] 10.2 Run `bun run build` và verify no build errors
- [ ] 10.3 Manual test: login flow với valid/invalid credentials
- [ ] 10.4 Manual test: register flow với valid/invalid data
- [ ] 10.5 Manual test: locale switching (`/vi/login` ↔ `/en/login`)
- [ ] 10.6 Manual test: locale detection (clear cookies, visit site)

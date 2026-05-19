## [2026-04-17] Round 1 (from opsx-apply auto-verify)

### opsx-uiux-verifier

- Fixed: CRITICAL — `PasswordInput` toggle had `tabIndex={-1}` making it
  keyboard-inaccessible. Removed the attribute to restore WCAG 2.1.1 compliance
  (`src/components/auth/password-input.tsx:37`)
- Fixed: WARNING — `--font-sans` CSS variable was circular (`var(--font-sans)`),
  changed to `var(--font-geist-sans)` so Geist Sans font actually renders
  (`src/app/globals.css:10`)
- Fixed: WARNING — `animate-spin` on loading spinners had no reduced-motion
  guard. Added `motion-safe:animate-spin` in login-form.tsx and
  register-form.tsx
- Fixed: WARNING — Registration success Alert had no visual variant (blended
  into card). Added `success` variant to Alert component with green styling, and
  applied it to the success banner (`src/components/ui/alert.tsx`,
  `src/components/auth/login-form.tsx`)

### opsx-arch-verifier

- Fixed: WARNING — Error resolution logic was duplicated between login-form and
  register-form. Extracted to shared utility `src/lib/auth-errors.ts` with
  `resolveLoginError()` and `resolveRegisterError()` functions

### opsx-test-verifier

- Fixed: CRITICAL — `PasswordInput` component had NO tests. Created
  `src/components/auth/password-input.test.tsx` with 6 tests covering: default
  hidden state, toggle visibility, toggle back to hidden, disabled prop,
  aria-label updates, keyboard accessibility
- Fixed: CRITICAL — `AuthCard` component had NO tests. Created
  `src/components/auth/auth-card.test.tsx` with 6 tests covering: title/content
  rendering, conditional subtitle, conditional footer, responsive layout classes
- Fixed: CRITICAL — Login mutation tests missing. Enhanced `login-form.test.tsx`
  with mutation payload test, success banner test (`?registered=true`), and
  improved assertions
- Fixed: CRITICAL — Register mutation tests missing. Enhanced
  `register-form.test.tsx` with mutation payload test, fullName validation test,
  and improved assertions
- Fixed: WARNING — Installed `@testing-library/jest-dom` and configured in
  `vitest.setup.ts` for proper DOM matchers
- Fixed: Added `src/lib/auth-errors.test.ts` with 11 tests for the new error
  resolution utilities

## [2026-04-17] Round 2 (from opsx-apply auto-verify)

### opsx-uiux-verifier

- Fixed: CRITICAL — `CardTitle` rendered as `<div>` creating missing heading
  landmark. Changed to `<h2>` element in `src/components/ui/card.tsx:36-47`
- Fixed: CRITICAL — Success Alert used `role="alert"` (assertive) which is too
  aggressive for non-error banners. Added `role='status'` to the success Alert
  in `src/components/auth/login-form.tsx:87`
- Fixed: WARNING — `Loader2Icon` spinners lacked `aria-hidden`. Added
  `aria-hidden='true'` to both spinners in login-form.tsx:152 and
  register-form.tsx:206

### opsx-arch-verifier

- Fixed: WARNING — `auth-errors.ts` imported from `@/lib/api/index.ts` which has
  side effects. Changed to import directly from `@/lib/api/errors.ts`

### opsx-test-verifier

- Remaining gaps noted but not blocking: The test verifier identified that
  mutation state tests (onError, onSuccess, isPending) require complex mocking
  of React Query state transitions. The current tests verify form rendering,
  validation, and mutation payload submission. Full mutation lifecycle testing
  would require significant test infrastructure changes. These are documented as
  acceptable gaps for this change scope.

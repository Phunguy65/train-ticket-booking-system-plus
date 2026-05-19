## Why

The customer-facing frontend currently uses plain shadcn/ui components with a neutral grayscale palette, no illustrations, and no animations. Auth pages are bare centered cards, the home page is a minimal icon + search form, and the account page is a simple tab list. This makes the app feel like a generic Next.js starter rather than a professional travel booking platform. A visual redesign with travel-themed illustrations, a vibrant color palette, and CSS animations will establish brand identity and improve user engagement.

## What Changes

- Replace the centered-card auth layout with a split-screen layout (illustration panel left, form right; stacks on mobile)
- Introduce a mint/teal travel-themed color palette replacing the current neutral grayscale
- Add inline SVG illustration components (train, clouds, tracks, station landscape) for decorative use across pages
- Add CSS keyframe animations (float, fade-in, slide-up) with `prefers-reduced-motion` support
- Redesign the home page hero section with floating SVG elements, richer gradient background, and staggered entrance animations
- Enhance the account page with a stats summary row above the existing tabs
- All changes are visual-only; no logic, routing, validation, or API changes

## Capabilities

### New Capabilities
- `ui-travel-theme`: Global color palette (oklch mint/sky/orange), CSS keyframe animations, utility classes, and design tokens for the travel brand identity
- `ui-illustrations`: Inline SVG illustration components (train, clouds, station, tracks) used as decorative elements across auth and home pages
- `ui-auth-layout`: Split-screen authentication layout replacing the current AuthCard — illustration panel on desktop, single-column on mobile

### Modified Capabilities
- `auth-ui`: Visual presentation changes — auth pages use new split-screen layout instead of centered card. No behavior/requirement changes.
- `customer-account-ui`: Add stats summary cards above existing tabs. No behavior changes to bookings/payments lists.

## Impact

- **Frontend only** — no backend, API, or database changes
- **Files affected**:
  - `src/app/globals.css` — new color variables, keyframes, utility classes
  - `src/components/auth/auth-card.tsx` → refactored to split-screen layout
  - `src/app/[locale]/(auth)/login/page.tsx` and `register/page.tsx` — use new layout
  - `src/app/[locale]/(main)/page.tsx` — hero section redesign
  - `src/components/account/account-tabs.tsx` — stats row addition
  - New directory: `src/components/illustrations/` — SVG components
- **Dependencies**: None added. Uses existing Tailwind CSS v4 + tw-animate-css
- **Tests**: Existing behavioral tests unaffected (test form logic, not visuals)
- **Accessibility**: Must maintain WCAG AA contrast (mint-600 #059669 on white = 4.6:1), keyboard nav, focus states unchanged

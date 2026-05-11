## 1. Travel Theme Foundation (globals.css)

- [x] 1.1 Update color palette in globals.css — replace neutral primary/accent/ring oklch values with mint-based values for both light and dark modes
- [x] 1.2 Add CSS keyframe definitions: `float` (translateY 0→-12px→0, 3s infinite), `fade-in` (opacity 0→1 + translateY 8→0, 0.5s once), `slide-up` (opacity 0→1 + translateY 20→0, 0.6s once)
- [x] 1.3 Add animation utility classes (`animate-float`, `animate-fade-in`, `animate-slide-up`) with `prefers-reduced-motion` guard
- [x] 1.4 Add animation delay utilities (`delay-100`, `delay-200`, `delay-300`, `delay-400`) ← (verify: all animations disabled when prefers-reduced-motion: reduce, contrast ratio of new primary on background >= 4.5:1)

## 2. SVG Illustration Components

- [x] 2.1 Create `src/components/illustrations/train-journey.tsx` — inline SVG of stylized train on tracks with landscape, accepts className, aria-hidden="true"
- [x] 2.2 Create `src/components/illustrations/clouds-decoration.tsx` — inline SVG with 2-3 cloud shapes, accepts className, aria-hidden="true"
- [x] 2.3 Create `src/components/illustrations/station-platform.tsx` — inline SVG of station scene, accepts className, aria-hidden="true"
- [x] 2.4 Create `src/components/illustrations/track-pattern.tsx` — inline SVG of repeating rail track motif, accepts className, aria-hidden="true"
- [x] 2.5 Create `src/components/illustrations/index.ts` barrel export for all illustration components ← (verify: all components render valid SVG, use currentColor or CSS custom properties, barrel exports all 4 components)

## 3. Auth Layout Component

- [x] 3.1 Create `src/components/auth/auth-layout.tsx` — split-screen layout with grid-cols-1 md:grid-cols-2, min-h-screen, illustration panel (hidden md:flex with gradient bg), form panel (centered content with max-w-sm)
- [x] 3.2 AuthLayout accepts props: title, subtitle, children, footer, illustration (ReactNode)
- [x] 3.3 Update `src/app/[locale]/(auth)/login/page.tsx` — replace AuthCard with AuthLayout, pass TrainJourney as illustration
- [x] 3.4 Update `src/app/[locale]/(auth)/register/page.tsx` — replace AuthCard with AuthLayout, pass StationPlatform as illustration ← (verify: both auth pages render split-screen on desktop, single-column on mobile, form logic unchanged, existing tests pass)

## 4. Home Page Hero Redesign

- [x] 4.1 Update `src/app/[locale]/(main)/page.tsx` — redesign hero section with richer gradient background (mint-50 to sky-50), enhanced card shadow/border
- [x] 4.2 Add CloudsDecoration SVG as floating decorative elements with animate-float class
- [x] 4.3 Add staggered animate-fade-in to heading (delay-100), subtitle (delay-200), and search card (delay-300) ← (verify: hero renders with floating clouds, staggered entrance, animations disabled with prefers-reduced-motion, TripSearchForm unchanged)

## 5. Account Page Enhancement

- [x] 5.1 Create stats summary row component in account page — 3 cards (total bookings, upcoming trips, total spent) derived from existing bookings data
- [x] 5.2 Style stat cards with accent colors and animate-fade-in with staggered delays
- [x] 5.3 Add skeleton loading state for stat cards while data loads
- [x] 5.4 Handle empty state (display "0" values) ← (verify: stats row appears above tabs, data derived from existing API response, skeleton shown during loading, zero state handled)

## 6. Cleanup and Verification

- [x] 6.1 Remove old `AuthCard` component if no longer imported anywhere (or keep if used elsewhere)
- [x] 6.2 Run `bun run lint` (biome check) and fix any issues
- [x] 6.3 Run `bun run test` (vitest) and ensure all existing tests pass
- [x] 6.4 Run `bun run build` (next build) and ensure no build errors ← (verify: zero lint errors, all tests pass, build succeeds, no unused imports or dead code)

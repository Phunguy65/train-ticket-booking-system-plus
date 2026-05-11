## Context

The customer frontend (`frontend/customer`) is a Next.js 16 app using Tailwind CSS v4, shadcn/ui (radix-nova), and tw-animate-css. Current auth pages use a simple `AuthCard` component that centers a card on a blank background. The home page has a minimal hero with a Lucide icon and search form. The account page is a plain tabs component. There are no custom illustrations, no brand colors (pure grayscale oklch), and no CSS animations beyond the loading spinner.

The app supports `vi` and `en` locales via next-intl. Vietnamese text is ~20-30% longer than English, requiring generous layout spacing.

## Goals / Non-Goals

**Goals:**
- Establish a travel-themed visual identity with mint/teal primary color palette
- Create a split-screen auth layout that showcases illustrations on desktop and gracefully degrades on mobile
- Add inline SVG illustration components reusable across pages
- Implement CSS-only animations (keyframes) with accessibility support (prefers-reduced-motion)
- Enhance home hero section with floating decorative elements and richer gradients
- Add stats summary to account page for quick overview

**Non-Goals:**
- No changes to form logic, validation, API calls, or routing
- No new npm dependencies (no framer-motion, lottie, GSAP)
- No redesign of search results, booking flow, seat selection, or payment pages
- No dark mode color adjustments (keep existing dark theme structure, update values)
- No backend changes

## Decisions

### 1. Color Palette — oklch mint/sky/orange

**Decision**: Replace neutral grayscale primary with mint-based travel palette.

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| --primary | oklch(0.55 0.17 162) | oklch(0.75 0.15 162) | Buttons, links, active states |
| --primary-foreground | oklch(0.99 0 0) | oklch(0.15 0 0) | Text on primary |
| --accent | oklch(0.92 0.04 162) | oklch(0.25 0.04 162) | Hover backgrounds, subtle highlights |
| --ring | oklch(0.55 0.17 162) | oklch(0.65 0.12 162) | Focus rings |

Secondary (sky) and accent (orange) used sparingly for CTAs and badges. Destructive/muted/border tokens remain structurally the same.

**Rationale**: oklch provides perceptual uniformity. Mint/teal aligns with Vietnamese railway branding and travel industry conventions (Trainline, Omio). Contrast ratio of mint on white exceeds 4.5:1 for WCAG AA.

**Alternative considered**: Blue primary (Skyscanner style) — rejected because it conflicts with existing link styling and feels less distinctive for a Vietnamese brand.

### 2. Auth Layout — Split-screen with illustration panel

**Decision**: Replace `AuthCard` with a new `AuthLayout` component using CSS Grid.

```
┌─────────────────────────────────────────────────────┐
│  Desktop (md+)                                      │
│  ┌──────────────────┬──────────────────────────┐    │
│  │  Illustration    │     Form Panel           │    │
│  │  Panel           │                          │    │
│  │  (gradient bg    │     Logo                 │    │
│  │   + SVG)         │     Title/Subtitle       │    │
│  │                  │     [Form Fields]        │    │
│  │  hidden on       │     [Submit Button]      │    │
│  │  mobile          │     Footer link          │    │
│  └──────────────────┴──────────────────────────┘    │
│                                                     │
│  Mobile (<md)                                       │
│  ┌──────────────────────────────────────────────┐   │
│  │  Form Panel (full width, gradient bg subtle) │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

- Grid: `grid-cols-1 md:grid-cols-2`
- Illustration panel: `hidden md:flex`, gradient background, centered SVG
- Form panel: vertically centered, max-w-sm inner container
- Login and Register share the same layout, different illustration

**Rationale**: Split-screen is the industry standard for travel auth pages. Provides visual engagement without cluttering the form. Mobile users get a clean single-column experience.

**Alternative considered**: Overlay form on background image — rejected due to contrast/readability concerns and complexity of responsive handling.

### 3. SVG Illustrations — Custom inline components

**Decision**: Create 4-5 React components in `src/components/illustrations/` that render inline SVGs.

Components:
- `TrainJourney` — stylized train on tracks with landscape (auth pages)
- `CloudsDecoration` — floating cloud shapes (hero background)
- `StationPlatform` — station scene (register page variant)
- `TrackPattern` — decorative rail track pattern (borders/dividers)

Each SVG uses `currentColor` or CSS custom properties for theming. Dimensions controlled via Tailwind width/height classes.

**Rationale**: Inline SVGs are tree-shakeable, themeable, and don't require network requests. Custom illustrations ensure brand uniqueness without licensing concerns.

**Alternative considered**: External SVG files in `/public` — rejected because they can't use CSS custom properties for theming and add network requests.

### 4. CSS Animations — Keyframes in globals.css

**Decision**: Define animations in `globals.css` and expose as Tailwind utility classes.

| Animation | Keyframe | Duration | Usage |
|-----------|----------|----------|-------|
| `animate-float` | translateY 0→-12px→0 | 3s infinite | Floating decorative SVGs |
| `animate-fade-in` | opacity 0→1, translateY 8px→0 | 0.5s once | Page load elements |
| `animate-slide-up` | opacity 0→1, translateY 20px→0 | 0.6s once | Staggered card entrance |
| `animate-pulse-slow` | opacity 1→0.7→1 | 4s infinite | Subtle background glow |

All animations wrapped in `@media (prefers-reduced-motion: no-preference)`. Existing `motion-safe:` Tailwind prefix also respected.

**Rationale**: CSS keyframes run off-thread (compositor), zero JS overhead. tw-animate-css already installed provides the pattern. No new dependency needed.

### 5. Home Hero — Layered composition

**Decision**: Redesign hero as a layered section with:
- Background: gradient from mint-50 to sky-50 (light) / dark equivalents
- Middle layer: floating SVG decorations (clouds, abstract shapes) with `animate-float`
- Foreground: search form card with enhanced shadow and border-accent

The existing `TripSearchForm` component stays unchanged — only its wrapper styling changes.

### 6. Account Stats Row

**Decision**: Add a row of 3 stat cards above the existing `AccountTabs`:
- Total bookings count
- Upcoming trips count  
- Total spent amount

Data sourced from existing API responses (bookings list already fetched). Cards use the new accent colors and `animate-fade-in` on mount.

## Risks / Trade-offs

- **[oklch browser support]** → oklch is supported in all modern browsers (Chrome 111+, Firefox 113+, Safari 15.4+). The app already uses oklch in globals.css, so no new risk.
- **[Animation jank on low-end devices]** → Mitigated by using only `transform` and `opacity` (compositor-friendly). `will-change` applied sparingly. `prefers-reduced-motion` disables all animations.
- **[SVG bundle size]** → Inline SVGs add to JS bundle. Mitigated by keeping illustrations simple (< 5KB each) and using Next.js code splitting (illustrations only loaded on pages that use them).
- **[Vietnamese text overflow in split layout]** → Form panel uses `max-w-sm` with padding. Tested with longest Vietnamese labels. Illustration panel is decorative-only, no text overflow risk.
- **[Existing test breakage]** → Tests assert on form behavior (labels, buttons, submission), not layout structure. AuthCard → AuthLayout rename requires updating imports in page files but not test files (tests render the form component directly).

## MODIFIED Requirements

### Requirement: Account page layout

The account page SHALL display a stats summary row above the existing bookings/payments tabs, providing users with a quick overview of their activity.

#### Scenario: Stats row displayed above tabs

- **WHEN** authenticated user navigates to `/account`
- **THEN** a row of 3 stat cards is displayed above the tabs
- **THEN** the stats show: total bookings count, upcoming trips count, total amount spent

#### Scenario: Stats cards use travel theme styling

- **WHEN** the stats row renders
- **THEN** each card uses the new accent color palette (mint/sky tones)
- **THEN** cards have `animate-fade-in` entrance animation with staggered delays

#### Scenario: Stats derived from existing data

- **WHEN** the bookings list data is loaded
- **THEN** stats are computed from the same data (no additional API call)
- **THEN** while data is loading, stat cards show skeleton placeholders

#### Scenario: Empty state

- **WHEN** user has zero bookings
- **THEN** all stat values display "0" (not hidden, not error state)

## ADDED Requirements

### Requirement: Home page hero redesign

The home page hero section SHALL feature a visually rich composition with floating SVG decorations, enhanced gradient background, and staggered entrance animations.

#### Scenario: Hero renders with decorative elements

- **WHEN** user navigates to the home page
- **THEN** the hero section displays floating cloud SVGs with `animate-float` animation
- **THEN** the background uses a gradient from mint-50 to sky-50 (light mode)
- **THEN** the search form card has enhanced shadow and a subtle border accent

#### Scenario: Hero entrance animation

- **WHEN** the home page loads
- **THEN** the heading, subtitle, and search form card appear with staggered `animate-fade-in` (100ms, 200ms, 300ms delays)

#### Scenario: Reduced motion on hero

- **WHEN** user has `prefers-reduced-motion: reduce`
- **THEN** floating animations are disabled
- **THEN** entrance animations are disabled (elements appear immediately)

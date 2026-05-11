## ADDED Requirements

### Requirement: Travel color palette

The system SHALL define a mint/teal-based color palette using oklch values in `globals.css` that replaces the current neutral grayscale for primary, accent, and ring tokens.

#### Scenario: Light mode colors applied

- **WHEN** the app renders in light mode (no `.dark` class on html)
- **THEN** `--primary` resolves to oklch(0.55 0.17 162) (mint-600 equivalent)
- **THEN** `--accent` resolves to oklch(0.92 0.04 162) (mint-50 equivalent)
- **THEN** `--ring` resolves to oklch(0.55 0.17 162)

#### Scenario: Dark mode colors applied

- **WHEN** the app renders in dark mode (`.dark` class on html)
- **THEN** `--primary` resolves to oklch(0.75 0.15 162) (lighter mint for dark backgrounds)
- **THEN** `--accent` resolves to oklch(0.25 0.04 162)

#### Scenario: WCAG AA contrast maintained

- **WHEN** primary-colored text is rendered on the default background
- **THEN** the contrast ratio SHALL be at least 4.5:1 in both light and dark modes

### Requirement: CSS keyframe animations

The system SHALL define reusable CSS keyframe animations in `globals.css` exposed as utility classes.

#### Scenario: Float animation available

- **WHEN** an element has class `animate-float`
- **THEN** the element animates vertically (translateY 0 → -12px → 0) over 3 seconds, repeating infinitely

#### Scenario: Fade-in animation available

- **WHEN** an element has class `animate-fade-in`
- **THEN** the element transitions from opacity 0 + translateY 8px to opacity 1 + translateY 0 over 0.5 seconds, playing once

#### Scenario: Slide-up animation available

- **WHEN** an element has class `animate-slide-up`
- **THEN** the element transitions from opacity 0 + translateY 20px to opacity 1 + translateY 0 over 0.6 seconds, playing once

#### Scenario: Reduced motion respected

- **WHEN** the user has `prefers-reduced-motion: reduce` enabled
- **THEN** all custom animations (float, fade-in, slide-up) SHALL be disabled (animation: none or duration: 0)

### Requirement: Animation delay utilities

The system SHALL provide stagger delay utility classes for sequencing entrance animations.

#### Scenario: Staggered delays applied

- **WHEN** multiple elements use `animate-fade-in` with classes `delay-100`, `delay-200`, `delay-300`
- **THEN** each element starts its animation 100ms, 200ms, 300ms after page load respectively

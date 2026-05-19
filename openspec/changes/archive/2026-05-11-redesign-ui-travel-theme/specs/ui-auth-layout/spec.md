## ADDED Requirements

### Requirement: Split-screen auth layout

The system SHALL provide an `AuthLayout` component that renders a split-screen layout for authentication pages with an illustration panel and a form panel.

#### Scenario: Desktop layout (md+ breakpoint)

- **WHEN** the viewport width is >= 768px (md breakpoint)
- **THEN** the layout renders as a 2-column grid (grid-cols-2)
- **THEN** the left column displays the illustration panel with a gradient background and centered SVG illustration
- **THEN** the right column displays the form content vertically centered

#### Scenario: Mobile layout (< md breakpoint)

- **WHEN** the viewport width is < 768px
- **THEN** the illustration panel is hidden (display: none)
- **THEN** the form panel takes full width with a subtle gradient background
- **THEN** the form content is vertically centered

#### Scenario: Full viewport height

- **WHEN** `AuthLayout` is rendered
- **THEN** the layout occupies at minimum the full viewport height (min-h-screen)

### Requirement: AuthLayout accepts illustration prop

The system SHALL allow the `AuthLayout` component to receive an `illustration` prop to customize which SVG is shown in the illustration panel.

#### Scenario: Custom illustration rendered

- **WHEN** `<AuthLayout illustration={<TrainJourney />}>` is rendered on desktop
- **THEN** the illustration panel displays the `TrainJourney` SVG component

#### Scenario: Different illustrations per page

- **WHEN** the login page renders `AuthLayout` with `TrainJourney` illustration
- **WHEN** the register page renders `AuthLayout` with `StationPlatform` illustration
- **THEN** each page shows its respective illustration in the left panel

### Requirement: AuthLayout form panel structure

The system SHALL render the form panel with consistent structure: logo area, title, subtitle, form content, and footer.

#### Scenario: All slots rendered

- **WHEN** `AuthLayout` receives `title`, `subtitle`, `children`, and `footer` props
- **THEN** the form panel displays: title as h1, subtitle as paragraph, children (form), and footer below the form

#### Scenario: AuthLayout replaces AuthCard

- **WHEN** login or register page renders
- **THEN** it uses `AuthLayout` instead of the previous `AuthCard` component
- **THEN** the visual output is a split-screen layout (not a centered card)

## ADDED Requirements

### Requirement: Train journey illustration component

The system SHALL provide a `TrainJourney` React component that renders an inline SVG depicting a stylized train on tracks with landscape elements.

#### Scenario: Renders as inline SVG

- **WHEN** `<TrainJourney />` is rendered
- **THEN** it outputs an `<svg>` element with a train, tracks, and landscape shapes
- **THEN** the SVG uses `currentColor` or CSS custom properties for fill colors
- **THEN** the component accepts `className` prop for sizing via Tailwind

#### Scenario: Accessible by default

- **WHEN** `<TrainJourney />` is rendered without explicit aria attributes
- **THEN** the SVG has `aria-hidden="true"` (decorative illustration)

### Requirement: Clouds decoration component

The system SHALL provide a `CloudsDecoration` React component that renders floating cloud shapes as inline SVG.

#### Scenario: Renders cloud shapes

- **WHEN** `<CloudsDecoration />` is rendered
- **THEN** it outputs an `<svg>` element containing 2-3 cloud shapes at different positions
- **THEN** the component accepts `className` prop for positioning and sizing

#### Scenario: Decorative and hidden from assistive tech

- **WHEN** `<CloudsDecoration />` is rendered
- **THEN** the SVG has `aria-hidden="true"`

### Requirement: Station platform illustration component

The system SHALL provide a `StationPlatform` React component that renders a station scene as inline SVG.

#### Scenario: Renders station scene

- **WHEN** `<StationPlatform />` is rendered
- **THEN** it outputs an `<svg>` element depicting a station platform with architectural elements
- **THEN** the component accepts `className` prop for sizing

### Requirement: Track pattern component

The system SHALL provide a `TrackPattern` React component that renders a decorative rail track pattern.

#### Scenario: Renders track pattern

- **WHEN** `<TrackPattern />` is rendered
- **THEN** it outputs an `<svg>` element with a repeating rail track motif
- **THEN** the component accepts `className` prop for sizing and positioning

### Requirement: Illustration barrel export

The system SHALL export all illustration components from `src/components/illustrations/index.ts`.

#### Scenario: All illustrations importable from barrel

- **WHEN** code imports from `@/components/illustrations/index.ts`
- **THEN** `TrainJourney`, `CloudsDecoration`, `StationPlatform`, and `TrackPattern` are available as named exports

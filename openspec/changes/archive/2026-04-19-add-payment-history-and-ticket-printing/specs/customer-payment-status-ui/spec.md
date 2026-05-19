## ADDED Requirements

### Requirement: Accessible payment status badges

The system SHALL provide a reusable payment status badge for payment history and
payment detail views that combines localized text with a non-color-only visual
indicator.

#### Scenario: Render payment status badge in history and detail views

- **WHEN** a payment card or payment detail view displays a known payment status
- **THEN** the system renders a badge with an icon and translated status label
  for that status

#### Scenario: Render success styling for paid status

- **WHEN** a payment status badge renders a successful paid state
- **THEN** the system uses the success badge variant instead of relying on
  generic default styling

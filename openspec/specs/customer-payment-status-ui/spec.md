## Purpose

Define reusable customer payment-status presentation behavior across booking,
payment history, and payment detail experiences.

## Requirements

### ADDED Requirements

### Requirement: Payment status presentation

The system SHALL provide a reusable payment-status UI component for the customer
frontend that renders booking payment progress and outcome states consistently
across booking-related pages.

#### Scenario: Render pending payment state

- **WHEN** a booking is awaiting payment and a payment deadline is available
- **THEN** the system renders a pending payment state with the localized pending
  label and deadline/countdown information

#### Scenario: Render redirecting payment state

- **WHEN** the frontend has initiated checkout handoff to an external payment
  page
- **THEN** the system renders a localized redirecting state with loading
  feedback indicating the user is being sent to secure payment

#### Scenario: Render payment outcome states

- **WHEN** the payment status is successful, failed, or expired
- **THEN** the system renders a distinct localized success, failure, or expired
  presentation with the appropriate follow-up action controls

### Requirement: Payment countdown behavior

The system SHALL derive and update payment time remaining from the booking's
payment deadline in client-rendered payment-status views.

#### Scenario: Update countdown every second

- **WHEN** a pending payment state is shown with a future deadline
- **THEN** the system updates the displayed remaining time at 1-second intervals

#### Scenario: Highlight urgent countdown state

- **WHEN** the remaining payment time falls below five minutes
- **THEN** the system highlights the countdown using destructive urgency styling

#### Scenario: Transition to expired state locally

- **WHEN** the countdown reaches zero before the page receives a refreshed paid
  status
- **THEN** the system renders the payment session as expired and disables the
  pending countdown presentation

### Requirement: Payment status actions

The system SHALL allow booking pages to attach context-appropriate actions to
payment status states.

#### Scenario: Retry failed payment

- **WHEN** the payment status is failed and a retry handler is provided
- **THEN** the system renders a retry action that invokes the supplied retry
  behavior

#### Scenario: Restart expired booking flow

- **WHEN** the payment status is expired and a start-over handler is provided
- **THEN** the system renders a start-over action that invokes the supplied
  restart behavior

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

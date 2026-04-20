## ADDED Requirements

### Requirement: Payment and ticket translations

The system SHALL provide localized customer-facing copy for payment history,
payment detail, payment statuses, and ticket printing in both supported locales.

#### Scenario: Vietnamese payment and ticket translations

- **WHEN** locale là `vi` and the customer uses account payments, payment
  detail, or ticket print pages
- **THEN** the system shows Vietnamese labels for payment statuses, payment
  history states, payment detail fields, print actions, share actions, and
  printable ticket content

#### Scenario: English payment and ticket translations

- **WHEN** locale là `en` and the customer uses account payments, payment
  detail, or ticket print pages
- **THEN** the system shows English labels for payment statuses, payment history
  states, payment detail fields, print actions, share actions, and printable
  ticket content

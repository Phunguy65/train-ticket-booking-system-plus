## ADDED Requirements

### Requirement: Account dashboard payments tab

The system SHALL provide a localized payments tab within the protected customer
account dashboard in addition to the existing bookings view.

#### Scenario: Switch to payments tab

- **WHEN** an authenticated customer opens the account dashboard and selects the
  payments tab
- **THEN** the system renders the customer's payment history without navigating
  to a separate account page

### Requirement: Payment history list

The system SHALL list the authenticated customer's payments in the account
payments tab using the user payment history API.

#### Scenario: Render payment history entries

- **WHEN** `getUserPayments` returns one or more payment records for the
  authenticated customer
- **THEN** the payments tab renders payment cards showing payment status,
  amount, creation date, related booking summary, and an action to open payment
  details

#### Scenario: Render empty payment history state

- **WHEN** `getUserPayments` returns no payment records for the authenticated
  customer
- **THEN** the payments tab renders a localized empty state

#### Scenario: Retry payment history after failure

- **WHEN** the payment history request fails because of a network or transient
  API error
- **THEN** the payments tab renders a localized error state with a retry action

### Requirement: Payment detail access from account

The system SHALL allow authenticated customers to open a localized payment
detail page from the account payments tab.

#### Scenario: Navigate from payment card to payment detail

- **WHEN** the customer activates the view-details action for a payment in the
  payments tab
- **THEN** the system navigates to `/[locale]/payment/[id]` for that payment

### Requirement: Payment detail ticket print entry point

The system SHALL expose a print-ticket action from payment detail only when the
payment has been completed successfully.

#### Scenario: Show print-ticket action for paid payment

- **WHEN** the payment detail page loads a payment with status `PAID`
- **THEN** the system renders a print-ticket action linked to the localized
  ticket route for the associated booking

#### Scenario: Hide print-ticket action for unpaid payment

- **WHEN** the payment detail page loads a payment with a non-`PAID` status
- **THEN** the system does not render the print-ticket action

## Purpose

Define the protected customer account experience for viewing bookings, payment
history, payment details, ticket-print entry points, cancellations, and profile
management.

## Requirements

### ADDED Requirements

### Requirement: Protected account routes

The system SHALL protect the customer account area so unauthenticated users are
redirected to the localized login page before account content is rendered.

#### Scenario: Unauthenticated access to account routes

- **WHEN** an unauthenticated user requests `/[locale]/account` or
  `/[locale]/account/profile`
- **THEN** the system redirects the user to `/[locale]/login`

#### Scenario: Authenticated access to account routes

- **WHEN** an authenticated user requests an account route
- **THEN** the system renders the protected account content

### Requirement: User bookings dashboard

The system SHALL provide an account dashboard at `/[locale]/account` that lists
the authenticated user's bookings using `getUserBookings`.

#### Scenario: Switch to payments tab

- **WHEN** an authenticated customer opens the account dashboard and selects the
  payments tab
- **THEN** the system renders the customer's payment history without navigating
  to a separate account page

#### Scenario: Render bookings list

- **WHEN** `getUserBookings` returns one or more bookings for the authenticated
  user
- **THEN** the dashboard renders each booking with booking identifier, trip
  summary, status, and actions to view details or cancel when allowed

#### Scenario: Render empty bookings state

- **WHEN** `getUserBookings` returns no bookings
- **THEN** the dashboard renders a localized empty state for first-time or
  inactive customers

#### Scenario: Retry bookings query after failure

- **WHEN** the bookings list request fails because of a network or transient API
  error
- **THEN** the dashboard renders an error state with a retry action

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

### Requirement: Booking status badges

The system SHALL show a localized visual badge for each booking status in the
dashboard and booking detail views.

#### Scenario: Render known booking status labels

- **WHEN** a booking has status `HELD`, `CONFIRMED`, or `CANCELLED`
- **THEN** the system renders a distinct translated badge for that status

### Requirement: Booking detail access from account

The system SHALL allow authenticated users to open details for a specific
booking from the account area.

#### Scenario: Navigate from bookings list to detail

- **WHEN** the user activates the view-details action for a booking in the
  account dashboard
- **THEN** the system navigates to a localized booking detail view for that
  booking

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

### Requirement: Booking cancellation flow

The system SHALL allow eligible bookings to be cancelled from the account area
with explicit confirmation.

#### Scenario: Confirm cancellation before mutation

- **WHEN** the user chooses to cancel a cancellable booking
- **THEN** the system opens a confirmation dialog before any cancellation
  request is sent

#### Scenario: Cancel booking successfully

- **WHEN** the user confirms cancellation and `cancelBooking` succeeds
- **THEN** the system updates the booking status in the UI, invalidates affected
  booking queries, and shows localized success feedback

#### Scenario: Cancellation request fails

- **WHEN** `cancelBooking` fails because of a network, permission, or state
  error
- **THEN** the system keeps the current booking visible and shows localized
  error feedback

### Requirement: Profile view and edit page

The system SHALL provide a profile page at `/[locale]/account/profile` that lets
authenticated users view and edit their profile using `getAuthenticatedUser` and
`updateAuthenticatedUser`.

#### Scenario: Load existing profile information

- **WHEN** an authenticated user opens the profile page
- **THEN** the system loads the current authenticated user data and populates
  the profile form fields

#### Scenario: Save valid profile changes

- **WHEN** the user submits valid edited profile information
- **THEN** the system calls `updateAuthenticatedUser`, refreshes the displayed
  user data, and shows localized success feedback

#### Scenario: Show inline validation for invalid profile input

- **WHEN** the user submits profile changes that fail client-side validation
- **THEN** the system shows inline translated validation messages and does not
  call the update API

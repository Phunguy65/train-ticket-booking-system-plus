# ADDED Requirements

## Requirement: Protected account routes

The system SHALL protect the customer account area so unauthenticated users are
redirected to the localized login page before account content is rendered.

### Scenario: Unauthenticated access to account routes

- **WHEN** an unauthenticated user requests `/[locale]/account` or
  `/[locale]/account/profile`
- **THEN** the system redirects the user to `/[locale]/login`

### Scenario: Authenticated access to account routes

- **WHEN** an authenticated user requests an account route
- **THEN** the system renders the protected account content

## Requirement: User bookings dashboard

The system SHALL provide an account dashboard at `/[locale]/account` that lists
the authenticated user's bookings using `getUserBookings`.

### Scenario: Render bookings list

- **WHEN** `getUserBookings` returns one or more bookings for the authenticated
  user
- **THEN** the dashboard renders each booking with booking identifier, trip
  summary, status, and actions to view details or cancel when allowed

### Scenario: Render empty bookings state

- **WHEN** `getUserBookings` returns no bookings
- **THEN** the dashboard renders a localized empty state for first-time or
  inactive customers

### Scenario: Retry bookings query after failure

- **WHEN** the bookings list request fails because of a network or transient API
  error
- **THEN** the dashboard renders an error state with a retry action

## Requirement: Booking status badges

The system SHALL show a localized visual badge for each booking status in the
dashboard and booking detail views.

### Scenario: Render known booking status labels

- **WHEN** a booking has status `HELD`, `CONFIRMED`, or `CANCELLED`
- **THEN** the system renders a distinct translated badge for that status

## Requirement: Booking detail access from account

The system SHALL allow authenticated users to open details for a specific
booking from the account area.

### Scenario: Navigate from bookings list to detail

- **WHEN** the user activates the view-details action for a booking in the
  account dashboard
- **THEN** the system navigates to a localized booking detail view for that
  booking

## Requirement: Booking cancellation flow

The system SHALL allow eligible bookings to be cancelled from the account area
with explicit confirmation.

### Scenario: Confirm cancellation before mutation

- **WHEN** the user chooses to cancel a cancellable booking
- **THEN** the system opens a confirmation dialog before any cancellation
  request is sent

### Scenario: Cancel booking successfully

- **WHEN** the user confirms cancellation and `cancelBooking` succeeds
- **THEN** the system updates the booking status in the UI, invalidates affected
  booking queries, and shows localized success feedback

### Scenario: Cancellation request fails

- **WHEN** `cancelBooking` fails because of a network, permission, or state
  error
- **THEN** the system keeps the current booking visible and shows localized
  error feedback

## Requirement: Profile view and edit page

The system SHALL provide a profile page at `/[locale]/account/profile` that lets
authenticated users view and edit their profile using `getAuthenticatedUser` and
`updateAuthenticatedUser`.

### Scenario: Load existing profile information

- **WHEN** an authenticated user opens the profile page
- **THEN** the system loads the current authenticated user data and populates
  the profile form fields

### Scenario: Save valid profile changes

- **WHEN** the user submits valid edited profile information
- **THEN** the system calls `updateAuthenticatedUser`, refreshes the displayed
  user data, and shows localized success feedback

### Scenario: Show inline validation for invalid profile input

- **WHEN** the user submits profile changes that fail client-side validation
- **THEN** the system shows inline translated validation messages and does not
  call the update API

# MODIFIED Requirements

## Requirement: Multi-seat selection rules

The system SHALL allow users to select multiple available seats, capped at the
configured maximum seats per booking exposed by the customer application and
backed by a default of five seats.

### Scenario: Select and deselect available seats

- **WHEN** the user activates an available seat in the current coach grid
- **THEN** the system toggles that seat in the current selection set and updates
  the selected-seat summary

### Scenario: Enforce configured seat-selection limit

- **WHEN** the user has already selected the configured maximum number of seats
  and attempts to select an additional available seat
- **THEN** the system blocks the extra selection and shows localized feedback
  explaining the maximum-seat rule

## Requirement: Booking confirmation page

The system SHALL provide a booking confirmation page at `/[locale]/booking` that
summarizes the chosen trip, seats, booker information, per-seat passenger
information, total cost, and flow progress before creating a booking.

### Scenario: Render confirmation from selected trip and seats

- **WHEN** the user arrives on the booking page with a valid trip identifier and
  selected seat identifiers
- **THEN** the system renders the selected trip summary, selected seats, booker
  summary, one passenger form per selected seat, total price, and the current
  booking-step indicator

### Scenario: Missing booking context prevents confirmation

- **WHEN** the user opens the booking page without the required trip or seat
  context
- **THEN** the system blocks booking submission and directs the user back to a
  valid step in the flow

### Scenario: Show reusable trip summary and price breakdown

- **WHEN** the booking confirmation page is rendered with valid context
- **THEN** the system shows a trip summary card and itemized price breakdown
  before booking submission

### Scenario: Mobile review action stays visible

- **WHEN** the user reviews booking confirmation on a mobile viewport
- **THEN** the system keeps the total and confirm action accessible in a sticky
  footer while desktop layouts continue to show inline actions

### Scenario: Confirmation blocks submit until passenger forms are valid

- **WHEN** any selected seat is missing passenger information or two passenger
  forms reuse the same identity document number
- **THEN** the system disables booking confirmation and shows localized
  validation feedback until the passenger data becomes valid

## Requirement: Booking creation and confirmation

The system SHALL create a booking with `createBooking`, including one passenger
payload per selected seat, show the booking confirmation information, and
continue the user to payment checkout.

### Scenario: Create booking successfully

- **WHEN** the user confirms booking with valid trip and seat selections and a
  complete passenger list
- **THEN** the system calls `createBooking` with the selected seat identifiers,
  trip context, and the passenger assignments

### Scenario: Show confirmation metadata after booking creation

- **WHEN** `createBooking` succeeds
- **THEN** the system shows the created booking reference, payment deadline,
  booker information, passenger list, and a localized payment status experience
  before redirecting the user to the returned payment checkout URL

### Scenario: Automatically continue to payment checkout

- **WHEN** `createBooking` succeeds and returns a checkout URL
- **THEN** the system automatically redirects the user to payment checkout after
  a short confirmation delay while showing localized redirecting feedback

### Scenario: Booking creation failure

- **WHEN** `createBooking` fails because the seats are no longer available,
  passenger validation fails, or a network/API error occurs
- **THEN** the system preserves the user's context, shows localized error
  feedback, and allows the user to retry or go back to seat selection

## Requirement: Booking detail page

The system SHALL provide a booking detail page at
`/[locale]/booking/[bookingId]` that displays the details of a created booking.

### Scenario: View booking detail by booking identifier

- **WHEN** the user opens a valid booking detail route
- **THEN** the system loads `getBooking` and renders booking status, trip
  information, selected seats, booker information, passenger assignments, and
  payment deadline data

### Scenario: Show pending payment guidance for held bookings

- **WHEN** the booking detail page loads a booking whose status is `HELD`
- **THEN** the system prominently displays the payment countdown/status UI and a
  pay action when checkout remains available

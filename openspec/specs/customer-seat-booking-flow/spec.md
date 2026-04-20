# ADDED Requirements

## Requirement: Seat selection page

The system SHALL provide a seat selection page at
`/[locale]/trips/[tripId]/seats` where users can inspect coach layouts and pick
up to five seats for a trip.

### Scenario: Load seat selection for a trip

- **WHEN** the user opens the seat selection page for a valid trip identifier
- **THEN** the system loads coach seat-map data for that trip and renders the
  seat selection interface

### Scenario: Invalid or unavailable trip seat data

- **WHEN** the seat-map request cannot load a valid trip seat layout
- **THEN** the system renders a localized recoverable error state instead of an
  unusable seat grid

## Requirement: Coach navigation and seat map status rendering

The system SHALL use `getCoachSeatMap` to render coach tabs and a visual seat
grid whose seats expose availability status.

### Scenario: Switch between coaches

- **WHEN** the seat-map response contains multiple coaches and the user selects
  a different coach tab
- **THEN** the system renders the seat grid for the selected coach

### Scenario: Render seat statuses visually and accessibly

- **WHEN** the seat grid is displayed
- **THEN** each seat communicates whether it is available, held, or booked using
  visible status styling and screen-reader-readable state

### Scenario: Non-available seats cannot be selected

- **WHEN** the user attempts to activate a held or booked seat
- **THEN** the system prevents selection of that seat

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

## Requirement: Selected seat summary

The system SHALL display a booking summary on the seat-selection page showing
the selected seats and total price, while keeping the primary continue action
prominent across mobile and desktop layouts.

### Scenario: Update summary as seats change

- **WHEN** the current seat selection changes
- **THEN** the system updates the summary with the selected seat labels, seat
  count, and recalculated total price

### Scenario: Continue disabled without seat selection

- **WHEN** the user has not selected any seat
- **THEN** the continue-to-booking action remains disabled

### Scenario: Mobile sticky action mirrors selection state

- **WHEN** the user is on the seat-selection page on a mobile viewport
- **THEN** the system shows a sticky footer summarizing the current price and
  the continue action while keeping the desktop inline summary available on
  larger viewports

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

## Requirement: Booking flow progress indicator

The system SHALL show a consistent four-step booking progress indicator across
the customer booking journey.

### Scenario: Show current step on booking pages

- **WHEN** the user is on seat selection, booking review, or booking payment
  detail screens
- **THEN** the system renders a localized stepper for `Search`, `Seats`,
  `Review`, and `Payment` with the current step highlighted and prior steps
  marked complete

### Scenario: Restrict step navigation to valid backward steps

- **WHEN** the user activates the stepper
- **THEN** the system allows navigation only to previously completed steps and
  prevents navigation to future steps that have not been reached yet

### Scenario: Compact mobile step display

- **WHEN** the stepper is rendered on a mobile viewport
- **THEN** the system presents a compact localized "Step X of Y" style summary
  with progress dots instead of full-width desktop labels

## Requirement: Collapsible trip summary presentation

The system SHALL present booking trip details in a reusable summary card that is
responsive to viewport size.

### Scenario: Mobile summary card collapses by default

- **WHEN** the summary card is rendered on a mobile viewport
- **THEN** the system shows the key trip identity in the collapsed header and
  keeps the detailed train, route, time, and seat information behind an
  expandable control

### Scenario: Desktop summary card remains expanded

- **WHEN** the summary card is rendered on a desktop viewport
- **THEN** the system keeps the trip details expanded without requiring user
  interaction

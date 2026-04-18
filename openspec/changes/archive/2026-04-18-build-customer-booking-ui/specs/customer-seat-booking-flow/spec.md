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

The system SHALL allow users to select multiple available seats, capped at five
seats per booking attempt.

### Scenario: Select and deselect available seats

- **WHEN** the user activates an available seat in the current coach grid
- **THEN** the system toggles that seat in the current selection set and updates
  the selected-seat summary

### Scenario: Enforce five-seat selection limit

- **WHEN** the user has already selected five seats and attempts to select an
  additional available seat
- **THEN** the system blocks the extra selection and shows localized feedback
  explaining the maximum-seat rule

## Requirement: Selected seat summary

The system SHALL display a booking summary on the seat-selection page showing
the selected seats and total price.

### Scenario: Update summary as seats change

- **WHEN** the current seat selection changes
- **THEN** the system updates the summary with the selected seat labels, seat
  count, and recalculated total price

### Scenario: Continue disabled without seat selection

- **WHEN** the user has not selected any seat
- **THEN** the continue-to-booking action remains disabled

## Requirement: Booking confirmation page

The system SHALL provide a booking confirmation page at `/[locale]/booking` that
summarizes the chosen trip, seats, passenger information, and total cost before
creating a booking.

### Scenario: Render confirmation from selected trip and seats

- **WHEN** the user arrives on the booking page with a valid trip identifier and
  selected seat identifiers
- **THEN** the system renders the selected trip summary, selected seats, total
  price, and authenticated passenger information

### Scenario: Missing booking context prevents confirmation

- **WHEN** the user opens the booking page without the required trip or seat
  context
- **THEN** the system blocks booking submission and directs the user back to a
  valid step in the flow

## Requirement: Booking creation and confirmation

The system SHALL create a booking with `createBooking`, show the booking
confirmation information, and continue the user to payment checkout.

### Scenario: Create booking successfully

- **WHEN** the user confirms booking with valid trip and seat selections
- **THEN** the system calls `createBooking` with the selected seat identifiers
  and trip context

### Scenario: Show confirmation metadata after booking creation

- **WHEN** `createBooking` succeeds
- **THEN** the system shows the created booking reference, payment deadline, and
  a localized payment-redirect loading state before redirecting the user to the
  returned payment checkout URL

### Scenario: Automatically continue to payment checkout

- **WHEN** `createBooking` succeeds and returns a checkout URL
- **THEN** the system automatically redirects the user to payment checkout after
  a short confirmation delay

### Scenario: Booking creation failure

- **WHEN** `createBooking` fails because the seats are no longer available or a
  network/API error occurs
- **THEN** the system preserves the user's context, shows localized error
  feedback, and allows the user to retry or go back to seat selection

## Requirement: Booking detail page

The system SHALL provide a booking detail page at
`/[locale]/booking/[bookingId]` that displays the details of a created booking.

### Scenario: View booking detail by booking identifier

- **WHEN** the user opens a valid booking detail route
- **THEN** the system loads `getBooking` and renders booking status, trip
  information, selected seats, passenger information, and payment deadline data

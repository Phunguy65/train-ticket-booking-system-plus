## Purpose

Define the localized printable ticket experience, including ticket rendering, QR
generation, printing, and sharing behavior.

## Requirements

### ADDED Requirements

### Requirement: Printable ticket route

The system SHALL provide a localized printable ticket page at
`/[locale]/ticket/[bookingId]` for paid bookings.

#### Scenario: Render ticket print page for a paid booking

- **WHEN** a customer opens the localized ticket route for a booking that has an
  associated paid payment
- **THEN** the system renders a printable ticket view with passenger, train,
  route, timing, and seat information

### Requirement: Printable ticket content

The system SHALL render the printable ticket from JSON-backed frontend data and
include the booking details needed for travel validation and customer reference.

#### Scenario: Show ticket identity and travel details

- **WHEN** the printable ticket page loads successfully
- **THEN** the ticket displays booking identifier, booker information, passenger
  assignments, train name and number, origin, destination, departure and arrival
  times, and the reserved seats associated with each passenger

#### Scenario: Show ticket QR code

- **WHEN** the printable ticket page loads successfully
- **THEN** the ticket displays a client-generated QR code derived from
  booking-related ticket data

### Requirement: Ticket print and share actions

The system SHALL provide customer actions for printing and, when supported,
sharing the ticket from the ticket page.

#### Scenario: Print ticket from browser

- **WHEN** the customer activates the print action on the ticket page
- **THEN** the system triggers the browser print dialog and applies
  print-specific styling that hides non-ticket controls

#### Scenario: Share ticket on supported mobile browsers

- **WHEN** the customer activates the share action in a browser that supports
  the Web Share API
- **THEN** the system invokes the native share flow with ticket-related
  information

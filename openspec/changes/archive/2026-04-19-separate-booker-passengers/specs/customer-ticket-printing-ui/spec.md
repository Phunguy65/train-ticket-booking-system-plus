# MODIFIED Requirements

## Requirement: Printable ticket content

The system SHALL render the printable ticket from JSON-backed frontend data and
include the booking details needed for travel validation and customer reference.

### Scenario: Show ticket identity and travel details

- **WHEN** the printable ticket page loads successfully
- **THEN** the ticket displays booking identifier, booker information, passenger
  assignments, train name and number, origin, destination, departure and arrival
  times, and the reserved seats associated with each passenger

### Scenario: Show ticket QR code

- **WHEN** the printable ticket page loads successfully
- **THEN** the ticket displays a client-generated QR code derived from
  booking-related ticket data

# ADDED Requirements

## Requirement: Booking creation schema supports per-seat passengers

The system SHALL describe booking creation requests with a `passengers` array,
where each item contains `seatId`, `fullName`, `idDocumentNumber`,
`dateOfBirth`, and `gender`.

### Scenario: Document booking creation passenger payload

- **WHEN** the generated customer API contract is inspected for booking creation
  operations
- **THEN** the request schema includes a required `passengers` array and each
  passenger item includes `seatId`, `fullName`, `idDocumentNumber`,
  `dateOfBirth`, and `gender`

## Requirement: Booking error contract documents passenger validation failures

The system SHALL document booking-creation validation failures for configured
seat-limit overflow, passenger-seat count mismatch, duplicate passenger identity
document numbers, and invalid passenger-seat assignments.

### Scenario: Document passenger validation errors

- **WHEN** SDK consumers inspect the booking error schema or operation responses
- **THEN** the contract describes error variants for too many seats,
  passenger-seat mismatch, duplicate passenger ID documents, and invalid
  passenger-seat assignment

# MODIFIED Requirements

## Requirement: Payment detail schema includes ticket-ready booking data

The system SHALL describe payment detail responses with nested booking, booker,
passenger, seat, and trip data needed for ticket presentation and printing.

### Scenario: Document enriched payment detail response

- **WHEN** the generated customer API contract is inspected for payment detail
  operations
- **THEN** the payment detail schema includes booking `id`, `status`,
  `bookerInfo`, `passengers` with seat assignments, `seats` with seat and car
  numbers, and trip data with train name, train number, origin, destination,
  departure time, and arrival time

## Requirement: Booking detail schema exposes booker and passenger assignments

The system SHALL describe booking response schemas with distinct `bookerInfo`
and `passengers` fields so customer booking screens can render purchaser and
traveler data separately.

### Scenario: Document booking detail passenger fields

- **WHEN** the generated customer API contract is inspected for booking detail
  operations
- **THEN** the booking detail schema includes `bookerInfo` plus a `passengers`
  array whose items include `seatId`, `fullName`, `idDocumentNumber`,
  `dateOfBirth`, and `gender`

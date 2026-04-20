## Purpose

Define the public customer API contract requirements for booking creation,
booking detail, user payment history, and ticket-ready payment detail responses.

## Requirements

### ADDED Requirements

### Requirement: User payment history endpoint is available in the public customer contract

The system SHALL expose a protected customer API operation for retrieving the
authenticated user's payment history at `GET /api/v1/users/{userId}/payments`.

#### Scenario: Document paginated user payments operation

- **WHEN** the generated customer API contract is inspected
- **THEN** it includes a bearer-auth protected
  `GET /api/v1/users/{userId}/payments` operation with `userId`, `page`, and
  `size` parameters and a paginated success payload

### Requirement: User payment history schema is documented for SDK consumers

The system SHALL describe user payment history items with the fields needed by
the account payments UI, including payment identity, status, pricing, creation
timestamp, related booking identifier, and booking summary information.

#### Scenario: Document payment history item fields

- **WHEN** SDK consumers inspect the payment history success schema
- **THEN** each payment item includes `id`, `status`, `amount`, `currency`,
  `createdAt`, `bookingId`, and a nested booking summary containing route and
  travel-date information

### Requirement: Booking creation schema supports per-seat passengers

The system SHALL describe booking creation requests with a `passengers` array,
where each item contains `seatId`, `fullName`, `idDocumentNumber`,
`dateOfBirth`, and `gender`.

#### Scenario: Document booking creation passenger payload

- **WHEN** the generated customer API contract is inspected for booking creation
  operations
- **THEN** the request schema includes a required `passengers` array and each
  passenger item includes `seatId`, `fullName`, `idDocumentNumber`,
  `dateOfBirth`, and `gender`

### Requirement: Booking error contract documents passenger validation failures

The system SHALL document booking-creation validation failures for configured
seat-limit overflow, passenger-seat count mismatch, duplicate passenger identity
document numbers, and invalid passenger-seat assignments.

#### Scenario: Document passenger validation errors

- **WHEN** SDK consumers inspect the booking error schema or operation responses
- **THEN** the contract describes error variants for too many seats,
  passenger-seat mismatch, duplicate passenger ID documents, and invalid
  passenger-seat assignment

### Requirement: Payment detail schema includes ticket-ready booking data

The system SHALL describe payment detail responses with nested booking, booker,
passenger, seat, and trip data needed for ticket presentation and printing.

#### Scenario: Document enriched payment detail response

- **WHEN** the generated customer API contract is inspected for payment detail
  operations
- **THEN** the payment detail schema includes booking `id`, `status`,
  `bookerInfo`, `passengers` with seat assignments, `seats` with seat and car
  numbers, and trip data with train name, train number, origin, destination,
  departure time, and arrival time

### Requirement: Booking detail schema exposes booker and passenger assignments

The system SHALL describe booking response schemas with distinct `bookerInfo`
and `passengers` fields so customer booking screens can render purchaser and
traveler data separately.

#### Scenario: Document booking detail passenger fields

- **WHEN** the generated customer API contract is inspected for booking detail
  operations
- **THEN** the booking detail schema includes `bookerInfo` plus a `passengers`
  array whose items include `seatId`, `fullName`, `idDocumentNumber`,
  `dateOfBirth`, and `gender`

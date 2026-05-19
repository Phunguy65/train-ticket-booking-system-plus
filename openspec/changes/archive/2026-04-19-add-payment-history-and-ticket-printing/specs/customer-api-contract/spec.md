## ADDED Requirements

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

### Requirement: Payment detail schema includes ticket-ready booking data

The system SHALL describe payment detail responses with nested booking,
passenger, seat, and trip data needed for ticket presentation and printing.

#### Scenario: Document enriched payment detail response

- **WHEN** the generated customer API contract is inspected for payment detail
  operations
- **THEN** the payment detail schema includes booking `id`, `status`,
  `passengerInfo`, `seats` with seat and car numbers, and trip data with train
  name, train number, origin, destination, departure time, and arrival time

## Purpose

Define booking-domain requirements for storing separate booker information and
per-seat passenger assignments throughout booking creation, persistence, and
reads.

## Requirements

### ADDED Requirements

### Requirement: Booking stores separate booker and per-seat passenger snapshots

The system SHALL store the authenticated user as booking `bookerInfo` and SHALL
store a passenger snapshot for each booked seat, where each passenger includes
`seatId`, `fullName`, `idDocumentNumber`, `dateOfBirth`, and `gender`.

#### Scenario: Create booking with one passenger per seat

- **WHEN** a booking is created with valid selected seats and a matching
  passenger payload
- **THEN** the system persists the authenticated user snapshot as `bookerInfo`
  and persists one passenger snapshot for each selected seat

#### Scenario: Reconstitute existing booking without passenger snapshot

- **WHEN** the system loads a historical booking row whose `passengers_snapshot`
  is null
- **THEN** the booking remains readable and the passenger collection is treated
  as absent or empty without failing the read

### Requirement: Booking passenger identities are unique within a booking

The system SHALL reject bookings that contain more than one passenger with the
same `idDocumentNumber`.

#### Scenario: Duplicate passenger document numbers are rejected

- **WHEN** the booking request includes two or more passengers with the same
  `idDocumentNumber`
- **THEN** the system rejects the booking with a duplicate passenger document
  validation error

### Requirement: Booking passenger-seat assignments are complete and valid

The system SHALL require exactly one passenger for each selected seat and SHALL
accept passenger assignments only for seats included in the booking request.

#### Scenario: Passenger count does not match selected seat count

- **WHEN** the booking request contains fewer or more passengers than selected
  seats
- **THEN** the system rejects the booking with a passenger-seat mismatch error

#### Scenario: Passenger references an unselected seat

- **WHEN** a passenger payload references a `seatId` that is not present in the
  selected seat list
- **THEN** the system rejects the booking with an invalid passenger-seat
  assignment error

### Requirement: Booking creation enforces configurable maximum seat count

The system SHALL enforce a configurable maximum number of seats allowed per
booking using `booking.max-seats-per-booking`, defaulting to `5` when the
environment variable is not set.

#### Scenario: Seat count exceeds configured maximum

- **WHEN** a booking request contains more selected seats than the configured
  maximum
- **THEN** the system rejects the booking with a too-many-seats error

#### Scenario: Seat count within configured maximum

- **WHEN** a booking request contains selected seats at or below the configured
  maximum
- **THEN** the system continues normal booking validation and creation

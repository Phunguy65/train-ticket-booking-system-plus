# ADDED Requirements

## Requirement: Admin can bulk-create seats for a coach

The system SHALL provide an endpoint `POST /{version}/coaches/{coachId}/seats:bulkCreate` that accepts a list of seat definitions and creates all of them atomically for the specified coach. The endpoint SHALL be restricted to users with the `ADMIN` role. The request list SHALL contain between 1 and 100 items.

### Scenario: Successful bulk create

- **WHEN** an admin submits a valid list of seats (1–100 items, each with a unique, non-blank `seatNumber` of at most 10 characters, none already existing under the coach)
- **THEN** all seats are persisted in a single transaction and the system returns HTTP 201 with a JSON body containing the created seats list and `createdCount`

### Scenario: Coach not found

- **WHEN** an admin submits a bulk-create request for a `coachId` that does not exist
- **THEN** the system returns HTTP 404 with error code `COACH_NOT_FOUND` and no seats are created

### Scenario: Duplicate seat numbers within the request

- **WHEN** an admin submits a list where two or more items share the same `seatNumber`
- **THEN** the system returns HTTP 422 with error code `SEAT_DUPLICATE_SEAT_NUMBERS_IN_REQUEST`, listing the duplicated seat numbers, and no seats are created

### Scenario: Seat numbers already exist in the coach (DB conflict)

- **WHEN** an admin submits a list where one or more `seatNumber` values already exist for the given coach in the database
- **THEN** the system returns HTTP 409 with error code `SEAT_NUMBERS_ALREADY_EXIST`, listing the conflicting seat numbers, and no seats are created

### Scenario: Request list is empty

- **WHEN** an admin submits a request with an empty `seats` list
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR`

### Scenario: Request list exceeds 100 items

- **WHEN** an admin submits a request with more than 100 seat items
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR`

### Scenario: Item has invalid seat number

- **WHEN** an admin submits a list containing an item with a blank `seatNumber` or a `seatNumber` exceeding 10 characters
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR` identifying the offending field(s), and no seats are created

### Scenario: Unauthenticated request

- **WHEN** a request is made without a valid admin token
- **THEN** the system returns HTTP 401 or 403 and no seats are created

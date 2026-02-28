# ADDED Requirements

## Requirement: Admin can bulk-create coaches for a train

The system SHALL provide an endpoint `POST /{version}/trains/{trainId}/coaches:bulkCreate` that accepts a list of coach definitions and creates all of them atomically for the specified train. The endpoint SHALL be restricted to users with the `ADMIN` role. The request list SHALL contain between 1 and 100 items.

### Scenario: Successful bulk create

- **WHEN** an admin submits a valid list of coaches (1–100 items, all with unique `carNumber` values not already in the train, each `carNumber` and `totalSeats` positive)
- **THEN** all coaches are persisted in a single transaction and the system returns HTTP 201 with a JSON body containing the created coaches list and `createdCount`

### Scenario: Train not found

- **WHEN** an admin submits a bulk-create request for a `trainId` that does not exist
- **THEN** the system returns HTTP 404 with error code `COACH_TRAIN_NOT_FOUND` and no coaches are created

### Scenario: Duplicate car numbers within the request

- **WHEN** an admin submits a list where two or more items share the same `carNumber`
- **THEN** the system returns HTTP 422 with error code `COACH_DUPLICATE_CAR_NUMBERS_IN_REQUEST`, listing the duplicated car numbers, and no coaches are created

### Scenario: Car numbers already exist in the train (DB conflict)

- **WHEN** an admin submits a list where one or more `carNumber` values already exist for the given train in the database
- **THEN** the system returns HTTP 409 with error code `COACH_CAR_NUMBERS_ALREADY_EXIST`, listing the conflicting car numbers, and no coaches are created

### Scenario: Request list is empty

- **WHEN** an admin submits a request with an empty `coaches` list
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR`

### Scenario: Request list exceeds 100 items

- **WHEN** an admin submits a request with more than 100 coach items
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR`

### Scenario: Item has invalid fields

- **WHEN** an admin submits a list containing an item with `carNumber` ≤ 0 or `totalSeats` ≤ 0
- **THEN** the system returns HTTP 400 with `VALIDATION_ERROR` identifying the offending field(s), and no coaches are created

### Scenario: Unauthenticated request

- **WHEN** a request is made without a valid admin token
- **THEN** the system returns HTTP 401 or 403 and no coaches are created

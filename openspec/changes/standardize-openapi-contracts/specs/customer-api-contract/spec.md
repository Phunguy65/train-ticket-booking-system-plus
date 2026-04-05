# ADDED Requirements

## Requirement: Generated customer API contract matches public backend endpoints

The system SHALL generate the customer-facing OpenAPI contract from backend code
annotations instead of maintaining the contract as a handwritten source. The
generated contract MUST cover every public customer REST endpoint exposed by the
backend, and it MUST exclude internal-only endpoints such as Stripe webhooks and
SSE streams.

### Scenario: Generate customer contract from backend annotations

- **WHEN** the backend generates its OpenAPI document
- **THEN** the contract is derived from Spring controllers, request DTOs,
  response DTOs, enums, and shared web payload types in code

### Scenario: Exclude internal-only endpoints from the customer contract

- **WHEN** the backend generates the customer API contract
- **THEN** Stripe webhook endpoints and SSE endpoints do not appear in the
  generated customer contract

## Requirement: Public endpoint schemas are documented consistently

The generated customer API contract SHALL describe public controller operations
with stable operation identifiers, path/query/header parameters, request bodies,
success payloads, shared failure payloads, and security requirements. Public
auth endpoints MUST be documented as unauthenticated, and protected endpoints
MUST be documented with bearer authentication.

### Scenario: Public auth endpoints remain unauthenticated in generated contract

- **WHEN** the generated contract is inspected for register, login,
  refresh-token, and logout operations
- **THEN** those operations do not require bearer authentication in the contract

### Scenario: Protected customer endpoints declare bearer authentication

- **WHEN** the generated contract is inspected for protected customer endpoints
  such as profile, bookings, payments, and protected trip resources
- **THEN** those operations declare bearer authentication in the contract

### Scenario: Parameter and body metadata is present for public operations

- **WHEN** the generated contract is inspected for any public customer endpoint
- **THEN** path, query, header, and request-body fields are described with
  stable names, types, and validation-aligned metadata

## Requirement: Success and failure payloads are represented for SDK consumption

The generated customer API contract SHALL expose successful responses as the
underlying business payload shape expected by SDK consumers, while failure
responses MUST document the shared JSend-based error envelope used by the
runtime API.

### Scenario: Successful responses are exposed as unwrapped payloads

- **WHEN** heyapi reads the generated customer API contract
- **THEN** success response schemas reflect the underlying business payload
  instead of the outer JSend success wrapper

### Scenario: Failure responses use shared JSend fail or error schemas

- **WHEN** the generated contract is inspected for 4xx and 5xx responses
- **THEN** those responses reference shared error schemas that describe
  validation failures, domain failures, and technical failures consistently

## Requirement: Shared request and response models preserve real backend data shapes

The generated customer API contract SHALL preserve the actual backend field
types, enum values, and nesting used by public request and response models,
including UUID identifiers, pagination wrappers, nested records, sensitive-field
metadata, and shared error code enums.

### Scenario: Request and response schemas match backend field types

- **WHEN** the generated contract is compared against public backend request and
  response records
- **THEN** UUID fields, dates, timestamps, arrays, nested objects, and enum
  values match the runtime model definitions

### Scenario: Sensitive fields use conservative schema metadata

- **WHEN** the generated contract is inspected for passwords, tokens, personal
  identifiers, addresses, and similar sensitive values
- **THEN** those fields use conservative schema metadata such as `writeOnly` and
  non-sensitive examples

## Requirement: Pagination styles are explicit in the generated contract

The generated customer API contract SHALL distinguish offset-based pagination
from cursor-based pagination in both request parameters and response schemas.

### Scenario: Offset-based endpoints expose page semantics

- **WHEN** the generated contract is inspected for offset-based list endpoints
- **THEN** those endpoints document `page` and `size` query parameters and
  return the `PageResponse` metadata fields expected by consumers

### Scenario: Cursor-based endpoints expose cursor semantics

- **WHEN** the generated contract is inspected for cursor-based search endpoints
- **THEN** those endpoints document `cursor` and `size` query parameters and
  return the `SliceResponse` metadata fields expected by consumers

## Requirement: Shared contract artifact is available for downstream tooling

The system SHALL make the generated customer API contract available to
downstream tooling and shared consumers, including a checked-in copy at
`shared/api-contracts/openapi.yaml` aligned with the backend-generated output.

### Scenario: Generated contract is mirrored to shared artifact location

- **WHEN** the customer API contract generation workflow runs
- **THEN** `shared/api-contracts/openapi.yaml` is updated to reflect the
  backend-generated customer contract

### Scenario: SDK generation consumes the generated customer contract

- **WHEN** heyapi generates the frontend client SDK
- **THEN** it reads the generated customer contract and produces types and
  operations for the public customer API surface

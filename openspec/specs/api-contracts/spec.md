# Capability: api-contracts

## Purpose

OpenAPI 3.0 specification defining REST API contracts for the train ticket
booking system.

## Requirements

### Requirement: OpenAPI specification file

The system SHALL provide an OpenAPI 3.0 specification file defining the REST API
contract.

#### Scenario: OpenAPI file exists

- **WHEN** project initialization completes
- **THEN** shared/api-contracts/openapi.yaml exists

#### Scenario: API metadata defined

- **WHEN** openapi.yaml is examined
- **THEN** it contains info section with title, version, and description

### Requirement: Core API endpoints

The system SHALL define core API endpoints for authentication, trains, and
bookings.

#### Scenario: Authentication endpoints defined

- **WHEN** openapi.yaml is examined
- **THEN** it defines POST /api/v1/auth/login and POST /api/v1/auth/register
  endpoints

#### Scenario: Train endpoints defined

- **WHEN** openapi.yaml is examined
- **THEN** it defines GET /api/v1/trains and GET /api/v1/trains/{id} endpoints

#### Scenario: Booking endpoints defined

- **WHEN** openapi.yaml is examined
- **THEN** it defines POST /api/v1/bookings, GET /api/v1/bookings, and GET
  /api/v1/bookings/{id} endpoints

### Requirement: Data schemas

The system SHALL define reusable schemas for request and response objects.

#### Scenario: Core schemas defined

- **WHEN** openapi.yaml is examined
- **THEN** components.schemas section contains User, Train, Booking, Seat, and
  Error schemas

### Requirement: Authentication scheme

The system SHALL define JWT Bearer authentication scheme.

#### Scenario: Security scheme defined

- **WHEN** openapi.yaml is examined
- **THEN** components.securitySchemes contains bearerAuth with type http and
  scheme bearer

#### Scenario: Protected endpoints marked

- **WHEN** openapi.yaml is examined
- **THEN** booking endpoints have security requirement for bearerAuth

### Requirement: Error responses

The system SHALL define standard error response formats.

#### Scenario: Error schema defined

- **WHEN** openapi.yaml is examined
- **THEN** Error schema contains code, message, and details fields

#### Scenario: Error responses documented

- **WHEN** openapi.yaml endpoints are examined
- **THEN** they include 400, 401, 404, and 500 error responses

### Requirement: API versioning

The system SHALL use URI path versioning with /api/v1 prefix.

#### Scenario: Version prefix used

- **WHEN** openapi.yaml is examined
- **THEN** all paths start with /api/v1

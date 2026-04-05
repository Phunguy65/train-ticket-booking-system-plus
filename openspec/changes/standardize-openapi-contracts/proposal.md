# Why

The customer API contract is currently split between handwritten OpenAPI content
in `shared/api-contracts/openapi.yaml` and the actual Spring controllers,
request DTOs, and response DTOs in `backend/`. The handwritten spec no longer
matches the codebase, which blocks reliable client SDK generation with heyapi
and creates avoidable drift whenever backend endpoints evolve.

## What Changes

- Standardize customer-facing controllers, request DTOs, response DTOs, and
  shared web payload types so springdoc can generate an accurate OpenAPI
  document from code annotations.
- Replace the outdated handwritten customer API contract with a generated
  contract served by the backend and mirrored to
  `shared/api-contracts/openapi.yaml` for shared consumption.
- Document request parameters, response payloads, pagination models, error
  envelopes, and security requirements consistently across all public customer
  API endpoints.
- Define how JSend envelopes, validation errors, shared error codes, and
  paginated responses appear in generated OpenAPI so heyapi can generate a
  usable frontend SDK.
- Exclude internal-only endpoints such as Stripe webhooks and SSE streams from
  the generated customer contract.

## Capabilities

### New Capabilities

- `customer-api-contract`: Defines the generated OpenAPI contract for
  customer-facing REST endpoints, including request/response schemas,
  pagination, validation errors, and endpoint visibility rules.

### Modified Capabilities

- None.

## Impact

- Affected backend code: Springdoc configuration, shared web payload types, all
  public customer controllers, and request/response DTO annotations in
  `backend/src/main/java`.
- Affected shared artifacts: `shared/api-contracts/openapi.yaml` becomes a
  generated contract aligned with backend behavior.
- Affected frontend integration: heyapi generation can consume the generated
  contract to produce a typed fetch-based SDK.
- Affected dependencies and tooling: springdoc output configuration and API
  generation workflow in local development / CI.

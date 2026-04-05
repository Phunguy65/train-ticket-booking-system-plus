# Context

The backend already uses Spring Boot with `springdoc-openapi-starter-webmvc-ui`,
but the customer API contract consumed by other parts of the repository is still
maintained manually in `shared/api-contracts/openapi.yaml`. The controller layer
already contains partial Swagger annotations such as `@Tag` and `@Operation`,
yet the generated contract does not currently reflect the real request and
response payloads used by the application.

The mismatch is now material: controller paths are versioned, request and
response records use UUIDs and JSend envelopes, pagination uses both
offset-based and cursor-based wrappers, and internal-only endpoints such as
Stripe webhooks and SSE streams must not appear in the public contract. The
generated contract also needs to be suitable for heyapi to generate a
fetch-based frontend SDK without depending on handwritten OpenAPI maintenance.

## Goals / Non-Goals

**Goals:**

- Make springdoc the single source of truth for the customer-facing REST
  contract.
- Standardize public controller annotations, request DTO annotations, response
  DTO annotations, and shared web payload annotations so the generated OpenAPI
  document is accurate and stable.
- Represent public success payloads in a way that keeps heyapi-generated client
  types ergonomic while still documenting shared failure envelopes and
  validation errors.
- Distinguish offset-based pagination and cursor-based pagination clearly in the
  generated contract.
- Keep internal-only endpoints such as Stripe webhooks and SSE streams out of
  the customer API contract.
- Mirror the generated contract into `shared/api-contracts/openapi.yaml` so
  shared tooling can consume a checked-in artifact.

**Non-Goals:**

- Changing business logic, authorization rules, endpoint semantics, or response
  payload content beyond annotation and documentation alignment.
- Publishing an admin/internal API contract.
- Replacing JSend in runtime responses.
- Converting the frontend to a different HTTP client or SDK generator.

## Decisions

### 1. Springdoc becomes the contract source of truth

The backend-generated OpenAPI document will become the authoritative customer
API contract. `shared/api-contracts/openapi.yaml` will be treated as a
mirrored/generated artifact instead of a handwritten source.

Why this choice:

- The current handwritten YAML is already drifted from the codebase.
- Controllers, DTOs, enums, and validation rules already exist in code and can
  be annotated directly.
- heyapi can consume the generated contract from the backend and avoids
  duplicated manual maintenance.

Alternatives considered:

- Keep handwritten YAML and verify it against code: rejected because it
  preserves double maintenance.
- Maintain separate backend and frontend specs: rejected because it duplicates
  the same public API contract.

### 2. Public success schemas are unwrapped for SDK generation

Runtime responses remain JSend, but the generated OpenAPI contract for
successful responses will expose the underlying domain payload rather than the
outer `JsendResponse<T>` wrapper. Failure responses remain explicitly documented
as shared JSend-based error envelopes.

Why this choice:

- The frontend decided to use heyapi with fetch and wants ergonomic generated
  types for successful responses.
- The outer `status` wrapper does not add meaningful type value for successful
  client calls.
- Validation and domain failures still need explicit shared documentation
  because they influence error handling.

Alternatives considered:

- Model all success responses as full JSend envelopes: rejected because it makes
  generated client usage noisier and less aligned with consumer intent.
- Hide JSend entirely, including failures: rejected because fail/error shapes
  are contractually important for frontend error handling.

### 3. Annotation work stays close to existing web and shared layers

Swagger/OpenAPI metadata will be added directly to:

- public customer controllers,
- request records in `infrastructure/web/request`,
- response records in `application/response`, and
- shared web payload records/enums in `shared/infrastructure/web` and pagination
  wrappers in `shared/domain`.

Why this choice:

- It keeps documentation adjacent to the types that actually define the API
  contract.
- It avoids introducing parallel documentation-only DTOs or adapter layers.
- It preserves the current clean architecture boundaries because only web-facing
  and shared boundary types are annotated.

Alternatives considered:

- Introduce separate OpenAPI-only schema classes: rejected because it duplicates
  boundary models and creates new drift risk.

### 4. Pagination is documented explicitly by transport style

Offset-based list endpoints keep `PageResponse<T>` and page/size query
parameters. Cursor-based search endpoints keep `SliceResponse<T>` and
cursor/size query parameters. Their descriptions and response schemas must make
the difference explicit.

Why this choice:

- The codebase already implements both styles.
- Consumers need to know when to send `page` versus `cursor`.
- Generated types should preserve the distinct response metadata fields.

Alternatives considered:

- Normalize everything to one pagination model: rejected because it would
  require behavioral changes outside the contract-standardization scope.

### 5. Internal-only endpoints remain excluded from the customer contract

Stripe webhook endpoints and SSE endpoints stay out of the generated customer
contract.

Why this choice:

- They are not frontend-consumed customer APIs.
- Including them pollutes the SDK surface and introduces irrelevant schemas.

Alternatives considered:

- Include them and mark them internal: rejected because the generated customer
  SDK should expose only consumer-relevant endpoints.

### 6. Sensitive fields are documented conservatively

Passwords, tokens, personal identifiers, addresses, and audit-heavy values use
conservative schema metadata such as `writeOnly`, omitted/placeholder examples,
and minimal descriptive exposure.

Why this choice:

- The frontend requested extra care for sensitive values.
- OpenAPI examples should not normalize leaking secrets or high-sensitivity
  identifiers.

Alternatives considered:

- Provide realistic examples for all fields: rejected for privacy and security
  reasons.

## Risks / Trade-offs

- [Generated contract still misrepresents wrapped success payloads] ->
  Mitigation: add a focused springdoc customization layer and verify generated
  output against representative endpoints before mirroring YAML.
- [Large annotation pass introduces inconsistency across controllers and DTOs]
  -> Mitigation: standardize a shared pattern for parameters, success responses,
  failure responses, and sensitive-field schema metadata.
- [Mirrored `shared/api-contracts/openapi.yaml` becomes stale again] ->
  Mitigation: define an explicit regeneration workflow tied to backend contract
  changes and heyapi generation.
- [Springdoc generic handling for pagination remains ambiguous] -> Mitigation:
  explicitly annotate paginated endpoints and shared pagination wrappers rather
  than relying on default generic inference.
- [Global security configuration accidentally marks public auth endpoints as
  protected] -> Mitigation: override security metadata on public auth operations
  and verify generated security sections.

## Migration Plan

1. Expand springdoc configuration and shared schema annotations in the backend.
2. Annotate public customer controllers and API-facing DTOs.
3. Generate and inspect the backend OpenAPI document.
4. Mirror the generated contract to `shared/api-contracts/openapi.yaml`.
5. Point heyapi generation to the backend-generated contract and validate the
   resulting SDK surface.

Rollback strategy:

- Revert the annotation/configuration change set and restore the previous
  checked-in YAML if the generated contract is not acceptable.
- Because runtime business behavior is not being changed, rollback is primarily
  a source-control rollback of documentation/configuration changes.

## Open Questions

- None at proposal time; the contract direction, SDK target, pagination
  treatment, and endpoint scope were all decided during exploration.

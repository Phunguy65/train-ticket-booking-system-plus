# Tasks

## 1. Springdoc contract foundation

-  [x] 1.1 Update backend springdoc configuration so the customer API contract is
      generated from backend code with the correct public-doc path and endpoint
      exclusions.
-  [x] 1.2 Extend shared OpenAPI configuration to declare customer API metadata,
      reusable error schemas, pagination schema descriptions, and
      public-vs-protected security metadata.
-  [x] 1.3 Add a springdoc customization layer for success-response schema
      unwrapping so generated 2xx payloads expose the business payload shape
      required by heyapi.

## 2. Controller contract annotations

-  [x] 2.1 Standardize Swagger/OpenAPI annotations across public auth and booking
      controllers, including operation IDs, parameter metadata, request-body
      metadata, and authenticated vs unauthenticated operations.
-  [x] 2.2 Standardize Swagger/OpenAPI annotations across train, scheduled-trip,
      coach, seat, route-template, station, and payment controllers, including
      paginated endpoint response metadata.
-  [x] 2.3 Verify internal-only endpoints such as Stripe webhooks and SSE streams
      remain outside the generated customer contract surface.

## 3. Shared request and response schema annotations

-  [x] 3.1 Annotate public request records in backend web layers with
      descriptions, validation-aligned field metadata, pagination semantics, and
      conservative handling for sensitive inputs.
-  [x] 3.2 Annotate public response records and nested response types with
      accurate field descriptions, enum visibility, UUID/date formats,
      pagination wrappers, and conservative handling for sensitive outputs.
-  [x] 3.3 Annotate shared web payload records and enums such as `FailData`,
      `Violation`, `ErrorCode`, `ViolationCode`, `PageResponse`, and
      `SliceResponse` so shared success/failure contracts generate consistently.

## 4. Generated artifact and SDK workflow

-  [x] 4.1 Generate and validate the backend OpenAPI document against
      representative customer endpoints, ensuring success payloads, failure
      envelopes, pagination, and security metadata match the agreed contract.
-  [x] 4.2 Mirror the generated customer contract to
      `shared/api-contracts/openapi.yaml` and remove outdated handwritten
      mismatches.
-  [x] 4.3 Add or update the heyapi generation workflow for the fetch-based
      frontend SDK so it consumes the backend-generated customer contract.

## 2026-04-05 Round 1 (from spx-apply auto-verify)

### spx-verifier

- Fixed: corrected `SuccessResponseKind.ARRAY` handling in
  `JsendSuccessResponseCustomizer` so array success payloads do not generate
  array-of-array schemas.
- Fixed: registered shared `FailData`, `Violation`, `ErrorCode`,
  `ViolationCode`, `PageResponse`, and `SliceResponse` schemas in
  `OpenApiConfig` so shared component references are available to the generated
  contract.
- Fixed: added a reusable `JsendSuccessResponse` component and explicit
  pagination wrapper descriptions in `OpenApiConfig` to complete the group 1
  contract foundation.

### spx-arch-verifier

- Fixed: switched success payload schema resolution to `resolveAsRef(true)` and
  documented referenced-schema accumulation in `JsendSuccessResponseCustomizer`
  so generated response schemas stay reusable and the singleton customizer state
  is intentional.

### spx-test-verifier

- Fixed: added `OpenApiConfigTest` and `JsendSuccessResponseCustomizerTest` to
  cover the new OpenAPI foundation wiring, shared schema registration, response
  unwrapping behavior, and `SuccessPayload` defaults.

## 2026-04-05 Round 2 (from spx-apply milestone verify)

### spx-arch-verifier

- Fixed: hid `SeatEventController` and `StripeWebhookController` with `@Hidden`,
  broadened the customer `GroupedOpenApi` webhook exclusion pattern, and marked
  task `2.3` complete so internal SSE and webhook endpoints stay out of the
  customer contract surface.
- Fixed: added explicit `@SecurityRequirement(name = "bearerAuth")` and
  `@Parameter(hidden = true)` on authenticated auth, booking, and payment
  controller methods so per-operation security metadata is explicit and injected
  authentication objects are not exposed as request parameters.

### spx-test-verifier

- Fixed: added `ControllerContractAnnotationsTest` to verify controller-level
  `@SuccessPayload`, security requirements, hidden internal controllers, and
  shared `Trains` tagging across the customer contract surface.
- Fixed: updated `OpenApiConfigTest` to cover the broadened customer webhook
  exclusion pattern.

## 2026-04-05 Round 3 (from spx-apply milestone re-verify)

### spx-test-verifier

- Fixed: expanded `ControllerContractAnnotationsTest` to verify every public
  controller method declares `@Operation`, `@ApiResponses`, `@SuccessPayload`,
  explicit security requirements, hidden injected authentication parameters, and
  documented path variables.
- Fixed: added taxonomy and pagination-kind coverage in
  `ControllerContractAnnotationsTest` so the shared tag set and PAGE/SLICE/ARRAY
  endpoint semantics are verified across the full customer controller surface.

## 2026-04-05 Round 4 (from spx-apply group 3 verify)

### spx-arch-verifier

- Fixed: added missing examples and conservative sensitive-value examples in
  `SearchScheduledTripsRequest`, `UserResponse`, `PassengerInfoResponse`, and
  `ScheduledTripResponse` so request and response schemas align with the
  design's pagination and sensitive-field guidance.

### spx-test-verifier

- Fixed: added `DtoSchemaAnnotationsTest` to verify request DTO, response DTO,
  and shared payload field annotations, including `writeOnly`, `READ_ONLY`,
  UUID/date formats, and array-schema coverage.
- Fixed: updated `DtoSchemaAnnotationsTest` to inspect generated record fields
  instead of `RecordComponent` annotations so the schema assertions match how
  record annotations are emitted at runtime.

## 2026-04-05 Round 5 (from spx-apply group 3 re-verify)

### spx-test-verifier

- Fixed: expanded `DtoSchemaAnnotationsTest` to validate UUID/date formats
  across all response and nested response records that expose those field types.
- Fixed: added pagination constraint and cursor-semantic assertions in
  `DtoSchemaAnnotationsTest` so offset-based and cursor-based request metadata
  stays aligned with the generated contract requirements.

## 2026-04-05 Round 6 (from spx-apply final auto-verify)

### spx-arch-verifier

- Fixed: referenced `ErrorCode` and `ViolationCode` from `FailData` and
  `Violation` so the mirrored contract keeps named enum component schemas
  instead of anonymous inline unions, and regenerated the customer SDK from the
  updated contract.

### spx-test-verifier

- Fixed: replaced ad-hoc `IllegalStateException` assertions in
  `CustomerOpenApiContractSupport` with AssertJ-backed checks so contract test
  failures report clean assertion messages.
- Fixed: added `WebConfigTest` to cover `/api` path-prefix targeting and the
  selective API-version resolver behavior for versioned, springdoc, and
  non-versioned internal paths.
- Fixed: added `CustomerOpenApiExporterTest` and refactored
  `CustomerOpenApiExporter` artifact writing so the mirrored YAML/debug JSON
  file outputs are unit-tested.
- Fixed: strengthened `CustomerOpenApiContractTest` YAML assertions to parse the
  serialized contract and verify core OpenAPI structure instead of relying only
  on substring checks.

## 2026-04-05 Round 7 (from spx-apply final re-verify)

### spx-arch-verifier

- Fixed: changed `UpdateAuthenticatedUserRequest.addressLine` to use a
  non-numeric redacted example so the generated mirrored contract keeps a
  string-valued example for the `JsonNullableString` schema.

### spx-test-verifier

- Fixed: added `JsendSuccessResponseCustomizerTest` coverage for automatic 500
  response injection, missing `@SuccessPayload`, null-response operations, and
  preserving pre-existing 500 responses.
- Fixed: replaced fragile substring-based `$ref` checks in
  `CustomerOpenApiContractSupport` with structured JSON traversal plus prefix
  matching for generated generic schema families.
- Fixed: tightened `DtoSchemaAnnotationsTest` around patch-request examples and
  explicit pagination/cursor constraint annotations so schema metadata failures
  are caught directly.

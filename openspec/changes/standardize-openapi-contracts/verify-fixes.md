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

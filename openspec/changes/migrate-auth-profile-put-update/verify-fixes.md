## [2026-04-16] Round 1 (from spx-apply auto-verify)

### spx-verifier

- Fixed: updated the authenticated profile update OpenAPI metadata in
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/infrastructure/web/AuthController.java`
  to describe full-replacement semantics and document the preserved `409`
  duplicate-email response.

### spx-arch-verifier

- Fixed: aligned
  `backend/src/main/java/io/github/phunguy65/ttbs/backend/user/infrastructure/web/request/UpdateAuthenticatedUserRequest.java`
  validation messages with existing auth request DTO conventions and rejected
  blank optional string values at the request boundary.

### spx-test-verifier

- Fixed: strengthened
  `backend/src/test/java/io/github/phunguy65/ttbs/backend/shared/infrastructure/web/CustomerOpenApiContractSupport.java`
  so representative contract verification now asserts `updateAuthenticatedUser`
  is exposed as `PUT` and still advertises failure responses.

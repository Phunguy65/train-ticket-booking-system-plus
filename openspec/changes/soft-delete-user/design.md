# Context

The `users` table in PostgreSQL already has a `deleted_at TIMESTAMPTZ` column with supporting indexes (`uq_users_email_active` partial unique index, `idx_users_deleted_at` partial index). The codebase comment explicitly documents "Soft delete via `deleted_at TIMESTAMPTZ` (NULL = active)… manual `WHERE deleted_at IS NULL` filtering in application queries". The `User` domain model and `UserEntity` JPA entity do not yet expose this column. No delete endpoint or use case exists.

The `user` module follows Vertical Slice / Clean Architecture inside a Spring Modulith boundary. Existing patterns to follow:
- `RefreshTokenEntity` uses manual `@Query` filtering (`revokedAt IS NULL`) — same pattern for `deletedAt IS NULL`
- `CancelBookingUseCase` shows the idempotent, domain-method-driven, event-publishing use case shape
- `Result<T, E>` monad throughout; `JsendResponse` for HTTP responses; `@PreAuthorize` for method-level security

## Goals / Non-Goals

**Goals:**
- Expose `deleted_at` in the `User` domain model and `UserEntity` with full mapper support
- Fix all existing JPA queries to exclude soft-deleted users (`findByEmail`, `findById`, `findAll`)
- Fix `UserDetailsServiceImpl` so soft-deleted users cannot authenticate after deletion
- Implement `SoftDeleteUserUseCase` (single) and `BulkSoftDeleteUsersUseCase` (batch)
- Add REST endpoints: `DELETE /api/v1/users/me`, `DELETE /api/v1/users/{id}`, `DELETE /api/v1/users`
- Revoke all active refresh tokens for deleted user(s) within the same transaction
- Publish `UserDeleted` domain event after successful soft delete

**Non-Goals:**
- Hard delete / permanent removal of user records
- GDPR anonymization / data retention policies (future concern)
- Restoring / undeleting users (future admin feature)
- Cascading soft-delete to bookings or other child entities (booking records are financial history — retained as-is)
- Frontend UI changes (out of scope for this backend change)

## Decisions

### Decision 1: Manual `WHERE deleted_at IS NULL` in each query (not `@SQLDelete` / `@Where`)

The codebase explicitly documents this as the chosen pattern (matching `refresh_tokens.revoked_at`). Using Hibernate's `@SQLDelete` and `@Where(clause = "deleted_at IS NULL")` would be an inconsistent second approach. Manual `@Query` annotations are explicit, easy to audit, and ArchUnit-safe.

**Alternative considered**: Hibernate `@SQLRestriction("deleted_at IS NULL")` (Hibernate 6+) — cleaner but invisible; developers might add queries without realising the global filter is there, leading to surprises when they need to query deleted records. Rejected for lack of transparency.

### Decision 2: `softDelete()` as a domain method on `User` aggregate

Keeps business rule ("a user can only be deleted once, and deletion sets `deletedAt`") inside the domain. Follows the `Booking.cancel()` pattern. The use case calls `user.softDelete()`, which returns `Result<Void, UserError>` and registers a `UserDeleted` domain event.

**Alternative considered**: Set `deletedAt` directly in the use case — rejected because it leaks domain logic out of the aggregate.

### Decision 3: Idempotent delete (calling delete on an already-deleted user returns success)

Matches `CancelBookingUseCase` behaviour. This simplifies retry logic and avoids spurious 409/404 errors in bulk operations where some IDs may already be deleted.

### Decision 4: Bulk delete uses a single `UPDATE … WHERE id IN (?)` JPQL query

Avoids N database round-trips. The entire batch runs inside a single `@Transactional` boundary — all succeed or all roll back. Partial-success (HTTP 207) is deliberately excluded to keep the semantics simple: either the batch is committed or it isn't.

**Alternative considered**: Loop over IDs, calling single-delete use case per ID — rejected for performance and inconsistent transaction semantics.

### Decision 5: Admin cannot delete themselves via the bulk endpoint; allowed via single endpoint with confirmation

For the single `DELETE /api/v1/users/{id}` endpoint, an admin may delete themselves (account closure). For bulk `DELETE /api/v1/users`, the request is rejected if the calling admin's own ID appears in the list — this protects against accidental self-removal in a mass operation.

### Decision 6: Refresh-token revocation inside the delete transaction

`RefreshTokenJpaRepository.revokeAllByUserId()` already exists and runs a single `UPDATE`. Calling it inside the same `@Transactional` use case ensures atomicity: if the user soft-delete fails (constraint, optimistic lock), tokens are not revoked, and vice-versa.

### Decision 7: `DELETE /api/v1/users` with a JSON body for bulk delete

`DELETE` with a request body is unconventional but widely accepted for bulk operations. Using query-string IDs (`?ids=…`) risks hitting URL length limits for large batches. A dedicated `/users/bulk` sub-path was considered but adds unnecessary nesting. The plain `DELETE /api/v1/users` + body is consistent with the existing pattern where `POST /api/v1/users` creates a single user and `GET /api/v1/users` lists them.

## Risks / Trade-offs

- **[Risk] Existing tokens remain valid after soft delete until expiry** — Mitigation: revoking all refresh tokens forces clients to re-authenticate at next token refresh, which will fail because the user is now excluded from `UserDetailsService`. Short-lived access tokens (JWT, typically 15 min) will continue to work until expiry. This is an acceptable trade-off; forced immediate invalidation would require a token blacklist (out of scope).

- **[Risk] Queries added in the future without `deletedAt IS NULL`** — Mitigation: add a `@DataJpaTest` that asserts soft-deleted users are not returned by any query method in `UserJpaRepository`.

- **[Risk] Bulk delete of a large number of IDs (>1000)** — Mitigation: apply Jakarta `@Size(max = 100)` validation on the bulk request DTO to cap the batch size. Large-scale removals can be handled via multiple requests.

- **[Risk] Admin accidentally bulk-deletes themselves** — Mitigation: controller checks that `currentUserId` is not in the submitted ID list before forwarding to the use case.

## Migration Plan

No database migration is required — `deleted_at` column and indexes already exist.

Deployment is backward-compatible:
1. Deploy the updated backend — existing active users are unaffected (all have `deleted_at = NULL`).
2. Existing `GET /api/v1/users/{id}` and list endpoints gain the `deletedAt IS NULL` filter transparently.
3. No data backfill needed.

Rollback: revert the deployment. No data is lost because soft delete only sets `deleted_at`; a rollback will cause deleted users to reappear in queries, which is acceptable for a short rollback window.

## Open Questions

- Should a self-deleted user receive a confirmation email? (Requires notification capability — not currently in scope; flag for future.)
- Should the `UserDto` / `UserHttpResponse` expose `deletedAt` to admin callers in list/get endpoints? (Currently excluded; admin visibility could be added as a follow-up.)

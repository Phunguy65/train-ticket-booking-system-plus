# Why

The system currently has no way to remove user accounts. The database schema already includes a `deleted_at` column on the `users` table, but no application-level logic exists to set it. Users need the ability to close their own accounts, and admins need the ability to remove any user — including bulk removal for moderation or data cleanup purposes. Implementing soft delete (rather than hard delete) preserves data integrity and satisfies audit/compliance requirements.

## What Changes

- **New endpoint**: `DELETE /api/v1/users/me` — allows an authenticated user to soft-delete their own account
- **New endpoint**: `DELETE /api/v1/users/{id}` — allows an admin to soft-delete any single user account
- **New endpoint**: `DELETE /api/v1/users` (body: `{ "userIds": [...] }`) — allows an admin to soft-delete multiple users in one request
- **Domain model update**: `User` aggregate gains `deletedAt` field, `softDelete()` method, and `isDeleted()` helper
- **Infrastructure update**: All JPA queries on `users` table gain `WHERE deleted_at IS NULL` filtering; two new soft-delete JPQL mutation queries added
- **Security fix**: `UserDetailsServiceImpl` must exclude soft-deleted users so they cannot authenticate after deletion
- **Cascade**: all active refresh tokens for a deleted user are revoked within the same transaction
- **Domain event**: `UserDeleted` event is published after a successful soft delete
- **Error type**: `UserError.UserAlreadyDeleted` added (delete is idempotent — calling it again returns success, not an error)

## Capabilities

### New Capabilities

- `user-soft-delete`: Soft-delete a single user (self or admin) and bulk-soft-delete multiple users (admin only), with token revocation and domain event publication

### Modified Capabilities

- `user-get`: Soft-deleted users MUST be excluded from all retrieval endpoints (`GET /api/v1/users/{id}`, `GET /api/v1/users/me`, `GET /api/v1/users`). Currently these endpoints have no such filtering and would return deleted user profiles.

## Impact

- **`user` module** (all layers): domain model, JPA entity, repository adapter, JPA repository queries, use cases, controller, security
- **`users` table**: no schema migration needed — `deleted_at` column and indexes already exist in `B11_0_0__baseline.sql`
- **Auth flow**: soft-deleted users are blocked from logging in and their existing JWT sessions are invalidated via refresh-token revocation
- **No other modules affected**: bookings and other entities reference `userId` by value (UUID) and are not cascaded

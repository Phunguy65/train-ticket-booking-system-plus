# Tasks

## 1. Domain Layer — User Aggregate

- [x] 1.1 Add `deletedAt` field (`Instant`, nullable) to `User` domain model and update `reconstitute()` factory method to accept it
- [x] 1.2 Add `softDelete()` method to `User` that sets `deletedAt = Instant.now()`, registers `UserDeleted` domain event, and returns `Result<Void, UserError>`
- [x] 1.3 Add `isDeleted()` helper method to `User` (`return deletedAt != null`)
- [x] 1.4 Create `UserDeleted` domain event class in `user/domain/event/`
- [x] 1.5 Add `UserAlreadyDeleted` error case to the `UserError` sealed interface

## 2. Infrastructure — Persistence Layer

- [x] 2.1 Add `deletedAt` field (`Instant`, nullable, `@Column(name = "deleted_at")`) to `UserEntity` JPA entity
- [x] 2.2 Update `UserEntityMapper.toDomain()` to map `deletedAt` from entity → domain model
- [x] 2.3 Update `UserEntityMapper.toEntity()` to map `deletedAt` from domain model → entity
- [x] 2.4 Rewrite `UserJpaRepository.findByEmail()` with explicit `@Query` adding `AND u.deletedAt IS NULL`
- [x] 2.5 Rewrite `UserJpaRepository.findById()` (or override via `@Query`) to add `AND u.deletedAt IS NULL`
- [x] 2.6 Update `UserJpaRepository.findAll()` / paginated list query to filter `WHERE u.deletedAt IS NULL`
- [x] 2.7 Add `@Modifying @Query` method `softDeleteById(UUID id, Instant deletedAt)` to `UserJpaRepository`
- [x] 2.8 Add `@Modifying @Query` method `softDeleteByIds(List<UUID> ids, Instant deletedAt)` to `UserJpaRepository`
- [x] 2.9 Add `softDeleteById(UserId id, Instant deletedAt)` and `softDeleteByIds(List<UserId> ids, Instant deletedAt)` to `UserRepository` domain port interface
- [x] 2.10 Implement the new repository port methods in `UserRepositoryAdapter`, delegating to the JPA repository

## 3. Infrastructure — Security Fix

- [x] 3.1 Update `UserDetailsServiceImpl.loadUserByUsername()` to return `null` / throw `UsernameNotFoundException` for users whose `deletedAt` is set (rely on the updated `findById` query that filters deleted users — verify no extra code needed)

## 4. Application Layer — Single Delete Use Case

- [x] 4.1 Create `SoftDeleteUserCommand` record in `user/application/command/` (field: `UserId userId`)
- [x] 4.2 Create `SoftDeleteUserUseCase` in `user/application/usecase/`:
  - Annotate with `@Service @Transactional`
  - Load user by ID; return `UserError.UserNotFound` if absent
  - Return success immediately if `user.isDeleted()` (idempotent)
  - Call `user.softDelete()`; propagate any failure
  - Call `refreshTokenRepository.revokeAllByUserId(userId, Instant.now())`
  - Save user via `userRepository.save(user)`
  - Publish domain events via Spring's `ApplicationEventPublisher`

## 5. Application Layer — Bulk Delete Use Case

- [x] 5.1 Create `BulkSoftDeleteUsersCommand` record in `user/application/command/` (field: `List<UserId> userIds`)
- [x] 5.2 Create `BulkSoftDeleteUsersUseCase` in `user/application/usecase/`:
  - Annotate with `@Service @Transactional`
  - Call `userRepository.softDeleteByIds(userIds, Instant.now())`
  - Call `refreshTokenRepository.revokeAllByUserIds(userIds, Instant.now())` (add this method if missing)
  - Publish a `UserDeleted` event per affected user ID
  - Return the count of affected rows

## 6. Web Layer — REST Endpoints

- [x] 6.1 Create `BulkSoftDeleteUsersHttpRequest` record in `user/infrastructure/web/` with field `@NotEmpty @Size(max=100) List<@NotNull UUID> userIds`
- [x] 6.2 Add `DELETE /me` endpoint to `UserController` (no `@PreAuthorize`, authenticated only): extract caller UUID from `SecurityContextHolder`, delegate to `SoftDeleteUserUseCase`
- [x] 6.3 Add `DELETE /{id}` endpoint to `UserController` with `@PreAuthorize("hasRole('ADMIN')")`: delegate to `SoftDeleteUserUseCase`
- [x] 6.4 Add `DELETE /` (bulk) endpoint to `UserController` with `@PreAuthorize("hasRole('ADMIN')")`: validate that caller's UUID is not in the list, then delegate to `BulkSoftDeleteUsersUseCase`; return `{ "deletedCount": N }` on success
- [x] 6.5 Add `USER_CANNOT_BULK_DELETE_SELF` to `ErrorCode` enum (used when admin includes own ID in bulk list)
- [x] 6.6 Update `UserController.errorResponse()` to handle `UserError.UserAlreadyDeleted` (map to `200 OK` — idempotent success, not an error) and `USER_CANNOT_BULK_DELETE_SELF` (map to `400 Bad Request`)
- [x] 6.7 Update `SecurityConfig` to permit `DELETE /api/*/users` and `DELETE /api/*/users/**` for authenticated users (fine-grained auth handled by `@PreAuthorize` at method level)

## 7. Refresh Token — Bulk Revocation Support

- [x] 7.1 Check if `RefreshTokenJpaRepository` has a bulk revoke method accepting multiple user IDs; add `revokeAllByUserIdIn(List<UUID> userIds, Instant revokedAt)` if missing
- [x] 7.2 Expose the bulk revoke method through the corresponding repository port / adapter so `BulkSoftDeleteUsersUseCase` can call it

## 8. Tests

- [x] 8.1 Add unit tests to `UserTest` (domain): `softDelete()` sets `deletedAt`; second call is idempotent; `isDeleted()` reflects state; `UserDeleted` event is registered
- [x] 8.2 Add `@DataJpaTest` to `UserRepositoryAdapterTest`: verify `findByEmail`, `findById`, and `findAll` do NOT return soft-deleted users; verify `softDeleteById` sets `deletedAt`; verify `softDeleteByIds` bulk-updates correctly
- [x] 8.3 Add unit tests to `SoftDeleteUserUseCaseTest` (`@ExtendWith(MockitoExtension)`): user not found → `UserNotFound`; active user → deleted + tokens revoked + event published; already-deleted user → idempotent success
- [x] 8.4 Add unit tests to `BulkSoftDeleteUsersUseCaseTest`: happy path → correct count returned + tokens revoked; empty list handled (validation prevents reaching use case)
- [x] 8.5 Add `@WebMvcTest` to `UserControllerTest`: `DELETE /me` returns `200`; `DELETE /{id}` by admin returns `200`; non-admin `DELETE /{id}` returns `403`; bulk delete by admin returns `200` with `deletedCount`; bulk with self ID returns `400`; bulk >100 IDs returns `400`
- [x] 8.6 Add security test: soft-deleted user cannot load via `UserDetailsService` (returns `UsernameNotFoundException`)

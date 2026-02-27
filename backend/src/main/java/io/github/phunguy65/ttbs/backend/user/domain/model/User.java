package io.github.phunguy65.ttbs.backend.user.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import java.time.Instant;

public class User extends AggregateRoot<UserId> {

    private final UserId id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final String phone;
    private final UserRole role;
    private final Instant createdAt;
    private final Instant updatedAt;
    private Instant deletedAt;

    private User(
            UserId id,
            String email,
            String passwordHash,
            String fullName,
            String phone,
            UserRole role,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new user account. Registers {@link UserRegistered} domain event.
     */
    public static User create(
            UserId id, String email, String passwordHash, String fullName, String phone) {
        Instant now = Instant.now();
        User user = new User(
                id,
                email.toLowerCase().trim(),
                passwordHash,
                fullName,
                phone,
                UserRole.CUSTOMER,
                now,
                now,
                null);
        user.registerEvent(UserRegistered.of(id, email));
        return user;
    }

    /**
     * Factory method for reconstituting a user from persistence.
     * Does NOT register domain events.
     */
    public static User reconstitute(
            UserId id,
            String email,
            String passwordHash,
            String fullName,
            String phone,
            UserRole role,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        return new User(
                id, email, passwordHash, fullName, phone, role, createdAt, updatedAt, deletedAt);
    }

    /**
     * Soft-deletes this user by setting {@code deletedAt} to now and registering a
     * {@link UserDeleted} domain event. Idempotent: if the user is already deleted,
     * returns success immediately without modifying state or registering a second event.
     *
     * @return {@link Result#success()} or {@link Result#failure} with {@link UserError.UserAlreadyDeleted}
     *         (currently always succeeds; the error variant is reserved for future guard logic)
     */
    public Result<Void, UserError> softDelete() {
        if (isDeleted()) {
            return Result.success();
        }
        this.deletedAt = Instant.now();
        registerEvent(UserDeleted.of(id));
        return Result.success();
    }

    /** Returns {@code true} if this user has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public UserId getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}

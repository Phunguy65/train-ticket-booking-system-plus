package io.github.phunguy65.ttbs.backend.user.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
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

    private User(
            UserId id,
            String email,
            String passwordHash,
            String fullName,
            String phone,
            UserRole role,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
                now);
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
            Instant updatedAt) {
        return new User(id, email, passwordHash, fullName, phone, role, createdAt, updatedAt);
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
}

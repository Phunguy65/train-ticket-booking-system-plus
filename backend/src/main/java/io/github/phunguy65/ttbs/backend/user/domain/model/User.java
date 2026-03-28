package io.github.phunguy65.ttbs.backend.user.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class User extends AggregateRoot<UserId> {

    private final UserId id;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final PersonName fullName;
    private final PhoneNumber phone;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final IdDocumentNumber idDocumentNumber;
    private final AddressLine addressLine;
    private final UserRole role;
    private final Instant createdAt;
    private final Instant updatedAt;
    private Instant deletedAt;

    private User(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            PersonName fullName,
            PhoneNumber phone,
            LocalDate dateOfBirth,
            Gender gender,
            IdDocumentNumber idDocumentNumber,
            AddressLine addressLine,
            UserRole role,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.idDocumentNumber = idDocumentNumber;
        this.addressLine = addressLine;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new user account. Registers {@link UserRegistered} domain event.
     */
    public static User create(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            PersonName fullName,
            PhoneNumber phone,
            LocalDate dateOfBirth,
            Gender gender,
            IdDocumentNumber idDocumentNumber,
            AddressLine addressLine) {
        Instant now = Instant.now();
        User user = new User(
                id,
                email,
                passwordHash,
                fullName,
                phone,
                dateOfBirth,
                gender,
                idDocumentNumber,
                addressLine,
                UserRole.CUSTOMER,
                now,
                now,
                null);
        user.registerEvent(UserRegistered.of(id, email.value()));
        return user;
    }

    /**
     * Factory method for reconstituting a user from persistence.
     * Does NOT register domain events.
     */
    public static User reconstitute(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            PersonName fullName,
            PhoneNumber phone,
            LocalDate dateOfBirth,
            Gender gender,
            IdDocumentNumber idDocumentNumber,
            AddressLine addressLine,
            UserRole role,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        return new User(
                id,
                email,
                passwordHash,
                fullName,
                phone,
                dateOfBirth,
                gender,
                idDocumentNumber,
                addressLine,
                role,
                createdAt,
                updatedAt,
                deletedAt);
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

    public EmailAddress getEmail() {
        return email;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public PersonName getFullName() {
        return fullName;
    }

    public Optional<PhoneNumber> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Optional<LocalDate> getDateOfBirth() {
        return Optional.ofNullable(dateOfBirth);
    }

    public Optional<Gender> getGender() {
        return Optional.ofNullable(gender);
    }

    public Optional<IdDocumentNumber> getIdDocumentNumber() {
        return Optional.ofNullable(idDocumentNumber);
    }

    public Optional<AddressLine> getAddressLine() {
        return Optional.ofNullable(addressLine);
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

package io.github.phunguy65.ttbs.backend.user.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD_HASH = "$2a$12$exampleHashedPassword";
    private static final String FULL_NAME = "Alice Nguyen";
    private static final String PHONE = "0901234567";

    @Test
    void create_shouldSetRoleToCustomer() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void create_shouldNormalizeEmailToLowercase() {
        User user = User.create(USER_ID, "ALICE@EXAMPLE.COM", PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void create_shouldStorePasswordHashNotPlainText() {
        String plainPassword = "myplainpassword";
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.getPasswordHash()).isNotEqualTo(plainPassword);
    }

    @Test
    void create_shouldRegisterUserRegisteredEvent() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.getDomainEvents()).hasSize(1);
        assertThat(user.getDomainEvents().getFirst()).isInstanceOf(UserRegistered.class);
        UserRegistered event = (UserRegistered) user.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL.toLowerCase());
    }

    @Test
    void create_shouldSetCorrectFields() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getFullName()).isEqualTo(FULL_NAME);
        assertThat(user.getPhone()).isEqualTo(PHONE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        User user = User.reconstitute(
                USER_ID,
                EMAIL,
                PASSWORD_HASH,
                FULL_NAME,
                PHONE,
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);

        assertThat(user.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-16T12:00:00Z");

        User user = User.reconstitute(
                USER_ID,
                EMAIL,
                PASSWORD_HASH,
                FULL_NAME,
                PHONE,
                UserRole.ADMIN,
                createdAt,
                updatedAt,
                null);

        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    // ── softDelete() ─────────────────────────────────────────────────────────

    @Test
    void softDelete_shouldSetDeletedAt() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);
        user.clearDomainEvents();

        Result<Void, ?> result = user.softDelete();

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void softDelete_shouldRegisterUserDeletedEvent() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);
        user.clearDomainEvents();

        user.softDelete();

        assertThat(user.getDomainEvents()).hasSize(1);
        assertThat(user.getDomainEvents().getFirst()).isInstanceOf(UserDeleted.class);
        UserDeleted event = (UserDeleted) user.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void softDelete_secondCall_isIdempotent() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);
        user.clearDomainEvents();
        user.softDelete();
        Instant firstDeletedAt = user.getDeletedAt();
        user.clearDomainEvents();

        Result<Void, ?> result = user.softDelete();

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(user.getDomainEvents()).isEmpty();
    }

    @Test
    void isDeleted_activeUser_returnsFalse() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);

        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_afterSoftDelete_returnsTrue() {
        User user = User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE);
        user.softDelete();

        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    void reconstitute_withDeletedAt_shouldRestoreDeletedAt() {
        Instant deletedAt = Instant.parse("2024-06-01T08:00:00Z");

        User user = User.reconstitute(
                USER_ID,
                EMAIL,
                PASSWORD_HASH,
                FULL_NAME,
                PHONE,
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                deletedAt);

        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(user.isDeleted()).isTrue();
    }
}

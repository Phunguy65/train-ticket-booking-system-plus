package io.github.phunguy65.ttbs.backend.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("User")
class UserTest {

    private static final UserId USER_ID =
            UserId.of(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
    private static final EmailAddress EMAIL = EmailAddress.of("user@example.com");
    private static final PasswordHash PASSWORD_HASH = PasswordHash.of("hashed-secret");
    private static final PersonName FULL_NAME = PersonName.of("Nguyen Van A");

    private static User createUser() {
        return User.create(USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, null, null, null, null, null);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("sets role CUSTOMER and registers UserRegistered event")
        void create_setsRoleCustomerAndRegistersUserRegisteredEvent() {
            User user = createUser();

            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(user.getDomainEvents()).hasSize(1);
            assertThat(user.getDomainEvents().get(0)).isInstanceOf(UserRegistered.class);
        }

        @Test
        @DisplayName("publishes UserRegistered with the created user id and email")
        void create_registersUserRegisteredEventWithExpectedValues() {
            User user = createUser();

            UserRegistered event = (UserRegistered) user.getDomainEvents().getFirst();

            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.email()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("sets deletedAt and registers UserDeleted event")
        void softDelete_setsDeletedAtAndRegistersUserDeletedEvent() {
            User user = createUser();
            user.clearDomainEvents();

            Result<Void, UserError> result = user.softDelete();

            assertThat(result.isSuccess()).isTrue();
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getDomainEvents()).hasSize(1);
            assertThat(user.getDomainEvents().get(0)).isInstanceOf(UserDeleted.class);
        }

        @Test
        @DisplayName("is idempotent — already deleted returns success with no second event")
        void softDelete_isIdempotent_noSecondEvent() {
            User user = createUser();
            user.softDelete();
            user.clearDomainEvents();

            Result<Void, UserError> result = user.softDelete();

            assertThat(result.isSuccess()).isTrue();
            assertThat(user.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("does not register events")
        void reconstitute_doesNotRegisterEvents() {
            User user = User.reconstitute(
                    USER_ID,
                    EMAIL,
                    PASSWORD_HASH,
                    FULL_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    UserRole.CUSTOMER,
                    Instant.now(),
                    Instant.now(),
                    null);

            assertThat(user.getDomainEvents()).isEmpty();
        }
    }
}

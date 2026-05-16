package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAuthenticatedUserUseCase")
class UpdateAuthenticatedUserUseCaseTest {

    private static final UUID USER_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UserId USER_ID = UserId.of(USER_UUID);
    private static final Instant CREATED_AT = Instant.parse("2026-05-15T10:15:30Z");
    private static final Instant ORIGINAL_UPDATED_AT = Instant.parse("2026-05-15T11:15:30Z");
    private static final Instant DELETED_AT = Instant.parse("2026-05-16T11:15:30Z");

    @Mock
    private UserRepository userRepository;

    private UpdateAuthenticatedUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAuthenticatedUserUseCase(userRepository);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("updates user and returns UserResponse when email unchanged")
        void execute_updatesUserAndReturnsUserResponseWhenEmailUnchanged() {
            User existing = existingUser("customer@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Result<UserResponse, UserError> result =
                    useCase.execute(command("customer@example.com"));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.email()).isEqualTo("customer@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyen Van B");
        }

        @Test
        @DisplayName("updates user and returns UserResponse when email changed to available one")
        void execute_updatesUserAndReturnsUserResponseWhenEmailChangedToAvailableOne() {
            User existing = existingUser("customer@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Result<UserResponse, UserError> result =
                    useCase.execute(command("updated@example.com"));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.email()).isEqualTo("updated@example.com");
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("calls findById with exact UserId from command")
        void execute_callsFindByIdWithExactUserIdFromCommand() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute(command("customer@example.com"));

            verify(userRepository).findById(USER_ID);
        }

        @Test
        @DisplayName("skips findByEmail when email is unchanged")
        void execute_skipsFindByEmailWhenEmailIsUnchanged() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute(command("customer@example.com"));

            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("calls findByEmail when email differs from current")
        void execute_callsFindByEmailWhenEmailDiffersFromCurrent() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute(command("updated@example.com"));

            verify(userRepository).findByEmail("updated@example.com");
        }

        @Test
        @DisplayName("filters findByEmail result to exclude self")
        void execute_filtersFindByEmailResultToExcludeSelf() {
            User existing = existingUser("customer@example.com");
            User sameUserWithNewEmail = existingUser("updated@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("updated@example.com"))
                    .thenReturn(Optional.of(sameUserWithNewEmail));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Result<UserResponse, UserError> result =
                    useCase.execute(command("updated@example.com"));

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("reconstitutes user with merged fields and updated timestamp")
        void execute_reconstitutesUserWithMergedFieldsAndUpdatedTimestamp() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute(command("customer@example.com"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getId()).isEqualTo(USER_ID);
            assertThat(saved.getEmail().value()).isEqualTo("customer@example.com");
            assertThat(saved.getFullName().value()).isEqualTo("Nguyen Van B");
            assertThat(saved.getPhone()).hasValueSatisfying(phone -> assertThat(phone.value())
                    .isEqualTo("+84909998888"));
            assertThat(saved.getDateOfBirth()).hasValue(LocalDate.of(1996, 6, 20));
            assertThat(saved.getGender())
                    .hasValueSatisfying(gender -> assertThat(gender.value()).isEqualTo("female"));
            assertThat(saved.getIdDocumentNumber())
                    .hasValueSatisfying(id -> assertThat(id.value()).isEqualTo("987654321000"));
            assertThat(saved.getAddressLine())
                    .hasValueSatisfying(
                            address -> assertThat(address.value()).isEqualTo("456 Updated Street"));
            assertThat(saved.getUpdatedAt()).isAfter(ORIGINAL_UPDATED_AT);
        }

        @Test
        @DisplayName("preserves passwordHash, role, createdAt, deletedAt from original user")
        void execute_preservesPasswordHashRoleCreatedAtAndDeletedAtFromOriginalUser() {
            User existing = existingUser("customer@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute(command("customer@example.com"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPasswordHash().value())
                    .isEqualTo(existing.getPasswordHash().value());
            assertThat(saved.getRole()).isEqualTo(existing.getRole());
            assertThat(saved.getCreatedAt()).isEqualTo(existing.getCreatedAt());
            assertThat(saved.getDeletedAt()).isEqualTo(existing.getDeletedAt());
        }
    }

    @Nested
    @DisplayName("failure paths")
    class FailurePaths {

        @Test
        @DisplayName("returns UserNotFound when user does not exist")
        void execute_returnsUserNotFoundWhenUserDoesNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            Result<UserResponse, UserError> result =
                    useCase.execute(command("customer@example.com"));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<UserResponse, UserError>) result).error())
                    .isInstanceOf(UserError.UserNotFound.class);
        }

        @Test
        @DisplayName("returns EmailAlreadyExists when new email taken by another account")
        void execute_returnsEmailAlreadyExistsWhenNewEmailTakenByAnotherAccount() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.findByEmail("updated@example.com"))
                    .thenReturn(Optional.of(otherUser("updated@example.com")));

            Result<UserResponse, UserError> result =
                    useCase.execute(command("updated@example.com"));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<UserResponse, UserError>) result).error())
                    .isInstanceOf(UserError.EmailAlreadyExists.class);
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates findById failures")
        void execute_propagatesFindByIdFailures() {
            when(userRepository.findById(USER_ID)).thenThrow(new RuntimeException("db down"));

            assertThatThrownBy(() -> useCase.execute(command("customer@example.com")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");
        }

        @Test
        @DisplayName("propagates findByEmail failures")
        void execute_propagatesFindByEmailFailures() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.findByEmail("updated@example.com"))
                    .thenThrow(new RuntimeException("email lookup down"));

            assertThatThrownBy(() -> useCase.execute(command("updated@example.com")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("email lookup down");
        }

        @Test
        @DisplayName("propagates save failures")
        void execute_propagatesSaveFailures() {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(existingUser("customer@example.com")));
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("save down"));

            assertThatThrownBy(() -> useCase.execute(command("customer@example.com")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("save down");
        }
    }

    private UpdateUserCommand command(String email) {
        return new UpdateUserCommand(
                USER_ID,
                "Nguyen Van B",
                email,
                "+84 909 998 888",
                LocalDate.of(1996, 6, 20),
                "female",
                "987654321000",
                "456 Updated Street");
    }

    private User existingUser(String email) {
        return User.reconstitute(
                USER_ID,
                EmailAddress.of(email),
                PasswordHash.of("hashed-secret"),
                PersonName.of("Nguyen Van A"),
                PhoneNumber.of("+84901234567"),
                LocalDate.of(1995, 5, 15),
                Gender.of("male"),
                IdDocumentNumber.of("012345678901"),
                AddressLine.of("123 Test Street"),
                UserRole.CUSTOMER,
                CREATED_AT,
                ORIGINAL_UPDATED_AT,
                DELETED_AT);
    }

    private User otherUser(String email) {
        return User.reconstitute(
                UserId.of(UUID.fromString("66666666-6666-6666-6666-666666666666")),
                EmailAddress.of(email),
                PasswordHash.of("other-hash"),
                PersonName.of("Another User"),
                null,
                null,
                null,
                null,
                null,
                UserRole.CUSTOMER,
                CREATED_AT,
                ORIGINAL_UPDATED_AT,
                null);
    }
}

package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder, eventPublisher);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName(
                "creates user with CUSTOMER role, saves via repository and returns UserResponse")
        void execute_createsCustomerUserAndReturnsUserResponse() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            Result<UserResponse, UserError> result = useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.id()).isNotNull();
            assertThat(response.email()).isEqualTo("new@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyen Van A");
            assertThat(response.phone()).isNull();
            assertThat(response.dateOfBirth()).isNull();
            assertThat(response.gender()).isNull();
            assertThat(response.idDocumentNumber()).isNull();
            assertThat(response.addressLine()).isNull();
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER.name());
            assertThat(response.createdAt()).isNotNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("publishes UserRegistered event after saving")
        void execute_publishesUserRegisteredEvent() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(UserRegistered.class);
        }

        @Test
        @DisplayName("returns EmailAlreadyExists failure when save hits unique email constraint")
        void execute_returnsEmailAlreadyExistsWhenSaveHitsUniqueConstraint() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate email"));

            Result<UserResponse, UserError> result = useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, UserError>) result).error())
                    .isInstanceOf(UserError.EmailAlreadyExists.class);
        }
    }

    @Nested
    @DisplayName("email already exists")
    class EmailAlreadyExists {

        @Test
        @DisplayName("returns EmailAlreadyExists failure when email exists")
        void execute_returnsEmailAlreadyExistsFailure() {
            User existingUser = User.reconstitute(
                    io.github.phunguy65.ttbs.backend.user.domain.model.UserId.of(
                            java.util.UUID.randomUUID()),
                    io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress.of(
                            "existing@example.com"),
                    io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash.of("hash"),
                    io.github.phunguy65.ttbs.backend.shared.domain.PersonName.of("Nguyen Van A"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    UserRole.CUSTOMER,
                    java.time.Instant.now(),
                    java.time.Instant.now(),
                    null);
            when(userRepository.findByEmail("existing@example.com"))
                    .thenReturn(Optional.of(existingUser));

            Result<UserResponse, UserError> result = useCase.execute(
                    new RegisterUserCommand("existing@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, UserError>) result).error())
                    .isInstanceOf(UserError.EmailAlreadyExists.class);
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("passes the raw password to password encoder")
        void execute_passesRawPasswordToPasswordEncoder() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            verify(passwordEncoder).encode("secret123");
        }

        @Test
        @DisplayName("creates the user with the expected values before saving")
        void execute_createsUserWithExpectedValuesBeforeSaving() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();
            assertThat(savedUser.getId().value()).isNotNull();
            assertThat(savedUser.getEmail().value()).isEqualTo("new@example.com");
            assertThat(savedUser.getPasswordHash().value()).isEqualTo("hashed-secret");
            assertThat(savedUser.getFullName().value()).isEqualTo("Nguyen Van A");
            assertThat(savedUser.getPhone()).isEmpty();
            assertThat(savedUser.getDateOfBirth()).isEmpty();
            assertThat(savedUser.getGender()).isEmpty();
            assertThat(savedUser.getIdDocumentNumber()).isEmpty();
            assertThat(savedUser.getAddressLine()).isEmpty();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(savedUser.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("saves the user before publishing the domain event")
        void execute_savesUserBeforePublishingEvent() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            InOrder inOrder = inOrder(userRepository, eventPublisher);
            inOrder.verify(userRepository).save(any(User.class));
            inOrder.verify(eventPublisher).publishEvent(any(UserRegistered.class));
        }

        @Test
        @DisplayName("maps the saved user to a complete UserResponse")
        void execute_mapsSavedUserToCompleteUserResponse() {
            User savedUser = User.reconstitute(
                    io.github.phunguy65.ttbs.backend.user.domain.model.UserId.of(
                            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")),
                    io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress.of(
                            "saved@example.com"),
                    io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash.of("hashed-secret"),
                    io.github.phunguy65.ttbs.backend.shared.domain.PersonName.of("Saved User"),
                    io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber.of("+84901234567"),
                    java.time.LocalDate.of(1995, 5, 15),
                    io.github.phunguy65.ttbs.backend.shared.domain.Gender.of("female"),
                    io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber.of(
                            "012345678901"),
                    io.github.phunguy65.ttbs.backend.shared.domain.AddressLine.of(
                            "123 Test Street"),
                    UserRole.CUSTOMER,
                    java.time.Instant.parse("2026-05-15T10:15:30Z"),
                    java.time.Instant.parse("2026-05-15T10:15:30Z"),
                    null);
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Result<UserResponse, UserError> result = useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.id())
                    .isEqualTo(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
            assertThat(response.email()).isEqualTo("saved@example.com");
            assertThat(response.fullName()).isEqualTo("Saved User");
            assertThat(response.phone()).isEqualTo("+84901234567");
            assertThat(response.dateOfBirth()).isEqualTo(java.time.LocalDate.of(1995, 5, 15));
            assertThat(response.gender()).isEqualTo("female");
            assertThat(response.idDocumentNumber()).isEqualTo("012345678901");
            assertThat(response.addressLine()).isEqualTo("123 Test Street");
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER.name());
            assertThat(response.createdAt())
                    .isEqualTo(java.time.Instant.parse("2026-05-15T10:15:30Z"));
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates save failures so the transaction can roll back")
        void execute_propagatesSaveFailure() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db down"));

            assertThatThrownBy(() -> useCase.execute(new RegisterUserCommand(
                            "new@example.com", "secret123", "Nguyen Van A")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");
        }

        @Test
        @DisplayName("does not save a user when password encoding fails")
        void execute_doesNotSaveUserWhenPasswordEncodingFails() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123"))
                    .thenThrow(new RuntimeException("encoder failure"));

            assertThatThrownBy(() -> useCase.execute(new RegisterUserCommand(
                            "new@example.com", "secret123", "Nguyen Van A")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("encoder failure");
            verify(userRepository, never()).save(any(User.class));
        }
    }
}

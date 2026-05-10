package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
            when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            Result<UserResponse, UserError> result = useCase.execute(
                    new RegisterUserCommand("new@example.com", "secret", "Nguyen Van A"));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.email()).isEqualTo("new@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyen Van A");
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER.name());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("publishes UserRegistered event after saving")
        void execute_publishesUserRegisteredEvent() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new RegisterUserCommand("new@example.com", "secret", "Nguyen Van A"));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(UserRegistered.class);
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
                    new RegisterUserCommand("existing@example.com", "secret", "Nguyen Van A"));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, UserError>) result).error())
                    .isInstanceOf(UserError.EmailAlreadyExists.class);
        }
    }
}

package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.CreateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.CreateUserResult;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateUserUseCase(userRepository, passwordEncoder, eventPublisher);
    }

    @Test
    void execute_uniqueEmail_shouldReturnSuccessWithTemporaryPasswordAndUserDto() {
        CreateUserCommand command =
                new CreateUserCommand("alice@example.com", "Alice Nguyen", "0901234567");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<CreateUserResult, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        CreateUserResult createResult =
                ((Result.Success<CreateUserResult, UserError>) result).value();
        assertThat(createResult.temporaryPassword()).isNotBlank();
        assertThat(createResult.user().email()).isEqualTo("alice@example.com");
        assertThat(createResult.user().fullName()).isEqualTo("Alice Nguyen");
        assertThat(createResult.user().phone()).isEqualTo("0901234567");
    }

    @Test
    void execute_uniqueEmail_shouldEncodeTemporaryPassword() {
        CreateUserCommand command = new CreateUserCommand("bob@example.com", "Bob", null);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<CreateUserResult, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        String returnedTempPassword =
                ((Result.Success<CreateUserResult, UserError>) result).value().temporaryPassword();
        verify(passwordEncoder, times(1)).encode(returnedTempPassword);
    }

    @Test
    void execute_uniqueEmail_shouldPublishUserRegisteredEvent() {
        CreateUserCommand command = new CreateUserCommand("carol@example.com", "Carol", null);
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(UserRegistered.class);
        UserRegistered event = (UserRegistered) eventCaptor.getValue();
        assertThat(event.email()).isEqualTo("carol@example.com");
    }

    @Test
    void execute_duplicateEmail_shouldReturnEmailAlreadyExistsAndNeverSave() {
        CreateUserCommand command =
                new CreateUserCommand("existing@example.com", "Existing User", null);
        User existingUser = User.create(
                io.github.phunguy65.ttbs.backend.user.domain.model.UserId.of(
                        java.util.UUID.randomUUID()),
                "existing@example.com",
                "$2a$12$hash",
                "Existing User",
                null);
        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        Result<CreateUserResult, UserError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<CreateUserResult, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.EmailAlreadyExists.class);
        verify(userRepository, never()).save(any());
    }
}

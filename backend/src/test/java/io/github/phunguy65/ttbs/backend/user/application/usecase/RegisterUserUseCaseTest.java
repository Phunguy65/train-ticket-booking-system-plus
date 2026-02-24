package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
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

    @Test
    void execute_success_shouldSaveUserAndReturnDto() {
        RegisterUserCommand command = new RegisterUserCommand(
                "bob@example.com", "password123", "Bob Nguyen", "0901234567");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<UserDto, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        UserDto dto = ((Result.Success<UserDto, UserError>) result).value();
        assertThat(dto.email()).isEqualTo("bob@example.com");
        assertThat(dto.fullName()).isEqualTo("Bob Nguyen");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void execute_duplicateEmail_shouldReturnEmailAlreadyExistsError() {
        RegisterUserCommand command = new RegisterUserCommand(
                "existing@example.com", "password123", "Existing User", null);
        User existingUser = User.create(
                io.github.phunguy65.ttbs.backend.user.domain.model.UserId.of(
                        java.util.UUID.randomUUID()),
                "existing@example.com",
                "$2a$12$hash",
                "Existing User",
                null);
        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        Result<UserDto, UserError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<UserDto, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.EmailAlreadyExists.class);
        verify(userRepository, never()).save(any());
    }
}

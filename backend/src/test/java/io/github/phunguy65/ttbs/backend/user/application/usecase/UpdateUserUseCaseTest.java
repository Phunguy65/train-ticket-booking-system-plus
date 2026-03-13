package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private UpdateUserUseCase useCase;

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final UserId OTHER_ID = UserId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserUseCase(userRepository);
    }

    private User makeUser() {
        return User.reconstitute(
                USER_ID,
                "alice@example.com",
                "$2a$12$hashed",
                "Alice",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_updateFullNameOnly_shouldUpdateOnlyFullName() {
        User existing = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                JsonNullable.of("New Name"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        UserResponse dto = ((Result.Success<UserResponse, UserError>) result).value();
        assertThat(dto.fullName()).isEqualTo("New Name");
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.phone()).isEqualTo("090");
    }

    @Test
    void execute_updatePhoneToNull_shouldRemovePhone() {
        User existing = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID, JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.of(null));

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        UserResponse dto = ((Result.Success<UserResponse, UserError>) result).value();
        assertThat(dto.phone()).isNull();
        assertThat(dto.fullName()).isEqualTo("Alice");
        assertThat(dto.email()).isEqualTo("alice@example.com");
    }

    @Test
    void execute_emptyCommand_shouldReturnUnchangedUser() {
        User existing = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        User saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPhone()).isEqualTo("090");
    }

    @Test
    void execute_userNotFound_shouldReturnUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                JsonNullable.of("New Name"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<UserResponse, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.UserNotFound.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_emailOwnedByDifferentUser_shouldReturnEmailAlreadyExists() {
        User existing = makeUser();
        User otherUser = User.reconstitute(
                OTHER_ID,
                "taken@example.com",
                "$2a$12$hashed2",
                "Other",
                null,
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                JsonNullable.undefined(),
                JsonNullable.of("taken@example.com"),
                JsonNullable.undefined());

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<UserResponse, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.EmailAlreadyExists.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_emailUnchanged_shouldNotReturnEmailAlreadyExists() {
        User existing = makeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                JsonNullable.undefined(),
                JsonNullable.of("alice@example.com"),
                JsonNullable.undefined());

        Result<UserResponse, UserError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository, never()).findByEmail(anyString());
    }
}

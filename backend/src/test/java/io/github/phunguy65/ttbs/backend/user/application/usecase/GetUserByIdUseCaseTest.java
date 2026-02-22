package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetUserByIdUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private GetUserByIdUseCase useCase;

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new GetUserByIdUseCase(userRepository);
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
                Instant.now());
    }

    @Test
    void execute_userFound_shouldReturnSuccessWithUserDto() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));

        Result<UserDto, UserError> result = useCase.execute(USER_ID);

        assertThat(result.isSuccess()).isTrue();
        UserDto dto = ((Result.Success<UserDto, UserError>) result).value();
        assertThat(dto.id()).isEqualTo(USER_ID.value());
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.fullName()).isEqualTo("Alice");
        assertThat(dto.role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void execute_userNotFound_shouldReturnFailureWithUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        Result<UserDto, UserError> result = useCase.execute(USER_ID);

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<UserDto, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.UserNotFound.class);
    }
}

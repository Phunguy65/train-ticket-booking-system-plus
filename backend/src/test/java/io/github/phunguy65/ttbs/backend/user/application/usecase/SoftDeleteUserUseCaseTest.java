package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SoftDeleteUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BookingValidationPort bookingValidationPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SoftDeleteUserUseCase useCase;

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new SoftDeleteUserUseCase(
                userRepository, refreshTokenRepository, bookingValidationPort, eventPublisher);
    }

    private User makeActiveUser() {
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

    private User makeDeletedUser() {
        return User.reconstitute(
                USER_ID,
                "alice@example.com",
                "$2a$12$hashed",
                "Alice",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                Instant.now().minusSeconds(60));
    }

    @Test
    void execute_userNotFound_shouldReturnUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, UserError>) result).error())
                .isInstanceOf(UserError.UserNotFound.class);
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void execute_activeUser_shouldSoftDeleteAndRevokeTokensAndPublishEvent() {
        User user = makeActiveUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingValidationPort.hasActiveBookingsForUser(USER_ID)).thenReturn(false);

        Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
        verify(userRepository).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(UserDeleted.class);
        UserDeleted event = (UserDeleted) eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
    }

    @Test
    void execute_alreadyDeletedUser_shouldReturnSuccessIdempotently() {
        User deletedUser = makeDeletedUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(deletedUser));

        Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}

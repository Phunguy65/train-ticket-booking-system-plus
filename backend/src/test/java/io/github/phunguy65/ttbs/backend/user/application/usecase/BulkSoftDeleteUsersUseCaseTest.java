package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.user.application.command.BulkSoftDeleteUsersCommand;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkSoftDeleteUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BulkSoftDeleteUsersUseCase(
                userRepository, refreshTokenRepository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldReturnAffectedCountAndRevokeTokensAndPublishEvents() {
        UserId id1 = UserId.of(UUID.randomUUID());
        UserId id2 = UserId.of(UUID.randomUUID());
        List<UserId> ids = List.of(id1, id2);

        when(userRepository.softDeleteByIds(eq(ids), any())).thenReturn(2);

        int count = useCase.execute(new BulkSoftDeleteUsersCommand(ids));

        assertThat(count).isEqualTo(2);
        verify(refreshTokenRepository).revokeAllByUserIds(ids);
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }

    @Test
    void execute_singleUser_shouldReturnOneAndPublishOneEvent() {
        UserId id = UserId.of(UUID.randomUUID());
        List<UserId> ids = List.of(id);
        when(userRepository.softDeleteByIds(eq(ids), any())).thenReturn(1);

        int count = useCase.execute(new BulkSoftDeleteUsersCommand(ids));

        assertThat(count).isEqualTo(1);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void execute_allUsersAlreadyDeleted_shouldReturnZero() {
        UserId id = UserId.of(UUID.randomUUID());
        List<UserId> ids = List.of(id);
        // Already deleted → softDeleteByIds returns 0 affected rows
        when(userRepository.softDeleteByIds(eq(ids), any())).thenReturn(0);

        int count = useCase.execute(new BulkSoftDeleteUsersCommand(ids));

        assertThat(count).isEqualTo(0);
        // Tokens are still revoked (idempotent) and events still published per input ID
        verify(refreshTokenRepository).revokeAllByUserIds(ids);
    }
}

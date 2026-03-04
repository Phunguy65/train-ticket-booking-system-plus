package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteCoachesUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkSoftDeleteCoachesUseCase useCase;

    private static final UUID COACH_UUID_1 = UUID.randomUUID();
    private static final UUID COACH_UUID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new BulkSoftDeleteCoachesUseCase(coachRepository, eventPublisher);
    }

    @Test
    void execute_shouldSoftDeleteAndPublishSingleBulkEvent() {
        CoachId id1 = CoachId.of(COACH_UUID_1);
        CoachId id2 = CoachId.of(COACH_UUID_2);
        List<CoachId> coachIds = List.of(id1, id2);

        when(coachRepository.softDeleteByIds(eq(coachIds), any(Instant.class))).thenReturn(2);

        Result<Integer, CoachError> result =
                useCase.execute(new BulkSoftDeleteCoachesCommand(coachIds));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((Result.Success<Integer, CoachError>) result).value()).isEqualTo(2);

        // Exactly one CoachesDeleted bulk event, not N individual events
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoachesDeleted.class);
        CoachesDeleted event = (CoachesDeleted) captor.getValue();
        assertThat(event.coachIds()).containsExactlyInAnyOrderElementsOf(coachIds);
    }

    @Test
    void execute_whenNothingAffected_shouldNotPublishEvent() {
        CoachId id1 = CoachId.of(COACH_UUID_1);
        List<CoachId> coachIds = List.of(id1);

        when(coachRepository.softDeleteByIds(any(), any())).thenReturn(0);

        useCase.execute(new BulkSoftDeleteCoachesCommand(coachIds));

        verifyNoInteractions(eventPublisher);
    }
}

package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteCoachCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SoftDeleteCoachUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SoftDeleteCoachUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID COACH_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SoftDeleteCoachUseCase(coachRepository, eventPublisher);
    }

    private Coach activeCoach() {
        return Coach.reconstitute(
                CoachId.of(COACH_UUID), TrainId.of(TRAIN_UUID), 1, 50, Instant.now(), null);
    }

    private Coach deletedCoach() {
        return Coach.reconstitute(
                CoachId.of(COACH_UUID),
                TrainId.of(TRAIN_UUID),
                1,
                50,
                Instant.now(),
                Instant.now());
    }

    private SoftDeleteCoachCommand command() {
        return new SoftDeleteCoachCommand(CoachId.of(COACH_UUID), TrainId.of(TRAIN_UUID));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void execute_activeCoach_shouldSoftDeleteAndPublishEvent() {
        Coach coach = activeCoach();
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.of(coach));
        when(coachRepository.save(any(Coach.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<Void, CoachError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        verify(coachRepository).save(any(Coach.class));
        verify(eventPublisher).publishEvent(any(CoachDeleted.class));
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    void execute_whenAlreadyDeleted_shouldReturnSuccessWithoutSideEffects() {
        Coach coach = deletedCoach();
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.of(coach));

        Result<Void, CoachError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        verify(coachRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void execute_whenCoachNotFound_shouldReturnCoachNotFound() {
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.empty());

        Result<Void, CoachError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, CoachError>) result).error())
                .isInstanceOf(CoachError.CoachNotFound.class);
        verify(coachRepository, never()).save(any());
    }

    // ── Train mismatch ────────────────────────────────────────────────────────

    @Test
    void execute_whenTrainIdMismatch_shouldReturnCoachNotFound() {
        // Coach belongs to a different train
        UUID differentTrainUuid = UUID.randomUUID();
        Coach coach = Coach.reconstitute(
                CoachId.of(COACH_UUID), TrainId.of(differentTrainUuid), 1, 50, Instant.now(), null);
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.of(coach));

        Result<Void, CoachError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, CoachError>) result).error())
                .isInstanceOf(CoachError.CoachNotFound.class);
        verify(coachRepository, never()).save(any());
    }
}

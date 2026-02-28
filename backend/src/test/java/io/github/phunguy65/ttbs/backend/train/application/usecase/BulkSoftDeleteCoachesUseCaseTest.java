package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteCoachesUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkSoftDeleteCoachesUseCase useCase;

    private static final UUID COACH_UUID_1 = UUID.randomUUID();
    private static final UUID COACH_UUID_2 = UUID.randomUUID();
    private static final UUID COACH_UUID_3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new BulkSoftDeleteCoachesUseCase(coachRepository, seatRepository, eventPublisher);
    }

    private Seat sampleSeat(UUID coachUuid) {
        return Seat.reconstitute(
                SeatId.of(UUID.randomUUID()), CoachId.of(coachUuid), "1A", Instant.now(), null);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void execute_whenNoCoachesHaveSeats_shouldDeleteAllAndPublishEvents() {
        CoachId id1 = CoachId.of(COACH_UUID_1);
        CoachId id2 = CoachId.of(COACH_UUID_2);
        List<CoachId> coachIds = List.of(id1, id2);

        when(seatRepository.findByCoachId(id1)).thenReturn(List.of());
        when(seatRepository.findByCoachId(id2)).thenReturn(List.of());
        when(coachRepository.softDeleteByIds(eq(coachIds), any(Instant.class))).thenReturn(2);

        Result<Integer, CoachError> result =
                useCase.execute(new BulkSoftDeleteCoachesCommand(coachIds));

        assertThat(result.isSuccess()).isTrue();
        int deletedCount = ((Result.Success<Integer, CoachError>) result).value();
        assertThat(deletedCount).isEqualTo(2);
        verify(coachRepository).softDeleteByIds(eq(coachIds), any(Instant.class));
        verify(eventPublisher, times(2)).publishEvent(any(CoachDeleted.class));
    }

    // ── Fail-all: one coach has seats ─────────────────────────────────────────

    @Test
    void execute_whenOneCoachHasSeats_shouldReturnCoachInUseWithConflictingId() {
        CoachId id1 = CoachId.of(COACH_UUID_1);
        CoachId id2 = CoachId.of(COACH_UUID_2);
        CoachId id3 = CoachId.of(COACH_UUID_3);
        List<CoachId> coachIds = List.of(id1, id2, id3);

        when(seatRepository.findByCoachId(id1)).thenReturn(List.of());
        when(seatRepository.findByCoachId(id2)).thenReturn(List.of(sampleSeat(COACH_UUID_2)));
        when(seatRepository.findByCoachId(id3)).thenReturn(List.of());

        Result<Integer, CoachError> result =
                useCase.execute(new BulkSoftDeleteCoachesCommand(coachIds));

        assertThat(result.isFailure()).isTrue();
        CoachError error = ((Result.Failure<Integer, CoachError>) result).error();
        assertThat(error).isInstanceOf(CoachError.CoachInUse.class);
        CoachError.CoachInUse inUse = (CoachError.CoachInUse) error;
        assertThat(inUse.conflictingIds()).containsExactly(COACH_UUID_2);
        verify(coachRepository, never()).softDeleteByIds(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── Fail-all: all coaches have seats ─────────────────────────────────────

    @Test
    void execute_whenAllCoachesHaveSeats_shouldReturnAllConflictingIds() {
        CoachId id1 = CoachId.of(COACH_UUID_1);
        CoachId id2 = CoachId.of(COACH_UUID_2);
        List<CoachId> coachIds = List.of(id1, id2);

        when(seatRepository.findByCoachId(id1)).thenReturn(List.of(sampleSeat(COACH_UUID_1)));
        when(seatRepository.findByCoachId(id2)).thenReturn(List.of(sampleSeat(COACH_UUID_2)));

        Result<Integer, CoachError> result =
                useCase.execute(new BulkSoftDeleteCoachesCommand(coachIds));

        assertThat(result.isFailure()).isTrue();
        CoachError error = ((Result.Failure<Integer, CoachError>) result).error();
        assertThat(error).isInstanceOf(CoachError.CoachInUse.class);
        CoachError.CoachInUse inUse = (CoachError.CoachInUse) error;
        assertThat(inUse.conflictingIds()).containsExactlyInAnyOrder(COACH_UUID_1, COACH_UUID_2);
        verify(coachRepository, never()).softDeleteByIds(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

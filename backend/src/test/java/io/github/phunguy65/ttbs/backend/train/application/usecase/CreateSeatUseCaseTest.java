package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSeatUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private SeatRepository seatRepository;

    private CreateSeatUseCase useCase;

    private static final UUID COACH_UUID = UUID.randomUUID();
    private static final String SEAT_NUMBER = "1A";

    @BeforeEach
    void setUp() {
        useCase = new CreateSeatUseCase(coachRepository, seatRepository);
    }

    private Coach sampleCoach() {
        return Coach.reconstitute(
                CoachId.of(COACH_UUID),
                TrainId.of(UUID.randomUUID()),
                1,
                50,
                java.time.Instant.now(),
                null);
    }

    @Test
    void execute_withValidCommand_shouldReturnSeatDto() {
        when(coachRepository.findById(CoachId.of(COACH_UUID)))
                .thenReturn(Optional.of(sampleCoach()));
        when(seatRepository.existsByCoachIdAndSeatNumber(any(), eq(SEAT_NUMBER)))
                .thenReturn(false);
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(COACH_UUID, SEAT_NUMBER));

        assertThat(result.isSuccess()).isTrue();
        SeatDto dto = ((Result.Success<SeatDto, SeatError>) result).value();
        assertThat(dto.coachId()).isEqualTo(COACH_UUID);
        assertThat(dto.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(dto.id()).isNotNull();
    }

    @Test
    void execute_whenCoachNotFound_shouldReturnCoachNotFoundError() {
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.empty());

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(COACH_UUID, SEAT_NUMBER));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.CoachNotFound.class);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void execute_whenDuplicateSeatNumber_shouldReturnSeatNumberAlreadyExistsError() {
        when(coachRepository.findById(CoachId.of(COACH_UUID)))
                .thenReturn(Optional.of(sampleCoach()));
        when(seatRepository.existsByCoachIdAndSeatNumber(any(), eq(SEAT_NUMBER)))
                .thenReturn(true);

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(COACH_UUID, SEAT_NUMBER));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.SeatNumberAlreadyExists.class);
        verify(seatRepository, never()).save(any());
    }
}

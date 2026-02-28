package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateCoachCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCoachUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private CoachRepository coachRepository;

    private CreateCoachUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final int CAR_NUMBER = 1;
    private static final int TOTAL_SEATS = 50;

    @BeforeEach
    void setUp() {
        useCase = new CreateCoachUseCase(trainRepository, coachRepository);
    }

    private Train sampleTrain() {
        return Train.reconstitute(
                TrainId.of(TRAIN_UUID), "SE001", "Express", 250, Instant.now(), null);
    }

    private Coach sampleCoach(UUID coachUuid) {
        return Coach.reconstitute(
                CoachId.of(coachUuid),
                TrainId.of(TRAIN_UUID),
                CAR_NUMBER,
                TOTAL_SEATS,
                Instant.now(),
                null);
    }

    @Test
    void execute_withValidCommand_shouldReturnCoachDto() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID)))
                .thenReturn(Optional.of(sampleTrain()));
        when(coachRepository.existsByTrainIdAndCarNumber(any(), eq(CAR_NUMBER))).thenReturn(false);
        when(coachRepository.save(any(Coach.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<CoachDto, CoachError> result =
                useCase.execute(new CreateCoachCommand(TRAIN_UUID, CAR_NUMBER, TOTAL_SEATS));

        assertThat(result.isSuccess()).isTrue();
        CoachDto dto = ((Result.Success<CoachDto, CoachError>) result).value();
        assertThat(dto.trainId()).isEqualTo(TRAIN_UUID);
        assertThat(dto.carNumber()).isEqualTo(CAR_NUMBER);
        assertThat(dto.totalSeats()).isEqualTo(TOTAL_SEATS);
        assertThat(dto.id()).isNotNull();
    }

    @Test
    void execute_whenTrainNotFound_shouldReturnTrainNotFoundError() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID))).thenReturn(Optional.empty());

        Result<CoachDto, CoachError> result =
                useCase.execute(new CreateCoachCommand(TRAIN_UUID, CAR_NUMBER, TOTAL_SEATS));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<CoachDto, CoachError>) result).error())
                .isInstanceOf(CoachError.TrainNotFound.class);
        verify(coachRepository, never()).save(any());
    }

    @Test
    void execute_whenDuplicateCarNumber_shouldReturnCarNumberAlreadyExistsError() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID)))
                .thenReturn(Optional.of(sampleTrain()));
        when(coachRepository.existsByTrainIdAndCarNumber(any(), eq(CAR_NUMBER))).thenReturn(true);

        Result<CoachDto, CoachError> result =
                useCase.execute(new CreateCoachCommand(TRAIN_UUID, CAR_NUMBER, TOTAL_SEATS));

        assertThat(result.isFailure()).isTrue();
        CoachError error = ((Result.Failure<CoachDto, CoachError>) result).error();
        assertThat(error).isInstanceOf(CoachError.CarNumberAlreadyExists.class);
        assertThat(((CoachError.CarNumberAlreadyExists) error).carNumber()).isEqualTo(CAR_NUMBER);
        verify(coachRepository, never()).save(any());
    }
}

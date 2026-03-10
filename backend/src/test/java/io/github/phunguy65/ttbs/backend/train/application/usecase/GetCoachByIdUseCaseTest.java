package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
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

@ExtendWith(MockitoExtension.class)
class GetCoachByIdUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    private GetCoachByIdUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID OTHER_TRAIN_UUID = UUID.randomUUID();
    private static final UUID COACH_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetCoachByIdUseCase(coachRepository);
    }

    private Coach sampleCoach(UUID trainUuid) {
        return Coach.reconstitute(
                CoachId.of(COACH_UUID), TrainId.of(trainUuid), 1, 50, Instant.now(), null);
    }

    @Test
    void execute_whenFoundAndMatchingTrainId_shouldReturnCoachDto() {
        when(coachRepository.findById(CoachId.of(COACH_UUID)))
                .thenReturn(Optional.of(sampleCoach(TRAIN_UUID)));

        Result<CoachDto, CoachError> result =
                useCase.execute(CoachId.of(COACH_UUID), TrainId.of(TRAIN_UUID));

        assertThat(result.isSuccess()).isTrue();
        CoachDto dto = ((Result.Success<CoachDto, CoachError>) result).value();
        assertThat(dto.id()).isEqualTo(COACH_UUID);
        assertThat(dto.trainId()).isEqualTo(TRAIN_UUID);
        assertThat(dto.carNumber()).isEqualTo(1);
    }

    @Test
    void execute_whenCoachNotFound_shouldReturnCoachNotFoundError() {
        when(coachRepository.findById(CoachId.of(COACH_UUID))).thenReturn(Optional.empty());

        Result<CoachDto, CoachError> result =
                useCase.execute(CoachId.of(COACH_UUID), TrainId.of(TRAIN_UUID));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<CoachDto, CoachError>) result).error())
                .isInstanceOf(CoachError.CoachNotFound.class);
    }

    @Test
    void execute_whenFoundButDifferentTrainId_shouldReturnCoachNotFoundError() {
        when(coachRepository.findById(CoachId.of(COACH_UUID)))
                .thenReturn(Optional.of(sampleCoach(OTHER_TRAIN_UUID)));

        Result<CoachDto, CoachError> result =
                useCase.execute(CoachId.of(COACH_UUID), TrainId.of(TRAIN_UUID));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<CoachDto, CoachError>) result).error())
                .isInstanceOf(CoachError.CoachNotFound.class);
    }
}

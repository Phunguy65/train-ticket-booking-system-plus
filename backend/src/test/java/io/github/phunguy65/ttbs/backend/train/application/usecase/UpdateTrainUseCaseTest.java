package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class UpdateTrainUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    private UpdateTrainUseCase useCase;

    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new UpdateTrainUseCase(trainRepository);
    }

    private Train makeTrain() {
        return Train.reconstitute(TRAIN_ID, "SE001", "Express", 100, Instant.now(), null);
    }

    @Test
    void execute_updateName_shouldUpdateOnlyName() {
        Train existing = makeTrain();
        when(trainRepository.findById(TRAIN_ID)).thenReturn(Optional.of(existing));
        when(trainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTrainCommand command = new UpdateTrainCommand(
                TRAIN_ID,
                JsonNullable.undefined(),
                JsonNullable.of("New Express"),
                JsonNullable.undefined());

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        TrainDto dto = ((Result.Success<TrainDto, TrainError>) result).value();
        assertThat(dto.name()).isEqualTo("New Express");
        assertThat(dto.trainNumber()).isEqualTo("SE001");
        assertThat(dto.totalSeats()).isEqualTo(100);
    }

    @Test
    void execute_trainNotFound_shouldReturnTrainNotFound() {
        when(trainRepository.findById(TRAIN_ID)).thenReturn(Optional.empty());

        UpdateTrainCommand command = new UpdateTrainCommand(
                TRAIN_ID,
                JsonNullable.of("SE002"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<TrainDto, TrainError>) result).error())
                .isInstanceOf(TrainError.TrainNotFound.class);
        verify(trainRepository, never()).save(any());
    }

    @Test
    void execute_trainNumberConflict_shouldReturnTrainNumberAlreadyExists() {
        Train existing = makeTrain();
        when(trainRepository.findById(TRAIN_ID)).thenReturn(Optional.of(existing));
        when(trainRepository.existsByTrainNumber("SE002")).thenReturn(true);

        UpdateTrainCommand command = new UpdateTrainCommand(
                TRAIN_ID,
                JsonNullable.of("SE002"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<TrainDto, TrainError>) result).error())
                .isInstanceOf(TrainError.TrainNumberAlreadyExists.class);
        verify(trainRepository, never()).save(any());
    }

    @Test
    void execute_sameTrainNumber_shouldNotCheckConflict() {
        Train existing = makeTrain();
        when(trainRepository.findById(TRAIN_ID)).thenReturn(Optional.of(existing));
        when(trainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTrainCommand command = new UpdateTrainCommand(
                TRAIN_ID,
                JsonNullable.of("SE001"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(trainRepository, never()).existsByTrainNumber(anyString());
    }
}

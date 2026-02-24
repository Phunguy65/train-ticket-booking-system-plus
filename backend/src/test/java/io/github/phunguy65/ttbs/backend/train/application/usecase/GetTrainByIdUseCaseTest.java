package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.TrainError;
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

@ExtendWith(MockitoExtension.class)
class GetTrainByIdUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    private GetTrainByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTrainByIdUseCase(trainRepository);
    }

    @Test
    void execute_found_shouldReturnTrainDto() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        Train train = Train.reconstitute(
                trainId,
                "SE001",
                "Reunification Express",
                250,
                Instant.parse("2024-01-01T00:00:00Z"));
        when(trainRepository.findById(trainId)).thenReturn(Optional.of(train));

        Result<TrainDto, TrainError> result = useCase.execute(trainId);

        assertThat(result.isSuccess()).isTrue();
        TrainDto dto = ((Result.Success<TrainDto, TrainError>) result).value();
        assertThat(dto.id()).isEqualTo(trainId.value());
        assertThat(dto.trainNumber()).isEqualTo("SE001");
        assertThat(dto.name()).isEqualTo("Reunification Express");
        assertThat(dto.totalSeats()).isEqualTo(250);
    }

    @Test
    void execute_notFound_shouldReturnTrainNotFoundError() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        when(trainRepository.findById(trainId)).thenReturn(Optional.empty());

        Result<TrainDto, TrainError> result = useCase.execute(trainId);

        assertThat(result.isFailure()).isTrue();
        TrainError error = ((Result.Failure<TrainDto, TrainError>) result).error();
        assertThat(error).isInstanceOf(TrainError.TrainNotFound.class);
    }
}

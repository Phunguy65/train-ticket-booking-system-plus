package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCoachesByTrainUseCaseTest {

    @Mock
    private CoachRepository coachRepository;

    private GetCoachesByTrainUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetCoachesByTrainUseCase(coachRepository);
    }

    private Coach sampleCoach(int carNumber) {
        return Coach.reconstitute(
                CoachId.of(UUID.randomUUID()),
                TrainId.of(TRAIN_UUID),
                carNumber,
                50,
                Instant.now(),
                null);
    }

    @Test
    void execute_shouldReturnMappedList() {
        when(coachRepository.findByTrainId(TrainId.of(TRAIN_UUID)))
                .thenReturn(List.of(sampleCoach(1), sampleCoach(2)));

        List<CoachDto> result = useCase.execute(TrainId.of(TRAIN_UUID));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).carNumber()).isEqualTo(1);
        assertThat(result.get(1).carNumber()).isEqualTo(2);
        assertThat(result).allMatch(dto -> dto.trainId().equals(TRAIN_UUID));
    }

    @Test
    void execute_whenEmptyTrain_shouldReturnEmptyList() {
        when(coachRepository.findByTrainId(TrainId.of(TRAIN_UUID))).thenReturn(List.of());

        List<CoachDto> result = useCase.execute(TrainId.of(TRAIN_UUID));

        assertThat(result).isEmpty();
    }
}

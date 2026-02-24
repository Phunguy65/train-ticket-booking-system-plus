package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTrainsUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    private GetTrainsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTrainsUseCase(trainRepository);
    }

    @Test
    void execute_shouldReturnPageResultWithCorrectMetadata() {
        Train train1 = Train.reconstitute(
                TrainId.of(UUID.randomUUID()), "SE001", "Express 1", 200, Instant.now());
        Train train2 = Train.reconstitute(
                TrainId.of(UUID.randomUUID()), "SE002", "Express 2", 300, Instant.now());
        PageResult<Train> trainPage = PageResult.of(List.of(train1, train2), 0, 20, false);
        when(trainRepository.findAll(0, 20, "createdAt", SortDirection.DESC)).thenReturn(trainPage);

        PageResult<TrainDto> result = useCase.execute(0, 20, "createdAt", SortDirection.DESC);

        assertThat(result.items()).hasSize(2);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.items())
                .extracting(TrainDto::trainNumber)
                .containsExactlyInAnyOrder("SE001", "SE002");
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        PageResult<Train> emptyPage = PageResult.of(List.of(), 0, 20, false);
        when(trainRepository.findAll(0, 20, "trainNumber", SortDirection.ASC))
                .thenReturn(emptyPage);

        PageResult<TrainDto> result = useCase.execute(0, 20, "trainNumber", SortDirection.ASC);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Train train = Train.reconstitute(
                TrainId.of(UUID.randomUUID()), "SE001", "Express 1", 200, Instant.now());
        PageResult<Train> trainPage = PageResult.of(List.of(train), 0, 1, true);
        when(trainRepository.findAll(0, 1, "trainNumber", SortDirection.ASC)).thenReturn(trainPage);

        PageResult<TrainDto> result = useCase.execute(0, 1, "trainNumber", SortDirection.ASC);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }
}

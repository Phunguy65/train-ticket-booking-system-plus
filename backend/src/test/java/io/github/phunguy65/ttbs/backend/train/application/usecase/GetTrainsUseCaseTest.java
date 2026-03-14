package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.train.application.query.GetTrainsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
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
                TrainId.of(UUID.randomUUID()), "SE001", "Express 1", 200, Instant.now(), null);
        Train train2 = Train.reconstitute(
                TrainId.of(UUID.randomUUID()), "SE002", "Express 2", 300, Instant.now(), null);
        PageResponse<Train> trainPage = PageResponse.of(List.of(train1, train2), 0, 20, false);
        when(trainRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(trainPage);

        PageResponse<TrainResponse> result = useCase.execute(new GetTrainsQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.content())
                .extracting(TrainResponse::trainNumber)
                .containsExactlyInAnyOrder("SE001", "SE002");
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        PageResponse<Train> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(trainRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(emptyPage);

        PageResponse<TrainResponse> result = useCase.execute(new GetTrainsQuery(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Train train = Train.reconstitute(
                TrainId.of(UUID.randomUUID()), "SE001", "Express 1", 200, Instant.now(), null);
        PageResponse<Train> trainPage = PageResponse.of(List.of(train), 0, 1, true);
        when(trainRepository.findAll(eq(0), eq(1), any(List.class))).thenReturn(trainPage);

        PageResponse<TrainResponse> result = useCase.execute(new GetTrainsQuery(0, 1));

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }
}

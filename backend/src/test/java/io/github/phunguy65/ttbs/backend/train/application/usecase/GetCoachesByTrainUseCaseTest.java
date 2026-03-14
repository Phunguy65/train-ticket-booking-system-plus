package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachesQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
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
    void execute_shouldReturnPageResultWithCoaches() {
        PageResponse<Coach> coachPage =
                PageResponse.of(List.of(sampleCoach(1), sampleCoach(2)), 0, 20, false);
        when(coachRepository.findAll(eq(0), eq(20), any(List.class), eq(TrainId.of(TRAIN_UUID))))
                .thenReturn(coachPage);

        PageResponse<CoachResponse> result =
                useCase.execute(new GetCoachesQuery(0, 20, TRAIN_UUID));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.content()).extracting(CoachResponse::carNumber).containsExactly(1, 2);
    }

    @Test
    void execute_whenEmptyTrain_shouldReturnEmptyPageResult() {
        PageResponse<Coach> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(coachRepository.findAll(anyInt(), anyInt(), any(List.class), any(TrainId.class)))
                .thenReturn(emptyPage);

        PageResponse<CoachResponse> result =
                useCase.execute(new GetCoachesQuery(0, 20, TRAIN_UUID));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        PageResponse<Coach> coachPage = PageResponse.of(List.of(sampleCoach(1)), 0, 1, true);
        when(coachRepository.findAll(eq(0), eq(1), any(List.class), any(TrainId.class)))
                .thenReturn(coachPage);

        PageResponse<CoachResponse> result = useCase.execute(new GetCoachesQuery(0, 1, TRAIN_UUID));

        assertThat(result.hasNext()).isTrue();
    }
}

package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.train.application.query.GetSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetSeatsByTrainUseCaseTest {

    @Mock
    private SeatRepository seatRepository;

    private GetSeatsByTrainUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID COACH_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetSeatsByTrainUseCase(seatRepository);
    }

    private Seat sampleSeat(String seatNumber) {
        return Seat.reconstitute(
                SeatId.of(UUID.randomUUID()),
                CoachId.of(COACH_UUID),
                seatNumber,
                Instant.now(),
                null);
    }

    @Test
    void execute_withSeats_shouldReturnPageResult() {
        PageResponse<Seat> seatPage =
                PageResponse.of(List.of(sampleSeat("1A"), sampleSeat("1B")), 0, 20, false);
        when(seatRepository.findAll(eq(0), eq(20), any(List.class), eq(TrainId.of(TRAIN_UUID))))
                .thenReturn(seatPage);

        PageResponse<SeatResponse> result = useCase.execute(new GetSeatsQuery(0, 20, TRAIN_UUID));

        assertThat(result.content()).hasSize(2);
        assertThat(result.content())
                .extracting(SeatResponse::seatNumber)
                .containsExactly("1A", "1B");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_withNoSeats_shouldReturnEmptyPageResult() {
        PageResponse<Seat> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(seatRepository.findAll(anyInt(), anyInt(), any(List.class), any(TrainId.class)))
                .thenReturn(emptyPage);

        PageResponse<SeatResponse> result = useCase.execute(new GetSeatsQuery(0, 20, TRAIN_UUID));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }
}

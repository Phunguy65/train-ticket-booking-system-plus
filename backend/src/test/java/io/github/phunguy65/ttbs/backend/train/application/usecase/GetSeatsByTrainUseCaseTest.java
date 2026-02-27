package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
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

    @BeforeEach
    void setUp() {
        useCase = new GetSeatsByTrainUseCase(seatRepository);
    }

    @Test
    void execute_withSeats_shouldReturnDtoList() {
        TrainId trainId = TrainId.of(TRAIN_UUID);
        Seat seat1 = Seat.reconstitute(SeatId.of(UUID.randomUUID()), trainId, "1A", Instant.now());
        Seat seat2 = Seat.reconstitute(SeatId.of(UUID.randomUUID()), trainId, "1B", Instant.now());
        when(seatRepository.findByTrainId(trainId)).thenReturn(List.of(seat1, seat2));

        List<SeatDto> result = useCase.execute(TRAIN_UUID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SeatDto::seatNumber).containsExactly("1A", "1B");
    }

    @Test
    void execute_withNoSeats_shouldReturnEmptyList() {
        when(seatRepository.findByTrainId(any())).thenReturn(List.of());

        List<SeatDto> result = useCase.execute(TRAIN_UUID);

        assertThat(result).isEmpty();
    }
}

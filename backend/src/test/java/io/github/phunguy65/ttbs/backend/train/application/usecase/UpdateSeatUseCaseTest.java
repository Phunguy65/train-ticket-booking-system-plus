package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
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
class UpdateSeatUseCaseTest {

    @Mock
    private SeatRepository seatRepository;

    private UpdateSeatUseCase useCase;

    private static final SeatId SEAT_ID = SeatId.of(UUID.randomUUID());
    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new UpdateSeatUseCase(seatRepository);
    }

    private Seat makeSeat() {
        return Seat.reconstitute(SEAT_ID, TRAIN_ID, "1A", Instant.now());
    }

    @Test
    void execute_updateSeatNumber_shouldUpdateSeatNumber() {
        Seat existing = makeSeat();
        when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.of(existing));
        when(seatRepository.existsByTrainIdAndSeatNumber(TRAIN_ID, "2B")).thenReturn(false);
        when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateSeatCommand command = new UpdateSeatCommand(SEAT_ID, JsonNullable.of("2B"));

        Result<SeatDto, SeatError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        SeatDto dto = ((Result.Success<SeatDto, SeatError>) result).value();
        assertThat(dto.seatNumber()).isEqualTo("2B");
    }

    @Test
    void execute_seatNotFound_shouldReturnSeatNotFound() {
        when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.empty());

        UpdateSeatCommand command = new UpdateSeatCommand(SEAT_ID, JsonNullable.of("2B"));

        Result<SeatDto, SeatError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.SeatNotFound.class);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void execute_seatNumberConflictWithinSameTrain_shouldReturnSeatNumberAlreadyExists() {
        Seat existing = makeSeat();
        when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.of(existing));
        when(seatRepository.existsByTrainIdAndSeatNumber(TRAIN_ID, "2B")).thenReturn(true);

        UpdateSeatCommand command = new UpdateSeatCommand(SEAT_ID, JsonNullable.of("2B"));

        Result<SeatDto, SeatError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.SeatNumberAlreadyExists.class);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void execute_sameSeatNumber_shouldNotCheckConflict() {
        Seat existing = makeSeat();
        when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.of(existing));
        when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateSeatCommand command = new UpdateSeatCommand(SEAT_ID, JsonNullable.of("1A"));

        Result<SeatDto, SeatError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(seatRepository, never()).existsByTrainIdAndSeatNumber(any(), anyString());
    }
}

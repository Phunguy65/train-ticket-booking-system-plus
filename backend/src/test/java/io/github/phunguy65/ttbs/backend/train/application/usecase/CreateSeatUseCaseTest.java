package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSeatUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private SeatRepository seatRepository;

    private CreateSeatUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final String SEAT_NUMBER = "1A";

    @BeforeEach
    void setUp() {
        useCase = new CreateSeatUseCase(trainRepository, seatRepository);
    }

    private Train sampleTrain() {
        return Train.reconstitute(
                TrainId.of(TRAIN_UUID), "SE001", "Express", 250, java.time.Instant.now(), null);
    }

    @Test
    void execute_withValidCommand_shouldReturnSeatDto() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID)))
                .thenReturn(Optional.of(sampleTrain()));
        when(seatRepository.existsByTrainIdAndSeatNumber(any(), eq(SEAT_NUMBER)))
                .thenReturn(false);
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(TRAIN_UUID, SEAT_NUMBER));

        assertThat(result.isSuccess()).isTrue();
        SeatDto dto = ((Result.Success<SeatDto, SeatError>) result).value();
        assertThat(dto.trainId()).isEqualTo(TRAIN_UUID);
        assertThat(dto.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(dto.id()).isNotNull();
    }

    @Test
    void execute_whenTrainNotFound_shouldReturnTrainNotFoundError() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID))).thenReturn(Optional.empty());

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(TRAIN_UUID, SEAT_NUMBER));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.TrainNotFound.class);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void execute_whenDuplicateSeatNumber_shouldReturnSeatNumberAlreadyExistsError() {
        when(trainRepository.findById(TrainId.of(TRAIN_UUID)))
                .thenReturn(Optional.of(sampleTrain()));
        when(seatRepository.existsByTrainIdAndSeatNumber(any(), eq(SEAT_NUMBER)))
                .thenReturn(true);

        Result<SeatDto, SeatError> result =
                useCase.execute(new CreateSeatCommand(TRAIN_UUID, SEAT_NUMBER));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<SeatDto, SeatError>) result).error())
                .isInstanceOf(SeatError.SeatNumberAlreadyExists.class);
        verify(seatRepository, never()).save(any());
    }
}

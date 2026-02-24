package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSeatUseCase {

    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;

    public CreateSeatUseCase(TrainRepository trainRepository, SeatRepository seatRepository) {
        this.trainRepository = trainRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Result<SeatDto, SeatError> execute(CreateSeatCommand command) {
        TrainId trainId = TrainId.of(command.trainId());

        if (trainRepository.findById(trainId).isEmpty()) {
            return Result.failure(new SeatError.TrainNotFound());
        }

        if (seatRepository.existsByTrainIdAndSeatNumber(trainId, command.seatNumber())) {
            return Result.failure(new SeatError.SeatNumberAlreadyExists(command.seatNumber()));
        }

        SeatId seatId = SeatId.of(UUID.randomUUID());
        Seat seat = Seat.create(seatId, trainId, command.seatNumber(), command.seatClass());
        Seat saved = seatRepository.save(seat);

        return Result.success(toDto(saved));
    }

    private SeatDto toDto(Seat seat) {
        return new SeatDto(
                seat.getId().value(),
                seat.getTrainId().value(),
                seat.getSeatNumber(),
                seat.getSeatClass(),
                seat.getCreatedAt());
    }
}

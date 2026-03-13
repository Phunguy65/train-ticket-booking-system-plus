package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSeatUseCase {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;

    public CreateSeatUseCase(CoachRepository coachRepository, SeatRepository seatRepository) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Result<SeatResponse, SeatError> execute(CreateSeatCommand command) {
        CoachId coachId = CoachId.of(command.coachId());

        if (coachRepository.findById(coachId).isEmpty()) {
            return Result.failure(new SeatError.CoachNotFound());
        }

        if (seatRepository.existsByCoachIdAndSeatNumber(coachId, command.seatNumber())) {
            return Result.failure(new SeatError.SeatNumberAlreadyExists(command.seatNumber()));
        }

        SeatId seatId = SeatId.of(UuidGenerator.generate());
        Seat seat = Seat.create(seatId, coachId, command.seatNumber());
        Seat saved = seatRepository.save(seat);

        return Result.success(toDto(saved));
    }

    private SeatResponse toDto(Seat seat) {
        return new SeatResponse(
                seat.getId().value(),
                seat.getCoachId().value(),
                seat.getSeatNumber(),
                seat.getCreatedAt());
    }
}

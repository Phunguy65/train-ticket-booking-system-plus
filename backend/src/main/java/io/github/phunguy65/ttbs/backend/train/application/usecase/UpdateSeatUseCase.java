package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateSeatUseCase {

    private final SeatRepository seatRepository;

    public UpdateSeatUseCase(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Result<SeatDto, SeatError> execute(UpdateSeatCommand command) {
        Seat seat = seatRepository.findById(command.seatId()).orElse(null);
        if (seat == null) {
            return Result.failure(new SeatError.SeatNotFound());
        }

        JsonNullable<String> seatNumberField = command.seatNumber();
        if (seatNumberField.isPresent()) {
            String newSeatNumber = seatNumberField.get();
            if (newSeatNumber != null && !newSeatNumber.equals(seat.getSeatNumber())) {
                if (seatRepository.existsByCoachIdAndSeatNumber(seat.getCoachId(), newSeatNumber)) {
                    return Result.failure(new SeatError.SeatNumberAlreadyExists(newSeatNumber));
                }
            }
        }

        String newSeatNumber =
                seatNumberField.isPresent() ? seatNumberField.get() : seat.getSeatNumber();

        Seat updated = Seat.reconstitute(
                seat.getId(),
                seat.getCoachId(),
                newSeatNumber,
                seat.getCreatedAt(),
                seat.getDeletedAt());

        Seat saved = seatRepository.save(updated);
        return Result.success(toDto(saved));
    }

    private SeatDto toDto(Seat seat) {
        return new SeatDto(
                seat.getId().value(),
                seat.getCoachId().value(),
                seat.getSeatNumber(),
                seat.getCreatedAt());
    }
}

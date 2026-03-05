package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteSeatCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteSeatUseCase {

    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteSeatUseCase(
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, SeatError> execute(SoftDeleteSeatCommand command) {
        Optional<Seat> found = seatRepository.findById(command.seatId());
        if (found.isEmpty()) {
            return Result.failure(new SeatError.SeatNotFound());
        }

        Seat seat = found.get();

        if (seat.isDeleted()) {
            return Result.success();
        }

        if (availabilityRepository.existsActiveBySeatId(command.seatId())) {
            return Result.failure(
                    new SeatError.SeatInUse(List.of(command.seatId().value())));
        }

        seat.softDelete();
        seatRepository.save(seat);

        for (DomainEvent event : seat.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        seat.clearDomainEvents();

        return Result.success();
    }
}

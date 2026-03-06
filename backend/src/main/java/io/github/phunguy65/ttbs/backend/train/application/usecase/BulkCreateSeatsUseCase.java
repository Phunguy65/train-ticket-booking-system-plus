package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkCreateSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkCreateSeatsUseCase {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;

    public BulkCreateSeatsUseCase(CoachRepository coachRepository, SeatRepository seatRepository) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Result<List<SeatDto>, SeatError> execute(BulkCreateSeatsCommand command) {
        CoachId coachId = CoachId.of(command.coachId());

        // Gate 1: parent coach must exist
        if (coachRepository.findById(coachId).isEmpty()) {
            return Result.failure(new SeatError.CoachNotFound());
        }

        // Gate 2: detect in-request duplicates
        List<String> requestedSeatNumbers = command.seats().stream()
                .map(BulkCreateSeatsCommand.SeatItem::seatNumber)
                .toList();
        List<String> inRequestDuplicates = findDuplicates(requestedSeatNumbers);
        if (!inRequestDuplicates.isEmpty()) {
            return Result.failure(new SeatError.DuplicateSeatNumbersInRequest(inRequestDuplicates));
        }

        // Gate 3: detect DB conflicts via single batch query
        Set<String> existingSeatNumbers = new HashSet<>();
        seatRepository
                .findByCoachId(coachId)
                .forEach(s -> existingSeatNumbers.add(s.getSeatNumber()));

        List<String> conflicting = requestedSeatNumbers.stream()
                .filter(existingSeatNumbers::contains)
                .toList();
        if (!conflicting.isEmpty()) {
            return Result.failure(new SeatError.SeatNumbersAlreadyExist(conflicting));
        }

        List<Seat> seats = command.seats().stream()
                .map(item -> Seat.create(
                        SeatId.of(UuidGenerator.generate()), coachId, item.seatNumber()))
                .toList();

        List<Seat> saved = seatRepository.saveAll(seats);

        return Result.success(saved.stream().map(this::toDto).toList());
    }

    private List<String> findDuplicates(List<String> values) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String value : values) {
            if (!seen.add(value)) {
                duplicates.add(value);
            }
        }
        return duplicates;
    }

    private SeatDto toDto(Seat seat) {
        return new SeatDto(
                seat.getId().value(),
                seat.getCoachId().value(),
                seat.getSeatNumber(),
                seat.getCreatedAt());
    }
}

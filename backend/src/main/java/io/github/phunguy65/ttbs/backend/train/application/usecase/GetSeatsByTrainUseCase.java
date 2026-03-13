package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSeatsByTrainUseCase {

    private final SeatRepository seatRepository;

    public GetSeatsByTrainUseCase(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> execute(UUID coachId) {
        return seatRepository.findByCoachId(CoachId.of(coachId)).stream()
                .map(seat -> new SeatResponse(
                        seat.getId().value(),
                        seat.getCoachId().value(),
                        seat.getSeatNumber(),
                        seat.getCreatedAt()))
                .toList();
    }
}

package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
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
    public List<SeatDto> execute(UUID trainId) {
        return seatRepository.findByTrainId(TrainId.of(trainId)).stream()
                .map(seat -> new SeatDto(
                        seat.getId().value(),
                        seat.getTrainId().value(),
                        seat.getSeatNumber(),
                        seat.getSeatClass(),
                        seat.getCreatedAt()))
                .toList();
    }
}

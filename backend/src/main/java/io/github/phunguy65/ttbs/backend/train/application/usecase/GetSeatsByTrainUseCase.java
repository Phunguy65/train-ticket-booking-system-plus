package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSeatsByTrainUseCase {

    private final SeatRepository seatRepository;

    public GetSeatsByTrainUseCase(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> execute(GetSeatsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("seatNumber"), SortOrder.asc("id"));
        PageResponse<Seat> seats = seatRepository.findAll(
                query.page(), query.size(), sort, TrainId.of(query.trainId()));
        return PageResponse.of(
                seats.content().stream().map(this::toDto).toList(),
                seats.page(),
                seats.size(),
                seats.hasNext(),
                seats.total());
    }

    private SeatResponse toDto(Seat seat) {
        return new SeatResponse(
                seat.getId().value(),
                seat.getCoachId().value(),
                seat.getSeatNumber(),
                seat.getCreatedAt());
    }
}

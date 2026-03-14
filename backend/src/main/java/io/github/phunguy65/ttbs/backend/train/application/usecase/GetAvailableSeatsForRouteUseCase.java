package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAvailableSeatsForRouteUseCase {

    private final SeatRepository seatRepository;

    public GetAvailableSeatsForRouteUseCase(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> execute(GetAvailableSeatsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("seatNumber"), SortOrder.asc("id"));
        PageResponse<Seat> seats = seatRepository.findAllAvailable(
                query.page(), query.size(), sort, RouteId.of(query.routeId()));
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

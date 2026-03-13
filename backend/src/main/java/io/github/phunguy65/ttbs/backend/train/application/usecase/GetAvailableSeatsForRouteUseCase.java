package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAvailableSeatsForRouteUseCase {

    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final SeatRepository seatRepository;

    public GetAvailableSeatsForRouteUseCase(
            RouteSeatAvailabilityRepository availabilityRepository, SeatRepository seatRepository) {
        this.availabilityRepository = availabilityRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> execute(UUID routeId) {
        List<RouteSeatAvailability> available =
                availabilityRepository.findAvailableByRouteId(RouteId.of(routeId));

        return available.stream()
                .map(a -> seatRepository.findById(a.getSeatId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toDto)
                .toList();
    }

    private SeatResponse toDto(Seat seat) {
        return new SeatResponse(
                seat.getId().value(),
                seat.getCoachId().value(),
                seat.getSeatNumber(),
                seat.getCreatedAt());
    }
}

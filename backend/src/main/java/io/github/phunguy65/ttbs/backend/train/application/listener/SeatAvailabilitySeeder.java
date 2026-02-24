package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Listens for {@link RouteCreated} domain events and seeds {@code route_seat_availability}
 * rows for every seat on the route's train.
 *
 * <p>Idempotent: duplicate events for the same route do not produce duplicate records because
 * the DB table has a composite PK {@code (route_id, seat_id)} that prevents duplicates.
 * The {@code saveAll} call uses Spring Data {@code save}, which performs an upsert-like
 * merge if the entity already exists (JPA merge semantics). For a stricter ON CONFLICT DO NOTHING,
 * the DB primary key constraint is the final guard.
 */
@Service
public class SeatAvailabilitySeeder {

    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;

    public SeatAvailabilitySeeder(
            SeatRepository seatRepository, RouteSeatAvailabilityRepository availabilityRepository) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @ApplicationModuleListener
    public void onRouteCreated(RouteCreated event) {
        List<Seat> seats = seatRepository.findByTrainId(event.trainId());

        List<RouteSeatAvailability> records = seats.stream()
                .filter(seat -> availabilityRepository
                        .findByRouteIdAndSeatId(event.routeId(), seat.getId())
                        .isEmpty())
                .map(seat -> RouteSeatAvailability.create(event.routeId(), seat.getId()))
                .toList();

        if (!records.isEmpty()) {
            availabilityRepository.saveAll(records);
        }
    }
}

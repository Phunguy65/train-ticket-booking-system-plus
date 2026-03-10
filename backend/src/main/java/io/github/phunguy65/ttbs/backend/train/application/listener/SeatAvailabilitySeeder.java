package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Listens for {@link RouteCreated} domain events and seeds {@code route_seat_availability}
 * rows for every seat on the route's train (via all coaches on the train).
 *
 * <p>Idempotent: duplicate events for the same route do not produce duplicate records because
 * the DB table has a composite PK {@code (route_id, seat_id)} that prevents duplicates.
 * The {@code saveAll} call uses Spring Data {@code save}, which performs an upsert-like
 * merge if the entity already exists (JPA merge semantics). For a stricter ON CONFLICT DO NOTHING,
 * the DB primary key constraint is the final guard.
 */
@Service
public class SeatAvailabilitySeeder {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;

    public SeatAvailabilitySeeder(
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @ApplicationModuleListener
    public void onRouteCreated(RouteCreated event) {
        List<Coach> coaches = coachRepository.findByTrainId(event.trainId());

        List<Seat> seats = coaches.stream()
                .flatMap(coach -> seatRepository.findByCoachId(coach.getId()).stream())
                .toList();

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

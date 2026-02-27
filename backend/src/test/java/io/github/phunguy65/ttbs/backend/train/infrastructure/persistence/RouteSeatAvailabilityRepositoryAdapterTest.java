package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({
    RouteSeatAvailabilityRepositoryAdapter.class,
    RouteSeatAvailabilityEntityMapper.class,
    SeatRepositoryAdapter.class,
    SeatEntityMapper.class,
    TrainRepositoryAdapter.class,
    TrainEntityMapper.class
})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class RouteSeatAvailabilityRepositoryAdapterTest {

    @Autowired
    private RouteSeatAvailabilityRepository availabilityRepository;

    @Autowired
    private SeatRepositoryAdapter seatRepository;

    @Autowired
    private TrainRepositoryAdapter trainRepository;

    private RouteId routeId;
    private SeatId seatId;
    private SeatId seatId2;

    @BeforeEach
    void setUp() {
        // Save train + seats to satisfy FK constraints
        TrainId trainId = TrainId.of(UUID.randomUUID());
        trainRepository.save(Train.create(
                trainId, "RSA-TEST-" + trainId.value().toString().substring(0, 8), "Test", 100));

        routeId = RouteId.of(UUID.randomUUID());

        Seat seat1 = Seat.create(SeatId.of(UUID.randomUUID()), trainId, "1A");
        seatId = seatRepository.save(seat1).getId();

        Seat seat2 = Seat.create(SeatId.of(UUID.randomUUID()), trainId, "1B");
        seatId2 = seatRepository.save(seat2).getId();
    }

    @Test
    void saveAll_shouldPersistMultipleRecords() {
        RouteSeatAvailability a1 = RouteSeatAvailability.create(routeId, seatId);
        List<RouteSeatAvailability> saved = availabilityRepository.saveAll(List.of(a1));

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void findAvailableByRouteId_shouldReturnOnlyAvailableRecords() {
        RouteSeatAvailability a1 = RouteSeatAvailability.create(routeId, seatId);
        availabilityRepository.saveAll(List.of(a1));

        List<RouteSeatAvailability> available =
                availabilityRepository.findAvailableByRouteId(routeId);

        assertThat(available).hasSize(1);
        assertThat(available.getFirst().getStatus())
                .isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void findByRouteIdAndSeatId_whenExists_shouldReturnRecord() {
        RouteSeatAvailability a1 = RouteSeatAvailability.create(routeId, seatId);
        availabilityRepository.saveAll(List.of(a1));

        Optional<RouteSeatAvailability> found =
                availabilityRepository.findByRouteIdAndSeatId(routeId, seatId);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void findByRouteIdAndSeatId_whenMissing_shouldReturnEmpty() {
        Optional<RouteSeatAvailability> found = availabilityRepository.findByRouteIdAndSeatId(
                RouteId.of(UUID.randomUUID()), SeatId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    @Test
    void save_afterBooking_shouldUpdateStatus() {
        RouteSeatAvailability a1 = RouteSeatAvailability.create(routeId, seatId);
        availabilityRepository.saveAll(List.of(a1));

        RouteSeatAvailability loaded =
                availabilityRepository.findByRouteIdAndSeatId(routeId, seatId).orElseThrow();
        loaded.book();
        availabilityRepository.save(loaded);

        RouteSeatAvailability updated =
                availabilityRepository.findByRouteIdAndSeatId(routeId, seatId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
    }

    // ── findByRouteIdAndSeatIdsForUpdate ─────────────────────────────────────

    @Test
    void findByRouteIdAndSeatIdsForUpdate_whenAllAvailable_shouldReturnAllLocked() {
        availabilityRepository.saveAll(List.of(
                RouteSeatAvailability.create(routeId, seatId),
                RouteSeatAvailability.create(routeId, seatId2)));

        List<RouteSeatAvailability> locked =
                availabilityRepository.findByRouteIdAndSeatIdsForUpdate(
                        routeId, List.of(seatId, seatId2));

        assertThat(locked).hasSize(2);
        assertThat(locked).allMatch(a -> a.getStatus() == RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void findByRouteIdAndSeatIdsForUpdate_whenPartialMatch_shouldReturnFewerRecords() {
        // Only save one seat availability record
        availabilityRepository.saveAll(List.of(RouteSeatAvailability.create(routeId, seatId)));

        List<RouteSeatAvailability> locked =
                availabilityRepository.findByRouteIdAndSeatIdsForUpdate(
                        routeId, List.of(seatId, seatId2));

        // Only 1 found instead of 2 — callers treat this as unavailable
        assertThat(locked).hasSize(1);
    }

    @Test
    void findByRouteIdAndSeatIdsForUpdate_afterHold_shouldTransitionToHeld() {
        availabilityRepository.saveAll(List.of(
                RouteSeatAvailability.create(routeId, seatId),
                RouteSeatAvailability.create(routeId, seatId2)));

        List<RouteSeatAvailability> locked =
                availabilityRepository.findByRouteIdAndSeatIdsForUpdate(
                        routeId, List.of(seatId, seatId2));

        locked.forEach(a -> a.hold());
        availabilityRepository.saveAll(locked);

        // Verify persisted as HELD
        RouteSeatAvailability updated1 =
                availabilityRepository.findByRouteIdAndSeatId(routeId, seatId).orElseThrow();
        RouteSeatAvailability updated2 =
                availabilityRepository.findByRouteIdAndSeatId(routeId, seatId2).orElseThrow();
        assertThat(updated1.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
        assertThat(updated2.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
    }
}

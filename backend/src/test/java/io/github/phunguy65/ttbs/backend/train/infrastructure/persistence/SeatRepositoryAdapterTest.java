package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
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
    SeatRepositoryAdapter.class,
    SeatEntityMapper.class,
    TrainRepositoryAdapter.class,
    TrainEntityMapper.class
})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class SeatRepositoryAdapterTest {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TrainRepositoryAdapter trainRepositoryAdapter;

    private TrainId trainId;

    @BeforeEach
    void setUp() {
        // We need an existing train to satisfy the FK constraint on seats.train_id
        trainId = TrainId.of(UUID.randomUUID());
        Train train = Train.create(
                trainId,
                "SE-TEST-" + trainId.value().toString().substring(0, 8),
                "Test Train",
                100);
        trainRepositoryAdapter.save(train);
    }

    private Seat newSeat(String seatNumber) {
        return Seat.create(SeatId.of(UUID.randomUUID()), trainId, seatNumber);
    }

    @Test
    void save_shouldPersistSeatAndReturnDomainModel() {
        Seat seat = newSeat("1A");

        Seat saved = seatRepository.save(seat);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSeatNumber()).isEqualTo("1A");
        assertThat(saved.getTrainId()).isEqualTo(trainId);
    }

    @Test
    void findByTrainId_shouldReturnAllSeatsForTrain() {
        seatRepository.save(newSeat("1A"));
        seatRepository.save(newSeat("1B"));
        seatRepository.save(newSeat("2A"));

        List<Seat> seats = seatRepository.findByTrainId(trainId);

        assertThat(seats).hasSize(3);
        assertThat(seats)
                .extracting(Seat::getSeatNumber)
                .containsExactlyInAnyOrder("1A", "1B", "2A");
    }

    @Test
    void findByTrainId_forDifferentTrain_shouldReturnEmpty() {
        seatRepository.save(newSeat("1A"));

        List<Seat> seats = seatRepository.findByTrainId(TrainId.of(UUID.randomUUID()));

        assertThat(seats).isEmpty();
    }

    @Test
    void findById_existingId_shouldReturnSeat() {
        Seat saved = seatRepository.save(newSeat("3C"));

        Optional<Seat> found = seatRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSeatNumber()).isEqualTo("3C");
    }

    @Test
    void findById_missingId_shouldReturnEmpty() {
        Optional<Seat> found = seatRepository.findById(SeatId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    @Test
    void existsByTrainIdAndSeatNumber_whenExists_shouldReturnTrue() {
        seatRepository.save(newSeat("5D"));

        assertThat(seatRepository.existsByTrainIdAndSeatNumber(trainId, "5D")).isTrue();
    }

    @Test
    void existsByTrainIdAndSeatNumber_whenMissing_shouldReturnFalse() {
        assertThat(seatRepository.existsByTrainIdAndSeatNumber(trainId, "99Z")).isFalse();
    }
}

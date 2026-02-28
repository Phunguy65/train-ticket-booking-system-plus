package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
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
    CoachRepositoryAdapter.class,
    CoachEntityMapper.class,
    TrainRepositoryAdapter.class,
    TrainEntityMapper.class
})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class CoachRepositoryAdapterTest {

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private TrainRepositoryAdapter trainRepositoryAdapter;

    private TrainId trainId;

    @BeforeEach
    void setUp() {
        // We need an existing train to satisfy the FK constraint on coaches.train_id
        trainId = TrainId.of(UUID.randomUUID());
        Train train = Train.create(
                trainId,
                "SE-TEST-" + trainId.value().toString().substring(0, 8),
                "Test Train",
                100);
        trainRepositoryAdapter.save(train);
    }

    private Coach newCoach(int carNumber) {
        return Coach.create(CoachId.of(UUID.randomUUID()), trainId, carNumber, 50);
    }

    // ── save ────────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistCoachAndReturnDomainModel() {
        Coach coach = newCoach(1);

        Coach saved = coachRepository.save(coach);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTrainId()).isEqualTo(trainId);
        assertThat(saved.getCarNumber()).isEqualTo(1);
        assertThat(saved.getTotalSeats()).isEqualTo(50);
        assertThat(saved.getDomainEvents()).isEmpty(); // reconstituted, no events
    }

    // ── findById ────────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_shouldReturnCoach() {
        Coach saved = coachRepository.save(newCoach(1));

        Optional<Coach> found = coachRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCarNumber()).isEqualTo(1);
    }

    @Test
    void findById_missingId_shouldReturnEmpty() {
        Optional<Coach> found = coachRepository.findById(CoachId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    // ── findByTrainId ────────────────────────────────────────────────────────────

    @Test
    void findByTrainId_shouldReturnAllActiveCoachesOrderedByCarNumber() {
        coachRepository.save(newCoach(3));
        coachRepository.save(newCoach(1));
        coachRepository.save(newCoach(2));

        List<Coach> coaches = coachRepository.findByTrainId(trainId);

        assertThat(coaches).hasSize(3);
        assertThat(coaches).extracting(Coach::getCarNumber).containsExactly(1, 2, 3);
    }

    @Test
    void findByTrainId_forDifferentTrain_shouldReturnEmpty() {
        coachRepository.save(newCoach(1));

        List<Coach> coaches = coachRepository.findByTrainId(TrainId.of(UUID.randomUUID()));

        assertThat(coaches).isEmpty();
    }

    // ── existsByTrainIdAndCarNumber ──────────────────────────────────────────────

    @Test
    void existsByTrainIdAndCarNumber_whenExists_shouldReturnTrue() {
        coachRepository.save(newCoach(5));

        assertThat(coachRepository.existsByTrainIdAndCarNumber(trainId, 5)).isTrue();
    }

    @Test
    void existsByTrainIdAndCarNumber_whenMissing_shouldReturnFalse() {
        assertThat(coachRepository.existsByTrainIdAndCarNumber(trainId, 99)).isFalse();
    }

    // ── softDeleteById ───────────────────────────────────────────────────────────

    @Test
    void softDeleteById_shouldExcludeFromActiveResults() {
        Coach coach = coachRepository.save(newCoach(1));
        CoachId coachId = coach.getId();

        coachRepository.softDeleteById(coachId, Instant.now());

        Optional<Coach> found = coachRepository.findById(coachId);
        assertThat(found).isEmpty();
    }

    @Test
    void softDeleteById_shouldExcludeFromFindByTrainId() {
        Coach coach1 = coachRepository.save(newCoach(1));
        coachRepository.save(newCoach(2));

        coachRepository.softDeleteById(coach1.getId(), Instant.now());

        List<Coach> coaches = coachRepository.findByTrainId(trainId);
        assertThat(coaches).hasSize(1);
        assertThat(coaches.getFirst().getCarNumber()).isEqualTo(2);
    }
}

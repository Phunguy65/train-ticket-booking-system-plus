package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({TrainRepositoryAdapter.class, TrainEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class TrainRepositoryAdapterTest {

    @Autowired
    private TrainRepository trainRepository;

    private Train newTrain(String trainNumber) {
        return Train.create(
                TrainId.of(UUID.randomUUID()), trainNumber, "Train " + trainNumber, 200);
    }

    // ── save ────────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistTrainAndReturnDomainModel() {
        Train train = newTrain("SE001");

        Train saved = trainRepository.save(train);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTrainNumber()).isEqualTo("SE001");
        assertThat(saved.getName()).isEqualTo("Train SE001");
        assertThat(saved.getTotalSeats()).isEqualTo(200);
        assertThat(saved.getDomainEvents()).isEmpty(); // reconstituted, no events
    }

    // ── findById ────────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_shouldReturnTrain() {
        Train train = newTrain("SE002");
        Train saved = trainRepository.save(train);

        Optional<Train> found = trainRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTrainNumber()).isEqualTo("SE002");
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void findById_missingId_shouldReturnEmpty() {
        Optional<Train> found = trainRepository.findById(TrainId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    // ── existsByTrainNumber ─────────────────────────────────────────────────────

    @Test
    void existsByTrainNumber_existingNumber_shouldReturnTrue() {
        trainRepository.save(newTrain("SE003"));

        assertThat(trainRepository.existsByTrainNumber("SE003")).isTrue();
    }

    @Test
    void existsByTrainNumber_missingNumber_shouldReturnFalse() {
        assertThat(trainRepository.existsByTrainNumber("NONEXISTENT")).isFalse();
    }

    // ── findAll ─────────────────────────────────────────────────────────────────

    @Test
    void findAll_emptyDatabase_returnsEmptyPageResult() {
        PageResponse<Train> result =
                trainRepository.findAll(0, 20, List.of(SortOrder.asc("trainNumber")));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_firstPage_returnsItemsWithCorrectMetadata() {
        for (int i = 0; i < 5; i++) {
            trainRepository.save(newTrain("TN" + String.format("%03d", i)));
        }

        PageResponse<Train> result =
                trainRepository.findAll(0, 3, List.of(SortOrder.asc("trainNumber")));

        assertThat(result.content()).hasSize(3);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_lastPage_hasNextFalseHasPreviousTrue() {
        for (int i = 0; i < 4; i++) {
            trainRepository.save(newTrain("LT" + String.format("%03d", i)));
        }

        PageResponse<Train> result =
                trainRepository.findAll(1, 3, List.of(SortOrder.asc("trainNumber")));

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void findAll_sortByTrainNumberAsc_returnsItemsInOrder() {
        trainRepository.save(newTrain("ZZZ"));
        trainRepository.save(newTrain("AAA"));
        trainRepository.save(newTrain("MMM"));

        PageResponse<Train> result =
                trainRepository.findAll(0, 10, List.of(SortOrder.asc("trainNumber")));

        assertThat(result.content())
                .extracting(Train::getTrainNumber)
                .containsExactly("AAA", "MMM", "ZZZ");
    }
}

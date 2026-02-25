package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({StationRepositoryAdapter.class, StationEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class StationRepositoryAdapterTest {

    @Autowired
    private StationRepository stationRepository;

    private Station newStation(String code) {
        return Station.create(
                StationId.of(UUID.randomUUID()), code, "Station " + code, "City " + code);
    }

    // ── save ────────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistStationAndReturnDomainModel() {
        Station station = newStation("HN");

        Station saved = stationRepository.save(station);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isEqualTo("HN");
        assertThat(saved.getName()).isEqualTo("Station HN");
        assertThat(saved.getCity()).isEqualTo("City HN");
        assertThat(saved.getDomainEvents()).isEmpty(); // reconstituted, no events
    }

    // ── findById ────────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_shouldReturnStation() {
        Station station = newStation("SGN");
        Station saved = stationRepository.save(station);

        Optional<Station> found = stationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("SGN");
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void findById_missingId_shouldReturnEmpty() {
        Optional<Station> found = stationRepository.findById(StationId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    // ── existsByCode ────────────────────────────────────────────────────────────

    @Test
    void existsByCode_existingCode_shouldReturnTrue() {
        stationRepository.save(newStation("DAN"));

        assertThat(stationRepository.existsByCode("DAN")).isTrue();
    }

    @Test
    void existsByCode_missingCode_shouldReturnFalse() {
        assertThat(stationRepository.existsByCode("NONEXISTENT")).isFalse();
    }

    // ── findAll ─────────────────────────────────────────────────────────────────

    @Test
    void findAll_emptyDatabase_returnsEmptyPageResult() {
        PageResult<Station> result =
                stationRepository.findAll(0, 20, "createdAt", SortDirection.DESC);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_firstPage_returnsItemsWithCorrectMetadata() {
        for (int i = 0; i < 5; i++) {
            stationRepository.save(newStation("S" + String.format("%02d", i)));
        }

        PageResult<Station> result = stationRepository.findAll(0, 3, "code", SortDirection.ASC);

        assertThat(result.items()).hasSize(3);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_lastPage_hasNextFalseHasPreviousTrue() {
        for (int i = 0; i < 4; i++) {
            stationRepository.save(newStation("T" + String.format("%02d", i)));
        }

        PageResult<Station> result = stationRepository.findAll(1, 3, "code", SortDirection.ASC);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }
}

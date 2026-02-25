package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStationsUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    private GetStationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStationsUseCase(stationRepository);
    }

    @Test
    void execute_shouldReturnPageResultWithCorrectMetadata() {
        Station s1 = Station.reconstitute(
                StationId.of(UUID.randomUUID()), "HN", "Hanoi Station", "Hanoi", Instant.now());
        Station s2 = Station.reconstitute(
                StationId.of(UUID.randomUUID()),
                "SGN",
                "Saigon Station",
                "Ho Chi Minh City",
                Instant.now());
        PageResult<Station> stationPage = PageResult.of(List.of(s1, s2), 0, 20, false);
        when(stationRepository.findAll(0, 20, "createdAt", SortDirection.DESC))
                .thenReturn(stationPage);

        PageResult<StationDto> result = useCase.execute(0, 20, "createdAt", SortDirection.DESC);

        assertThat(result.items()).hasSize(2);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.items())
                .extracting(StationDto::code)
                .containsExactlyInAnyOrder("HN", "SGN");
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        PageResult<Station> emptyPage = PageResult.of(List.of(), 0, 20, false);
        when(stationRepository.findAll(0, 20, "name", SortDirection.ASC)).thenReturn(emptyPage);

        PageResult<StationDto> result = useCase.execute(0, 20, "name", SortDirection.ASC);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Station s1 = Station.reconstitute(
                StationId.of(UUID.randomUUID()), "HN", "Hanoi Station", "Hanoi", Instant.now());
        PageResult<Station> stationPage = PageResult.of(List.of(s1), 0, 1, true);
        when(stationRepository.findAll(0, 1, "name", SortDirection.ASC)).thenReturn(stationPage);

        PageResult<StationDto> result = useCase.execute(0, 1, "name", SortDirection.ASC);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }
}

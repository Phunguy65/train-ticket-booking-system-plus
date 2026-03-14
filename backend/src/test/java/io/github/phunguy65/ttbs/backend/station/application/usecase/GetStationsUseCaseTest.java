package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
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
                StationId.of(UUID.randomUUID()),
                "HN",
                "Hanoi Station",
                "Hanoi",
                Instant.now(),
                null);
        Station s2 = Station.reconstitute(
                StationId.of(UUID.randomUUID()),
                "SGN",
                "Saigon Station",
                "Ho Chi Minh City",
                Instant.now(),
                null);
        PageResponse<Station> stationPage = PageResponse.of(List.of(s1, s2), 0, 20, false);
        when(stationRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(stationPage);

        PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.content())
                .extracting(StationResponse::code)
                .containsExactlyInAnyOrder("HN", "SGN");
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        PageResponse<Station> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(stationRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(emptyPage);

        PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Station s1 = Station.reconstitute(
                StationId.of(UUID.randomUUID()),
                "HN",
                "Hanoi Station",
                "Hanoi",
                Instant.now(),
                null);
        PageResponse<Station> stationPage = PageResponse.of(List.of(s1), 0, 1, true);
        when(stationRepository.findAll(eq(0), eq(1), any(List.class))).thenReturn(stationPage);

        PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(0, 1));

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }
}

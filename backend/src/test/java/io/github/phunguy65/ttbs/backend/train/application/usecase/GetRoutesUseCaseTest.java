package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRoutesQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetRoutesUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    private GetRoutesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetRoutesUseCase(routeRepository);
    }

    private Route sampleRoute() {
        return Route.reconstitute(
                RouteId.of(UUID.randomUUID()),
                TrainId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Instant.parse("2025-06-01T08:00:00Z"),
                Instant.parse("2025-06-01T12:00:00Z"),
                Money.vnd(15000L),
                RouteStatus.SCHEDULED,
                Instant.now(),
                null);
    }

    @Test
    void execute_shouldReturnPageResultWithCorrectMetadata() {
        Route route1 = sampleRoute();
        Route route2 = sampleRoute();
        PageResponse<Route> routePage = PageResponse.of(List.of(route1, route2), 0, 20, false);
        when(routeRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(routePage);

        PageResponse<RouteResponse> result = useCase.execute(new GetRoutesQuery(0, 20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        PageResponse<Route> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(routeRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(emptyPage);

        PageResponse<RouteResponse> result = useCase.execute(new GetRoutesQuery(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Route route = sampleRoute();
        PageResponse<Route> routePage = PageResponse.of(List.of(route), 0, 1, true);
        when(routeRepository.findAll(eq(0), eq(1), any(List.class))).thenReturn(routePage);

        PageResponse<RouteResponse> result = useCase.execute(new GetRoutesQuery(0, 1));

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void execute_delegatesToRepository() {
        PageResponse<Route> emptyPage = PageResponse.of(List.of(), 0, 20, false);
        when(routeRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(emptyPage);

        useCase.execute(new GetRoutesQuery(0, 20));

        verify(routeRepository).findAll(eq(0), eq(20), any(List.class));
    }
}

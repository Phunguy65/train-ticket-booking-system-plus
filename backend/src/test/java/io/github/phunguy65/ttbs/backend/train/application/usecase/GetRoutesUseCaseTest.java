package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.math.BigDecimal;
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
                new BigDecimal("150.00"),
                RouteStatus.SCHEDULED,
                Instant.now(),
                null);
    }

    @Test
    void execute_shouldReturnPageResultWithCorrectMetadata() {
        Route route1 = sampleRoute();
        Route route2 = sampleRoute();
        RouteFilter filter = RouteFilter.empty();
        PageResult<Route> routePage = PageResult.of(List.of(route1, route2), 0, 20, false);
        when(routeRepository.findAll(0, 20, "createdAt", SortDirection.DESC, filter))
                .thenReturn(routePage);

        PageResult<RouteDto> result =
                useCase.execute(0, 20, "createdAt", SortDirection.DESC, filter);

        assertThat(result.items()).hasSize(2);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void execute_emptyResult_shouldReturnEmptyPageResult() {
        RouteFilter filter = RouteFilter.empty();
        PageResult<Route> emptyPage = PageResult.of(List.of(), 0, 20, false);
        when(routeRepository.findAll(0, 20, "departureTime", SortDirection.ASC, filter))
                .thenReturn(emptyPage);

        PageResult<RouteDto> result =
                useCase.execute(0, 20, "departureTime", SortDirection.ASC, filter);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextTrue_shouldPropagateHasNext() {
        Route route = sampleRoute();
        RouteFilter filter = RouteFilter.empty();
        PageResult<Route> routePage = PageResult.of(List.of(route), 0, 1, true);
        when(routeRepository.findAll(0, 1, "createdAt", SortDirection.DESC, filter))
                .thenReturn(routePage);

        PageResult<RouteDto> result =
                useCase.execute(0, 1, "createdAt", SortDirection.DESC, filter);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void execute_withFilter_shouldDelegateFilterToRepository() {
        UUID originId = UUID.randomUUID();
        RouteFilter filter = new RouteFilter(originId, null, null, null);
        PageResult<Route> emptyPage = PageResult.of(List.of(), 0, 20, false);
        when(routeRepository.findAll(0, 20, "createdAt", SortDirection.DESC, filter))
                .thenReturn(emptyPage);

        useCase.execute(0, 20, "createdAt", SortDirection.DESC, filter);

        verify(routeRepository).findAll(0, 20, "createdAt", SortDirection.DESC, filter);
    }
}

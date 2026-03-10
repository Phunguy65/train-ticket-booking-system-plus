package io.github.phunguy65.ttbs.backend.train.application.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.station.domain.event.StationsDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.event.RoutesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CascadeOnStationsDeletedListenerTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CascadeOnStationsDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CascadeOnStationsDeletedListener(routeRepository, eventPublisher);
    }

    @Test
    void onStationsDeleted_withActiveRoutes_shouldSoftDeleteAndPublishRoutesDeleted() {
        StationId stationId = StationId.of(UUID.randomUUID());
        RouteId routeId1 = RouteId.of(UUID.randomUUID());
        RouteId routeId2 = RouteId.of(UUID.randomUUID());
        List<RouteId> routeIds = List.of(routeId1, routeId2);

        StationsDeleted event = StationsDeleted.of(List.of(stationId), Instant.now());
        when(routeRepository.findActiveIdsByStationIds(event.stationIds())).thenReturn(routeIds);

        listener.onStationsDeleted(event);

        verify(routeRepository).softDeleteByIds(eq(routeIds), any(Instant.class));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RoutesDeleted.class);
        RoutesDeleted published = (RoutesDeleted) captor.getValue();
        assertThat(published.routeIds()).containsExactlyInAnyOrderElementsOf(routeIds);
    }

    @Test
    void onStationsDeleted_withNoActiveRoutes_shouldNotDeleteOrPublish() {
        StationId stationId = StationId.of(UUID.randomUUID());
        StationsDeleted event = StationsDeleted.of(List.of(stationId), Instant.now());
        when(routeRepository.findActiveIdsByStationIds(any())).thenReturn(List.of());

        listener.onStationsDeleted(event);

        verify(routeRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }
}

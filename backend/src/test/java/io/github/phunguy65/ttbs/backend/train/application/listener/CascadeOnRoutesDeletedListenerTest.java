package io.github.phunguy65.ttbs.backend.train.application.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.RoutesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
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
class CascadeOnRoutesDeletedListenerTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private RouteSeatAvailabilityRepository availabilityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CascadeOnRoutesDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CascadeOnRoutesDeletedListener(
                routeRepository, trainRepository, availabilityRepository, eventPublisher);
    }

    @Test
    void onRoutesDeleted_orphanedTrain_shouldHardDeleteRsaAndSoftDeleteTrainAndPublish() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        TrainId trainId = TrainId.of(UUID.randomUUID());
        RoutesDeleted event = RoutesDeleted.of(List.of(routeId), Instant.now());

        when(routeRepository.findDistinctActiveTrainIdsByRouteIds(event.routeIds()))
                .thenReturn(List.of(trainId));
        when(routeRepository.countActiveByTrainId(trainId)).thenReturn(0L);

        listener.onRoutesDeleted(event);

        verify(availabilityRepository).hardDeleteByRouteIds(event.routeIds());
        verify(trainRepository).softDeleteByIds(eq(List.of(trainId)), any(Instant.class));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TrainsDeleted.class);
        TrainsDeleted published = (TrainsDeleted) captor.getValue();
        assertThat(published.trainIds()).containsExactly(trainId);
    }

    @Test
    void onRoutesDeleted_sharedTrainStillHasActiveRoutes_shouldNotDeleteTrain() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        TrainId trainId = TrainId.of(UUID.randomUUID());
        RoutesDeleted event = RoutesDeleted.of(List.of(routeId), Instant.now());

        when(routeRepository.findDistinctActiveTrainIdsByRouteIds(event.routeIds()))
                .thenReturn(List.of(trainId));
        // Train still has 1 active route remaining
        when(routeRepository.countActiveByTrainId(trainId)).thenReturn(1L);

        listener.onRoutesDeleted(event);

        verify(availabilityRepository).hardDeleteByRouteIds(event.routeIds());
        verify(trainRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void onRoutesDeleted_noTrainsReferenced_shouldOnlyHardDeleteRsa() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        RoutesDeleted event = RoutesDeleted.of(List.of(routeId), Instant.now());

        when(routeRepository.findDistinctActiveTrainIdsByRouteIds(event.routeIds()))
                .thenReturn(List.of());

        listener.onRoutesDeleted(event);

        verify(availabilityRepository).hardDeleteByRouteIds(event.routeIds());
        verify(trainRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }
}

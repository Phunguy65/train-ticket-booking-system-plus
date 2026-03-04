package io.github.phunguy65.ttbs.backend.train;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.station.domain.event.StationsDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.port.validation.SeatValidationPort;
import io.github.phunguy65.ttbs.backend.train.domain.event.RoutesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@TestPropertySource(
        properties = {
            "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-algorithm",
            "jwt.access-token-expiry=900",
            "jwt.refresh-token-expiry=604800"
        })
class CascadeDeleteIntegrationTest {

    @MockitoBean
    private RouteRepository routeRepository;

    @MockitoBean
    private SeatValidationPort seatValidationPort;

    @Test
    void whenStationsDeletedPublished_cascadeListenerShouldPublishRoutesDeleted(Scenario scenario) {
        StationId stationId = StationId.of(UUID.randomUUID());
        RouteId routeId = RouteId.of(UUID.randomUUID());
        List<RouteId> routeIds = List.of(routeId);

        when(routeRepository.findActiveIdsByStationIds(anyList())).thenReturn(routeIds);
        when(routeRepository.softDeleteByIds(anyList(), any(Instant.class))).thenReturn(1);
        when(routeRepository.findDistinctActiveTrainIdsByRouteIds(anyList())).thenReturn(List.of());

        scenario.publish(StationsDeleted.of(List.of(stationId), Instant.now()))
                .andWaitForEventOfType(RoutesDeleted.class)
                .toArriveAndVerify(event ->
                        assertThat(event.routeIds()).containsExactlyInAnyOrderElementsOf(routeIds));
    }
}

package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteRouteCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SoftDeleteRouteUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SoftDeleteRouteUseCase useCase;

    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new SoftDeleteRouteUseCase(routeRepository, eventPublisher);
    }

    private Route activeRoute() {
        return Route.reconstitute(
                ROUTE_ID,
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

    private Route deletedRoute() {
        return Route.reconstitute(
                ROUTE_ID,
                TrainId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Instant.parse("2025-06-01T08:00:00Z"),
                Instant.parse("2025-06-01T12:00:00Z"),
                new BigDecimal("150.00"),
                RouteStatus.SCHEDULED,
                Instant.now(),
                Instant.now().minusSeconds(3600));
    }

    @Test
    void execute_happyPath_shouldSoftDeleteAndPublishEvent() {
        Route route = activeRoute();
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(route));
        when(routeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void, RouteError> result = useCase.execute(new SoftDeleteRouteCommand(ROUTE_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(routeRepository).save(argThat(r -> r.isDeleted()));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RouteDeleted.class);
        RouteDeleted event = (RouteDeleted) eventCaptor.getValue();
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
    }

    @Test
    void execute_routeNotFound_shouldReturnFailure() {
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.empty());

        Result<Void, RouteError> result = useCase.execute(new SoftDeleteRouteCommand(ROUTE_ID));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<Void, RouteError>) result).error())
                .isInstanceOf(RouteError.RouteNotFound.class);
        verifyNoInteractions(eventPublisher);
        verify(routeRepository, never()).save(any());
    }

    @Test
    void execute_alreadyDeleted_shouldReturnSuccessWithNoSideEffects() {
        Route route = deletedRoute();
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(route));

        Result<Void, RouteError> result = useCase.execute(new SoftDeleteRouteCommand(ROUTE_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(routeRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}

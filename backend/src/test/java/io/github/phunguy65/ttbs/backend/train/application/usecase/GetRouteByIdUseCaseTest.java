package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetRouteByIdUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    private GetRouteByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetRouteByIdUseCase(routeRepository);
    }

    private Route sampleRoute(RouteId id) {
        return Route.reconstitute(
                id,
                TrainId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Instant.parse("2025-06-01T08:00:00Z"),
                Instant.parse("2025-06-01T12:00:00Z"),
                Money.vnd(15000L),
                RouteStatus.SCHEDULED,
                Instant.parse("2024-01-01T00:00:00Z"),
                null);
    }

    @Test
    void execute_found_shouldReturnRouteDto() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Route route = sampleRoute(routeId);
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));

        Result<RouteResponse, RouteError> result = useCase.execute(routeId);

        assertThat(result.isSuccess()).isTrue();
        RouteResponse dto = ((Result.Success<RouteResponse, RouteError>) result).value();
        assertThat(dto.id()).isEqualTo(routeId.value());
        assertThat(dto.status()).isEqualTo(RouteStatus.SCHEDULED);
    }

    @Test
    void execute_notFound_shouldReturnRouteNotFoundError() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        when(routeRepository.findById(routeId)).thenReturn(Optional.empty());

        Result<RouteResponse, RouteError> result = useCase.execute(routeId);

        assertThat(result.isFailure()).isTrue();
        RouteError error = ((Result.Failure<RouteResponse, RouteError>) result).error();
        assertThat(error).isInstanceOf(RouteError.RouteNotFound.class);
    }

    @Test
    void execute_found_shouldMapAllFieldsCorrectly() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Route route = sampleRoute(routeId);
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));

        Result<RouteResponse, RouteError> result = useCase.execute(routeId);

        RouteResponse dto = ((Result.Success<RouteResponse, RouteError>) result).value();
        assertThat(dto.trainId()).isEqualTo(route.getTrainId().value());
        assertThat(dto.originStationId()).isEqualTo(route.getOriginStationId().value());
        assertThat(dto.destinationStationId())
                .isEqualTo(route.getDestinationStationId().value());
        assertThat(dto.departureTime()).isEqualTo(route.getDepartureTime());
        assertThat(dto.arrivalTime()).isEqualTo(route.getArrivalTime());
        assertThat(dto.basePrice()).isEqualTo(route.getBasePrice().toLong());
        assertThat(dto.createdAt()).isEqualTo(route.getCreatedAt());
    }
}

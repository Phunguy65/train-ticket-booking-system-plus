package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
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
                new BigDecimal("150.00"),
                RouteStatus.SCHEDULED,
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void execute_found_shouldReturnRouteDto() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Route route = sampleRoute(routeId);
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));

        Result<RouteDto, RouteError> result = useCase.execute(routeId);

        assertThat(result.isSuccess()).isTrue();
        RouteDto dto = ((Result.Success<RouteDto, RouteError>) result).value();
        assertThat(dto.id()).isEqualTo(routeId.value());
        assertThat(dto.status()).isEqualTo(RouteStatus.SCHEDULED);
    }

    @Test
    void execute_notFound_shouldReturnRouteNotFoundError() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        when(routeRepository.findById(routeId)).thenReturn(Optional.empty());

        Result<RouteDto, RouteError> result = useCase.execute(routeId);

        assertThat(result.isFailure()).isTrue();
        RouteError error = ((Result.Failure<RouteDto, RouteError>) result).error();
        assertThat(error).isInstanceOf(RouteError.RouteNotFound.class);
    }

    @Test
    void execute_found_shouldMapAllFieldsCorrectly() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Route route = sampleRoute(routeId);
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));

        Result<RouteDto, RouteError> result = useCase.execute(routeId);

        RouteDto dto = ((Result.Success<RouteDto, RouteError>) result).value();
        assertThat(dto.trainId()).isEqualTo(route.getTrainId().value());
        assertThat(dto.originStationId()).isEqualTo(route.getOriginStationId().value());
        assertThat(dto.destinationStationId())
                .isEqualTo(route.getDestinationStationId().value());
        assertThat(dto.departureTime()).isEqualTo(route.getDepartureTime());
        assertThat(dto.arrivalTime()).isEqualTo(route.getArrivalTime());
        assertThat(dto.basePrice()).isEqualByComparingTo(route.getBasePrice());
        assertThat(dto.createdAt()).isEqualTo(route.getCreatedAt());
    }
}

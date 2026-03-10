package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
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
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class UpdateRouteUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    private UpdateRouteUseCase useCase;

    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());
    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final StationId ORIGIN_ID = StationId.of(UUID.randomUUID());
    private static final StationId DEST_ID = StationId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new UpdateRouteUseCase(routeRepository);
    }

    private Route makeRoute() {
        return Route.reconstitute(
                ROUTE_ID,
                TRAIN_ID,
                ORIGIN_ID,
                DEST_ID,
                Instant.parse("2025-06-01T08:00:00Z"),
                Instant.parse("2025-06-01T12:00:00Z"),
                Money.vnd(15000L),
                RouteStatus.SCHEDULED,
                Instant.now(),
                null);
    }

    @Test
    void execute_updateBasePrice_shouldUpdateOnlyBasePrice() {
        Route existing = makeRoute();
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(existing));
        when(routeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRouteCommand command = new UpdateRouteCommand(
                ROUTE_ID,
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(Money.vnd(20000L)),
                JsonNullable.undefined());

        Result<RouteDto, RouteError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        RouteDto dto = ((Result.Success<RouteDto, RouteError>) result).value();
        assertThat(dto.basePrice()).isEqualTo(20000L);
        assertThat(dto.status()).isEqualTo(RouteStatus.SCHEDULED);
    }

    @Test
    void execute_updateStatus_shouldUpdateStatus() {
        Route existing = makeRoute();
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(existing));
        when(routeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRouteCommand command = new UpdateRouteCommand(
                ROUTE_ID,
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(RouteStatus.CANCELLED));

        Result<RouteDto, RouteError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        RouteDto dto = ((Result.Success<RouteDto, RouteError>) result).value();
        assertThat(dto.status()).isEqualTo(RouteStatus.CANCELLED);
    }

    @Test
    void execute_routeNotFound_shouldReturnRouteNotFound() {
        when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.empty());

        UpdateRouteCommand command = new UpdateRouteCommand(
                ROUTE_ID,
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<RouteDto, RouteError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<RouteDto, RouteError>) result).error())
                .isInstanceOf(RouteError.RouteNotFound.class);
        verify(routeRepository, never()).save(any());
    }
}

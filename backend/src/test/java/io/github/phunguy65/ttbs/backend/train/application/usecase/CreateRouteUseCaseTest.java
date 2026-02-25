package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateRouteUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateRouteUseCase useCase;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID ORIGIN_UUID = UUID.randomUUID();
    private static final UUID DEST_UUID = UUID.randomUUID();
    private static final Instant DEPARTURE = Instant.parse("2025-06-01T08:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2025-06-01T12:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("150.00");

    @BeforeEach
    void setUp() {
        useCase = new CreateRouteUseCase(routeRepository, eventPublisher);
    }

    @Test
    void execute_success_shouldSaveRouteAndReturnDto() {
        CreateRouteCommand command = new CreateRouteCommand(
                TRAIN_UUID, ORIGIN_UUID, DEST_UUID, DEPARTURE, ARRIVAL, PRICE);
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<RouteDto, RouteError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        RouteDto dto = ((Result.Success<RouteDto, RouteError>) result).value();
        assertThat(dto.trainId()).isEqualTo(TRAIN_UUID);
        assertThat(dto.originStationId()).isEqualTo(ORIGIN_UUID);
        assertThat(dto.destinationStationId()).isEqualTo(DEST_UUID);
        assertThat(dto.departureTime()).isEqualTo(DEPARTURE);
        assertThat(dto.arrivalTime()).isEqualTo(ARRIVAL);
        assertThat(dto.basePrice()).isEqualByComparingTo(PRICE);
        assertThat(dto.status()).isEqualTo(RouteStatus.SCHEDULED);
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    void execute_success_shouldPublishRouteCreatedEvent() {
        CreateRouteCommand command = new CreateRouteCommand(
                TRAIN_UUID, ORIGIN_UUID, DEST_UUID, DEPARTURE, ARRIVAL, PRICE);
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        verify(eventPublisher, atLeastOnce()).publishEvent(any(RouteCreated.class));
    }

    @Test
    void execute_success_routeCreatedEventShouldReferenceCorrectTrainAndRoute() {
        CreateRouteCommand command = new CreateRouteCommand(
                TRAIN_UUID, ORIGIN_UUID, DEST_UUID, DEPARTURE, ARRIVAL, PRICE);
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        verify(eventPublisher).publishEvent(argThat((Object event) -> {
            if (!(event instanceof RouteCreated rc)) return false;
            return rc.trainId().equals(TrainId.of(TRAIN_UUID));
        }));
    }
}

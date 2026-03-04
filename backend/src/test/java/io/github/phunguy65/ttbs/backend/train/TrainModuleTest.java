package io.github.phunguy65.ttbs.backend.train;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.application.port.validation.SeatValidationPort;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateSeatUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainCreated;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@TestPropertySource(
        properties = {
            "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-algorithm",
            "jwt.access-token-expiry=900",
            "jwt.refresh-token-expiry=604800"
        })
class TrainModuleTest {

    @MockitoBean
    private SeatValidationPort seatValidationPort;

    @Autowired
    private CreateTrainUseCase createTrainUseCase;

    @Autowired
    private CreateSeatUseCase createSeatUseCase;

    @Autowired
    private RouteSeatAvailabilityPort routeSeatAvailabilityPort;

    @Test
    void trainModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid.
    }

    @Test
    void trainModule_exposesAvailabilityPortViaModelInterface() {
        // Verify that RouteSeatAvailabilityPort bean is wired from the
        // train::model named interface, confirming the module exposes it correctly.
        assertThat(routeSeatAvailabilityPort).isNotNull();
    }

    @Test
    void createTrain_publishesTrainCreatedEvent(PublishedEvents events) {
        CreateTrainCommand command =
                new CreateTrainCommand("SE-MODULE-TEST", "Module Test Train", 100);

        createTrainUseCase.execute(command);

        var trainCreatedEvents = events.ofType(TrainCreated.class);
        assertThat(trainCreatedEvents)
                .as("Expected TrainCreated event to be published")
                .hasSize(1);
        assertThat(trainCreatedEvents.iterator().next().trainNumber()).isEqualTo("SE-MODULE-TEST");
    }
}

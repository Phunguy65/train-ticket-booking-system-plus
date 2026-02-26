package io.github.phunguy65.ttbs.backend.station;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.station.application.command.CreateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.usecase.CreateStationUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationCreated;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.TestPropertySource;

@ApplicationModuleTest
@TestPropertySource(
        properties = {
            "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-algorithm",
            "jwt.access-token-expiry=900",
            "jwt.refresh-token-expiry=604800"
        })
class StationModuleTest {

    @Autowired
    private CreateStationUseCase createStationUseCase;

    @Test
    void stationModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid.
    }

    @Test
    void createStation_publishesStationCreatedEvent(PublishedEvents events) {
        CreateStationCommand command =
                new CreateStationCommand("HN-MOD", "Module Test Station", "Test City");

        createStationUseCase.execute(command);

        var stationCreatedEvents = events.ofType(StationCreated.class);
        assertThat(stationCreatedEvents)
                .as("Expected StationCreated event to be published")
                .hasSize(1);
        assertThat(stationCreatedEvents.iterator().next().code()).isEqualTo("HN-MOD");
    }
}

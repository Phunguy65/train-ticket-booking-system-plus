package io.github.phunguy65.ttbs.backend.user;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
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
class UserModuleTest {

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void userModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid.
    }

    @Test
    void registerUser_publishesUserRegisteredEvent(PublishedEvents events) {
        RegisterUserCommand command = new RegisterUserCommand(
                "moduletest@example.com", "password123", "Module Test User", null);

        registerUserUseCase.execute(command);

        var userRegisteredEvents = events.ofType(UserRegistered.class);
        assertThat(userRegisteredEvents)
                .as("Expected UserRegistered event to be published")
                .hasSize(1);
        assertThat(userRegisteredEvents.iterator().next().email())
                .isEqualTo("moduletest@example.com");
    }
}

package io.github.phunguy65.ttbs.backend.booking;

import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@TestPropertySource(
        properties = {
            "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-algorithm",
            "jwt.access-token-expiry=900",
            "jwt.refresh-token-expiry=604800"
        })
class BookingModuleTest {

    @MockitoBean
    private RouteSeatAvailabilityPort routeSeatAvailabilityPort;

    @Test
    void bookingModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid and
        // no circular dependencies exist.
    }
}

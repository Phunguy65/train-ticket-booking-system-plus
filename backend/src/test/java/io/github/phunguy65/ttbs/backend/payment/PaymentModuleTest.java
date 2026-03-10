package io.github.phunguy65.ttbs.backend.payment;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe.StripeConfig;
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
            "jwt.refresh-token-expiry=604800",
            "stripe.api-key=sk_test_dummy",
            "stripe.webhook-secret=whsec_dummy",
            "stripe.success-url=http://localhost:3000/success",
            "stripe.cancel-url=http://localhost:3000/cancel"
        })
class PaymentModuleTest {

    @MockitoBean
    private StripeGatewayPort stripeGatewayPort;

    @MockitoBean
    private StripeConfig stripeConfig;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private RouteSeatAvailabilityPort routeSeatAvailabilityPort;

    @Test
    void paymentModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid and
        // no circular dependencies exist.
    }
}

package io.github.phunguy65.ttbs.backend.payment;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
class PaymentModuleTest {

    @Test
    void paymentModuleShouldBeValid() {
        // Spring Modulith verifies module boundaries at bootstrap.
        // If the module has illegal dependencies, the context will fail to start.
    }
}

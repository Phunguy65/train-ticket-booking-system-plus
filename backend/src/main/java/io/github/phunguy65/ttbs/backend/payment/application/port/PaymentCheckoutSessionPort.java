package io.github.phunguy65.ttbs.backend.payment.application.port;

import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionStatus;

public interface PaymentCheckoutSessionPort {

    void expireSession(CheckoutSessionId checkoutSessionId);

    CheckoutSessionStatus retrieveSession(CheckoutSessionId checkoutSessionId);
}

package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCheckoutSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateCheckoutSessionUseCase.class);

    private final StripeGatewayPort stripeGatewayPort;
    private final PaymentRepository paymentRepository;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public CreateCheckoutSessionUseCase(
            StripeGatewayPort stripeGatewayPort, PaymentRepository paymentRepository) {
        this.stripeGatewayPort = stripeGatewayPort;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void execute(BookingId bookingId, UserId userId, Money amount, String currency) {
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            log.info("Checkout session already exists for bookingId={}, skipping", bookingId);
            return;
        }

        var command = new CreateCheckoutSessionCommand(
                bookingId, userId, amount, currency, successUrl, cancelUrl);

        StripeGatewayPort.CheckoutSessionResult result =
                stripeGatewayPort.createCheckoutSession(command);

        Payment payment = Payment.create(
                PaymentId.generate(),
                bookingId,
                userId,
                amount,
                result.sessionId(),
                result.checkoutUrl());
        paymentRepository.save(payment);

        log.info("Created checkout session for bookingId={}", bookingId);
    }
}

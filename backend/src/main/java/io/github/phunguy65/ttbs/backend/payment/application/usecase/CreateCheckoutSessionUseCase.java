package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateStripeCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
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
    public void execute(CreateCheckoutSessionCommand command) {
        BookingId bookingId = command.bookingId();
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            log.info("Checkout session already exists for bookingId={}, skipping", bookingId);
            return;
        }

        var stripeCommand = new CreateStripeCheckoutSessionCommand(
                command.bookingId(),
                command.userId(),
                command.amount(),
                command.currency(),
                successUrl,
                cancelUrl);

        StripeGatewayPort.CheckoutSessionResult result =
                stripeGatewayPort.createCheckoutSession(stripeCommand);

        Payment payment = Payment.create(
                PaymentId.generate(),
                command.bookingId(),
                command.userId(),
                command.amount(),
                result.sessionId(),
                result.checkoutUrl());
        paymentRepository.save(payment);

        log.info("Created checkout session for bookingId={}", bookingId);
    }
}

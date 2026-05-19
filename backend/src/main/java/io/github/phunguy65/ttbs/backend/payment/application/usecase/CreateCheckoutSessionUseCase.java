package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateStripeCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.application.response.CheckoutSessionResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.CreateCheckoutResult;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCheckoutSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateCheckoutSessionUseCase.class);

    private final BookingRepository bookingRepository;
    private final StripeGatewayPort stripeGatewayPort;
    private final PaymentRepository paymentRepository;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public CreateCheckoutSessionUseCase(
            BookingRepository bookingRepository,
            StripeGatewayPort stripeGatewayPort,
            PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.stripeGatewayPort = stripeGatewayPort;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Result<CreateCheckoutResult, PaymentError> execute(
            CreateCheckoutSessionCommand command) {

        BookingId bookingId = command.bookingId();

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Result.failure(new PaymentError.BookingNotFound());
        }

        if (!booking.getUserId().equals(command.userId())) {
            return Result.failure(new PaymentError.Forbidden());
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            return Result.failure(new PaymentError.InvalidBookingState(
                    "Booking is not in HELD status (current: " + booking.getStatus() + ")"));
        }
        if (booking.getPaymentDeadline() != null
                && booking.getPaymentDeadline().isBefore(Instant.now())) {
            return Result.failure(
                    new PaymentError.InvalidBookingState("Payment deadline has expired"));
        }

        Payment existingPayment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existingPayment != null) {
            return switch (existingPayment.getStatus()) {
                case PENDING -> {
                    log.info(
                            "Returning existing PENDING checkout session for bookingId={}",
                            bookingId);
                    yield Result.success(
                            new CreateCheckoutResult(toResponse(existingPayment), false));
                }
                case PAID -> Result.failure(new PaymentError.AlreadyProcessed());
                case FAILED, CANCELLED -> {
                    log.info(
                            "Existing payment is {}, creating new checkout session for bookingId={}",
                            existingPayment.getStatus(),
                            bookingId);
                    yield createNewCheckoutSession(booking);
                }
                case REFUNDED -> Result.failure(new PaymentError.AlreadyProcessed());
            };
        }

        return createNewCheckoutSession(booking);
    }

    private Result<CreateCheckoutResult, PaymentError> createNewCheckoutSession(Booking booking) {
        var stripeCommand = new CreateStripeCheckoutSessionCommand(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                successUrl,
                cancelUrl);

        StripeGatewayPort.CheckoutSessionResult result =
                stripeGatewayPort.createCheckoutSession(stripeCommand);

        Payment payment = Payment.create(
                PaymentId.generate(),
                booking.getBookingId(),
                booking.getUserId(),
                booking.getTotalPrice(),
                result.sessionId(),
                result.checkoutUrl());
        paymentRepository.save(payment);

        log.info("Created checkout session for bookingId={}", booking.getBookingId());
        return Result.success(new CreateCheckoutResult(toResponse(payment), true));
    }

    private static CheckoutSessionResponse toResponse(Payment payment) {
        return new CheckoutSessionResponse(
                payment.getPaymentId().value(), payment.getCheckoutUrl(), payment.getStatus());
    }
}

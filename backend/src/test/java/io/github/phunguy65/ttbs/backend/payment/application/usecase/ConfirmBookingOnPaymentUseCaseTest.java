package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.ConfirmSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.ConfirmSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ConfirmBookingOnPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ConfirmSeatHoldUseCase confirmSeatHoldUseCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ConfirmBookingOnPaymentUseCase useCase;

    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String EVENT_ID = "evt_test_001";

    @Test
    void execute_withPendingPayment_shouldConfirmAndDelegateToBooking() {
        Payment payment = Payment.create(
                BOOKING_ID, CheckoutSessionId.of("cs_test_abc"), new BigDecimal("300000"));
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);

        useCase.execute(BOOKING_ID, EVENT_ID);

        verify(paymentRepository).save(payment);
        verify(confirmSeatHoldUseCase).execute(any(ConfirmSeatHoldCommand.class));
    }

    @Test
    void execute_withNoPaymentFound_shouldSkipGracefully() {
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

        useCase.execute(BOOKING_ID, EVENT_ID);

        verify(paymentRepository, never()).save(any());
        verify(confirmSeatHoldUseCase, never()).execute(any());
    }

    @Test
    void execute_withAlreadyPaidPayment_shouldBeIdempotent() {
        Payment payment = Payment.create(
                BOOKING_ID, CheckoutSessionId.of("cs_test_abc"), new BigDecimal("300000"));
        payment.confirm(EVENT_ID);
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(payment));

        useCase.execute(BOOKING_ID, EVENT_ID);

        verify(paymentRepository, never()).save(any());
        verify(confirmSeatHoldUseCase, never()).execute(any());
    }
}

package io.github.phunguy65.ttbs.backend.booking.application.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkForceBookingCancellationHelperTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @InjectMocks
    private BulkForceBookingCancellationHelper helper;

    @Test
    void cancelAllReturnsImmediatelyWhenBookingIdsEmpty() {
        helper.cancelAll(List.of());

        verifyNoInteractions(bookingRepository, paymentRepository, stripeGatewayPort);
    }

    @Test
    void cancelAllBulkCancelsActiveBookingsAndRefundsOnlyPaidConfirmedOnes() {
        BookingId heldBookingId = BookingId.of(UUID.randomUUID());
        BookingId confirmedBookingId = BookingId.of(UUID.randomUUID());
        BookingRepository.CancellationCandidate heldCandidate =
                new BookingRepository.CancellationCandidate(heldBookingId, BookingStatus.HELD);
        BookingRepository.CancellationCandidate confirmedCandidate =
                new BookingRepository.CancellationCandidate(
                        confirmedBookingId, BookingStatus.CONFIRMED);
        Payment payment = Payment.reconstitute(
                PaymentId.of(UUID.randomUUID()),
                confirmedBookingId,
                UserId.of(UUID.randomUUID()),
                Money.vnd(150_000L),
                PaymentStatus.PAID,
                "checkout_session",
                "https://checkout.test",
                "pi_confirmed",
                "evt_1",
                null,
                java.time.Instant.now(),
                java.time.Instant.now());

        when(bookingRepository.findCancellationCandidatesByIds(any()))
                .thenReturn(List.of(heldCandidate, confirmedCandidate));
        when(paymentRepository.findByBookingIds(List.of(confirmedBookingId)))
                .thenReturn(List.of(payment));

        helper.cancelAll(List.of(
                heldBookingId.value(), confirmedBookingId.value(), confirmedBookingId.value()));

        verify(bookingRepository).cancelByIds(List.of(heldBookingId, confirmedBookingId));
        verify(stripeGatewayPort)
                .createRefund("pi_confirmed", "refund_" + confirmedBookingId.value());
        verify(paymentRepository).save(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void cancelAllSkipsRefundWhenConfirmedBookingHasNoPayment() {
        BookingId confirmedBookingId = BookingId.of(UUID.randomUUID());
        BookingRepository.CancellationCandidate confirmedCandidate =
                new BookingRepository.CancellationCandidate(
                        confirmedBookingId, BookingStatus.CONFIRMED);

        when(bookingRepository.findCancellationCandidatesByIds(any()))
                .thenReturn(List.of(confirmedCandidate));
        when(paymentRepository.findByBookingIds(List.of(confirmedBookingId))).thenReturn(List.of());

        helper.cancelAll(List.of(confirmedBookingId.value()));

        verify(bookingRepository).cancelByIds(List.of(confirmedBookingId));
        verify(stripeGatewayPort, never()).createRefund(any(), any());
    }

    @Test
    void cancelAllSkipsRefundWhenPaymentNotPaid() {
        BookingId confirmedBookingId = BookingId.of(UUID.randomUUID());
        BookingRepository.CancellationCandidate confirmedCandidate =
                new BookingRepository.CancellationCandidate(
                        confirmedBookingId, BookingStatus.CONFIRMED);
        Payment payment = Payment.reconstitute(
                PaymentId.of(UUID.randomUUID()),
                confirmedBookingId,
                UserId.of(UUID.randomUUID()),
                Money.vnd(150_000L),
                PaymentStatus.PENDING,
                "checkout_session",
                "https://checkout.test",
                null,
                "evt_1",
                null,
                java.time.Instant.now(),
                java.time.Instant.now());

        when(bookingRepository.findCancellationCandidatesByIds(any()))
                .thenReturn(List.of(confirmedCandidate));
        when(paymentRepository.findByBookingIds(List.of(confirmedBookingId)))
                .thenReturn(List.of(payment));

        helper.cancelAll(List.of(confirmedBookingId.value()));

        verify(bookingRepository).cancelByIds(List.of(confirmedBookingId));
        verify(stripeGatewayPort, never()).createRefund(any(), any());
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void cancelAllLogsAndContinuesWhenRefundFails() {
        BookingId confirmedBookingId = BookingId.of(UUID.randomUUID());
        BookingRepository.CancellationCandidate confirmedCandidate =
                new BookingRepository.CancellationCandidate(
                        confirmedBookingId, BookingStatus.CONFIRMED);
        Payment payment = Payment.reconstitute(
                PaymentId.of(UUID.randomUUID()),
                confirmedBookingId,
                UserId.of(UUID.randomUUID()),
                Money.vnd(150_000L),
                PaymentStatus.PAID,
                "checkout_session",
                "https://checkout.test",
                "pi_confirmed",
                "evt_1",
                null,
                java.time.Instant.now(),
                java.time.Instant.now());

        when(bookingRepository.findCancellationCandidatesByIds(any()))
                .thenReturn(List.of(confirmedCandidate));
        when(paymentRepository.findByBookingIds(List.of(confirmedBookingId)))
                .thenReturn(List.of(payment));
        doThrow(new RuntimeException("stripe down"))
                .when(stripeGatewayPort)
                .createRefund("pi_confirmed", "refund_" + confirmedBookingId.value());

        helper.cancelAll(List.of(confirmedBookingId.value()));

        verify(bookingRepository).cancelByIds(List.of(confirmedBookingId));
        verify(paymentRepository, never()).save(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}

package io.github.phunguy65.ttbs.backend.booking.application.helper;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BulkForceBookingCancellationHelper {

    private static final Logger log =
            LoggerFactory.getLogger(BulkForceBookingCancellationHelper.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final StripeGatewayPort stripeGatewayPort;

    public BulkForceBookingCancellationHelper(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            StripeGatewayPort stripeGatewayPort) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.stripeGatewayPort = stripeGatewayPort;
    }

    public void cancelAll(List<UUID> bookingIds) {
        if (bookingIds.isEmpty()) {
            return;
        }

        List<BookingId> deduplicatedBookingIds =
                bookingIds.stream().distinct().map(BookingId::of).toList();

        List<BookingRepository.CancellationCandidate> cancellationCandidates =
                bookingRepository.findCancellationCandidatesByIds(deduplicatedBookingIds);
        if (cancellationCandidates.isEmpty()) {
            return;
        }

        List<BookingId> activeBookingIds = cancellationCandidates.stream()
                .map(BookingRepository.CancellationCandidate::bookingId)
                .toList();
        bookingRepository.cancelByIds(activeBookingIds);

        List<BookingId> confirmedBookingIds = cancellationCandidates.stream()
                .filter(candidate -> candidate.status() == BookingStatus.CONFIRMED)
                .map(BookingRepository.CancellationCandidate::bookingId)
                .toList();
        if (confirmedBookingIds.isEmpty()) {
            return;
        }

        Map<BookingId, Payment> paymentsByBookingId =
                paymentRepository.findByBookingIds(confirmedBookingIds).stream()
                        .collect(Collectors.toMap(
                                Payment::getBookingId, Function.identity(), (left, right) -> left));

        for (BookingId bookingId : confirmedBookingIds) {
            Payment payment = paymentsByBookingId.get(bookingId);
            if (payment == null) {
                log.info(
                        "BulkForceBookingCancellation: no payment found for bookingId={}, skipping refund",
                        bookingId);
                continue;
            }

            if (payment.getStatus() != PaymentStatus.PAID) {
                log.info(
                        "BulkForceBookingCancellation: payment for bookingId={} is in status {}, skipping refund",
                        bookingId,
                        payment.getStatus());
                continue;
            }

            String idempotencyKey = "refund_" + bookingId.value();
            try {
                stripeGatewayPort.createRefund(payment.getStripePaymentIntentId(), idempotencyKey);
                payment.markRefunded();
                paymentRepository.save(payment);
                payment.clearDomainEvents();
            } catch (Exception e) {
                log.error(
                        "BulkForceBookingCancellation: refund failed for bookingId={}, paymentIntentId={}: {}",
                        bookingId,
                        payment.getStripePaymentIntentId(),
                        e.getMessage(),
                        e);
            }
        }
    }
}

package io.github.phunguy65.ttbs.backend.payment.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.UserPaymentSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for {@link Payment}.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<PaymentSummary> findSummaryById(PaymentId paymentId);

    Optional<Payment> findByBookingId(BookingId bookingId);

    Optional<PaymentSummary> findSummaryByBookingId(BookingId bookingId);

    List<Payment> findByBookingIds(List<BookingId> bookingIds);

    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);

    Optional<Payment> findByStripeEventId(String stripeEventId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Finds payments for a specific user with booking route information.
     *
     * @param userId owner of the payments
     * @param page zero-based page index
     * @param size page size
     * @param sort ordering rules
     * @return paginated user payment summaries with booking route data
     */
    PageResponse<UserPaymentSummary> findByUserId(
            UserId userId, int page, int size, List<SortOrder> sort);
}

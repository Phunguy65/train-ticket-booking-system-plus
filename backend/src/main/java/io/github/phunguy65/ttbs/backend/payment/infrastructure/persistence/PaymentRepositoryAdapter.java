package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentEntityMapper mapper;

    PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository, PaymentEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(payment)));
    }

    @Override
    public Optional<PaymentSummary> findSummaryById(PaymentId paymentId) {
        return jpaRepository.findSummaryById(paymentId.value());
    }

    @Override
    public Optional<Payment> findByBookingId(BookingId bookingId) {
        return jpaRepository.findByBookingId(bookingId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentSummary> findSummaryByBookingId(BookingId bookingId) {
        return jpaRepository.findSummaryByBookingId(bookingId.value());
    }

    @Override
    public List<Payment> findByBookingIds(List<BookingId> bookingIds) {
        List<java.util.UUID> uuids = bookingIds.stream().map(BookingId::value).toList();
        return jpaRepository.findByBookingIdIn(uuids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Payment> findByCheckoutSessionId(String checkoutSessionId) {
        return jpaRepository.findByCheckoutSessionId(checkoutSessionId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByStripeEventId(String stripeEventId) {
        return jpaRepository.findByStripeEventId(stripeEventId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return jpaRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
                .map(mapper::toDomain);
    }
}

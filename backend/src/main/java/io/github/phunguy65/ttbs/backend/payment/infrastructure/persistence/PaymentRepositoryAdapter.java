package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.UserPaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        return jpaRepository.findPaymentSummaryById(paymentId.value()).map(this::toSummary);
    }

    @Override
    public Optional<Payment> findByBookingId(BookingId bookingId) {
        return jpaRepository.findByBookingId(bookingId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentSummary> findSummaryByBookingId(BookingId bookingId) {
        return jpaRepository.findPaymentSummaryByBookingId(bookingId.value()).map(this::toSummary);
    }

    @Override
    public List<Payment> findByBookingIds(List<BookingId> bookingIds) {
        List<java.util.UUID> uuids = bookingIds.stream().map(BookingId::value).toList();
        return jpaRepository.findByBookingIdIn(uuids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<UserPaymentSummary> findByUserId(
            UserId userId, int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<UserPaymentSummaryView> result =
                jpaRepository.findUserPaymentsByUserId(userId.value(), pageable);
        List<UserPaymentSummary> items =
                result.getContent().stream().map(this::toUserPaymentSummary).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    private PaymentSummary toSummary(PaymentSummaryView view) {
        return new PaymentSummary(
                view.getId(),
                view.getBookingId(),
                view.getUserId(),
                view.getStatus(),
                view.getCheckoutUrl(),
                view.getAmount(),
                view.getCurrency(),
                view.getStripePaymentIntentId(),
                view.getCreatedAt());
    }

    private UserPaymentSummary toUserPaymentSummary(UserPaymentSummaryView view) {
        return new UserPaymentSummary(
                view.getId(),
                view.getBookingId(),
                view.getUserId(),
                view.getStatus(),
                view.getAmount(),
                view.getCurrency(),
                view.getCreatedAt(),
                view.getOriginStationName(),
                view.getDestinationStationName(),
                view.getDepartureTime());
    }

    private Sort toSpringSort(List<SortOrder> orders) {
        if (orders.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> springOrders = orders.stream()
                .map(o -> o.direction() == SortOrder.Direction.ASC
                        ? Sort.Order.asc(o.field())
                        : Sort.Order.desc(o.field()))
                .toList();
        return Sort.by(springOrders);
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

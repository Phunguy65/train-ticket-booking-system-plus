package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
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
        PaymentEntity entity = mapper.toEntity(payment);
        PaymentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByCheckoutSessionId(String checkoutSessionId) {
        return jpaRepository.findByCheckoutSessionId(checkoutSessionId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByBookingId(UUID bookingId) {
        return jpaRepository.findByBookingId(bookingId).map(mapper::toDomain);
    }
}

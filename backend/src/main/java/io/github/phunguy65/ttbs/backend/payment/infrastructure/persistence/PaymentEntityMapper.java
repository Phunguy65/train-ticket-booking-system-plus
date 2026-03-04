package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

@Component
class PaymentEntityMapper {

    Payment toDomain(PaymentEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getBookingId(),
                entity.getCheckoutSessionId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getStripeEventId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId().value());
        entity.setBookingId(payment.getBookingId());
        entity.setCheckoutSessionId(payment.getCheckoutSessionId().value());
        entity.setAmount(payment.getAmountVnd());
        entity.setCurrency("vnd");
        entity.setStatus(payment.getStatus());
        entity.setStripeEventId(payment.getStripeEventId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        return entity;
    }
}

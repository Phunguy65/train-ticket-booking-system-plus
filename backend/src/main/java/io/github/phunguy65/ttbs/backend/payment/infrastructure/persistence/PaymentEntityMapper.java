package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
class PaymentEntityMapper {

    Payment toDomain(PaymentEntity entity) {
        Money amount = Money.of(
                BigDecimal.valueOf(entity.getAmount()), Currency.getInstance(entity.getCurrency()));
        return Payment.reconstitute(
                PaymentId.of(entity.getId()),
                BookingId.of(entity.getBookingId()),
                UserId.of(entity.getUserId()),
                amount,
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getCheckoutSessionId(),
                entity.getCheckoutUrl(),
                entity.getStripePaymentIntentId(),
                entity.getStripeEventId(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    PaymentEntity toEntity(Payment domain) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(domain.getPaymentId().value());
        entity.setBookingId(domain.getBookingId().value());
        entity.setUserId(domain.getUserId().value());
        entity.setAmount(domain.getAmount().toLong());
        entity.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        entity.setStatus(domain.getStatus().name());
        entity.setCheckoutSessionId(domain.getCheckoutSessionId());
        entity.setCheckoutUrl(domain.getCheckoutUrl());
        entity.setStripePaymentIntentId(domain.getStripePaymentIntentId());
        entity.setStripeEventId(domain.getStripeEventId());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

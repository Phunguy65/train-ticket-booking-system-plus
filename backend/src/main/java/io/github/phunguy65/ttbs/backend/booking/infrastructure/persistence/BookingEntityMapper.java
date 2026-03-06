package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
class BookingEntityMapper {

    Booking toDomain(BookingEntity entity) {
        Money totalPrice = Money.of(
                BigDecimal.valueOf(entity.getTotalPrice()),
                Currency.getInstance(entity.getCurrency()));
        return Booking.reconstitute(
                BookingId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                RouteId.of(entity.getRouteId()),
                entity.getPassengerName(),
                entity.getPassengerEmail(),
                entity.getPassengerPhone(),
                totalPrice,
                entity.getCurrency(),
                BookingStatus.valueOf(entity.getStatus()),
                entity.getIdempotencyKey(),
                entity.getPaymentDeadline(),
                entity.getCreatedAt());
    }

    BookingEntity toEntity(Booking domain) {
        BookingEntity entity = new BookingEntity();
        entity.setId(domain.getBookingId().value());
        entity.setUserId(domain.getUserId().value());
        entity.setRouteId(domain.getRouteId().value());
        entity.setPassengerName(domain.getPassengerName());
        entity.setPassengerEmail(domain.getPassengerEmail());
        entity.setPassengerPhone(domain.getPassengerPhone());
        entity.setTotalPrice(domain.getTotalPrice().toLong());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus().name());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setPaymentDeadline(domain.getPaymentDeadline());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}

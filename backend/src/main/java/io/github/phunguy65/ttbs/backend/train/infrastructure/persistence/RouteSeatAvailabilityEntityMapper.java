package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import org.springframework.stereotype.Component;

@Component
class RouteSeatAvailabilityEntityMapper {

    RouteSeatAvailability toDomain(RouteSeatAvailabilityEntity entity) {
        Long priceLong = entity.getPriceAtBooking();
        Money priceAtBooking = priceLong != null ? Money.vnd(priceLong) : null;
        return RouteSeatAvailability.reconstitute(
                ScheduledTripId.of(entity.getId().getScheduledTripId()),
                SeatId.of(entity.getId().getSeatId()),
                RouteSeatAvailabilityStatus.valueOf(entity.getStatus()),
                entity.getBookingId(),
                priceAtBooking,
                entity.getVersion());
    }

    RouteSeatAvailabilityEntity toEntity(RouteSeatAvailability domain) {
        RouteSeatAvailabilityEntity entity = new RouteSeatAvailabilityEntity();
        entity.setId(new RouteSeatAvailabilityId(
                domain.getScheduledTripId().value(), domain.getSeatId().value()));
        entity.setStatus(domain.getStatus().name());
        entity.setBookingId(domain.getBookingId());
        entity.setPriceAtBooking(
                domain.getPriceAtBooking() != null ? domain.getPriceAtBooking().toLong() : null);
        entity.setVersion(domain.getVersion());
        return entity;
    }
}

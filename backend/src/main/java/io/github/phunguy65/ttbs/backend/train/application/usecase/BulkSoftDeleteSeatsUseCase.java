package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.helper.ForceBookingCancellationHelper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteSeatsUseCase {

    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final ForceBookingCancellationHelper forceBookingCancellationHelper;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteSeatsUseCase(
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            ForceBookingCancellationHelper forceBookingCancellationHelper,
            ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
        this.forceBookingCancellationHelper = forceBookingCancellationHelper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, SeatError> execute(BulkSoftDeleteSeatsCommand command) {
        // Cancel any active bookings for the affected seats before deleting.
        // The query returns DISTINCT booking IDs so each booking is cancelled at most once,
        // even when a booking covers multiple seats in the command list.
        List<UUID> activeBookingIds =
                availabilityRepository.findDistinctActiveBookingIdsBySeatIds(command.seatIds());
        for (UUID bookingId : activeBookingIds) {
            forceBookingCancellationHelper.cancel(bookingId);
        }

        Instant now = Instant.now();
        int affected = seatRepository.softDeleteByIds(command.seatIds(), now);

        if (affected > 0) {
            eventPublisher.publishEvent(SeatsDeleted.of(command.seatIds(), now));
        }

        return Result.success(affected);
    }
}

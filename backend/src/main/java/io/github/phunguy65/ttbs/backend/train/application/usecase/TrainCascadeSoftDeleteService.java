package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.helper.BulkForceBookingCancellationHelper;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class TrainCascadeSoftDeleteService {

    private final RouteRepository routeRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final TrainRepository trainRepository;
    private final BulkForceBookingCancellationHelper bulkForceBookingCancellationHelper;

    TrainCascadeSoftDeleteService(
            RouteRepository routeRepository,
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            TrainRepository trainRepository,
            BulkForceBookingCancellationHelper bulkForceBookingCancellationHelper) {
        this.routeRepository = routeRepository;
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
        this.trainRepository = trainRepository;
        this.bulkForceBookingCancellationHelper = bulkForceBookingCancellationHelper;
    }

    int execute(List<TrainId> trainIds, Instant deletedAt) {
        if (trainIds.isEmpty()) {
            return 0;
        }

        List<RouteId> routeIds = routeRepository.findActiveIdsByTrainIds(trainIds);
        List<CoachId> coachIds = coachRepository.findActiveIdsByTrainIds(trainIds);
        List<SeatId> seatIds =
                coachIds.isEmpty() ? List.of() : seatRepository.findActiveIdsByCoachIds(coachIds);
        List<UUID> bookingIds = seatIds.isEmpty()
                ? List.of()
                : availabilityRepository.findDistinctActiveBookingIdsBySeatIds(seatIds);

        bulkForceBookingCancellationHelper.cancelAll(bookingIds);

        if (!seatIds.isEmpty()) {
            availabilityRepository.hardDeleteBySeatIds(seatIds);
            seatRepository.softDeleteByIds(seatIds, deletedAt);
        }
        if (!routeIds.isEmpty()) {
            routeRepository.softDeleteByIds(routeIds, deletedAt);
        }
        if (!coachIds.isEmpty()) {
            coachRepository.softDeleteByIds(coachIds, deletedAt);
        }

        return trainRepository.softDeleteByIds(trainIds, deletedAt);
    }
}

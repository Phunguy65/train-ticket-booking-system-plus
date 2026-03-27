package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainCascadeSoftDeleteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RouteSeatAvailabilityRepository availabilityRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private BulkForceBookingCancellationHelper bulkForceBookingCancellationHelper;

    @InjectMocks
    private TrainCascadeSoftDeleteService service;

    @Test
    void executeCancelsBookingsAndSoftDeletesEntireTrainTree() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        RouteId routeId = RouteId.of(UUID.randomUUID());
        CoachId coachId = CoachId.of(UUID.randomUUID());
        SeatId seatId = SeatId.of(UUID.randomUUID());
        UUID bookingId = UUID.randomUUID();
        Instant deletedAt = Instant.now();

        when(routeRepository.findActiveIdsByTrainIds(List.of(trainId)))
                .thenReturn(List.of(routeId));
        when(coachRepository.findActiveIdsByTrainIds(List.of(trainId)))
                .thenReturn(List.of(coachId));
        when(seatRepository.findActiveIdsByCoachIds(List.of(coachId))).thenReturn(List.of(seatId));
        when(availabilityRepository.findDistinctActiveBookingIdsBySeatIds(List.of(seatId)))
                .thenReturn(List.of(bookingId));
        when(trainRepository.softDeleteByIds(List.of(trainId), deletedAt)).thenReturn(1);

        int affected = service.execute(List.of(trainId), deletedAt);

        assertThat(affected).isEqualTo(1);

        InOrder inOrder = inOrder(
                routeRepository,
                coachRepository,
                seatRepository,
                availabilityRepository,
                bulkForceBookingCancellationHelper,
                routeRepository,
                coachRepository,
                seatRepository,
                trainRepository);
        inOrder.verify(routeRepository).findActiveIdsByTrainIds(List.of(trainId));
        inOrder.verify(coachRepository).findActiveIdsByTrainIds(List.of(trainId));
        inOrder.verify(seatRepository).findActiveIdsByCoachIds(List.of(coachId));
        inOrder.verify(availabilityRepository)
                .findDistinctActiveBookingIdsBySeatIds(List.of(seatId));
        inOrder.verify(bulkForceBookingCancellationHelper).cancelAll(List.of(bookingId));
        inOrder.verify(availabilityRepository).hardDeleteBySeatIds(List.of(seatId));
        inOrder.verify(seatRepository).softDeleteByIds(List.of(seatId), deletedAt);
        inOrder.verify(routeRepository).softDeleteByIds(List.of(routeId), deletedAt);
        inOrder.verify(coachRepository).softDeleteByIds(List.of(coachId), deletedAt);
        inOrder.verify(trainRepository).softDeleteByIds(List.of(trainId), deletedAt);
    }

    @Test
    void executeSkipsChildQueriesWhenTrainIdsEmpty() {
        assertThat(service.execute(List.of(), Instant.now())).isZero();

        verifyNoInteractions(
                routeRepository,
                coachRepository,
                seatRepository,
                availabilityRepository,
                trainRepository,
                bulkForceBookingCancellationHelper);
    }

    @Test
    void executeHandlesMultipleTrainsAndChildren() {
        TrainId trainIdOne = TrainId.of(UUID.randomUUID());
        TrainId trainIdTwo = TrainId.of(UUID.randomUUID());
        List<TrainId> trainIds = List.of(trainIdOne, trainIdTwo);
        List<RouteId> routeIds =
                List.of(RouteId.of(UUID.randomUUID()), RouteId.of(UUID.randomUUID()));
        List<CoachId> coachIds =
                List.of(CoachId.of(UUID.randomUUID()), CoachId.of(UUID.randomUUID()));
        List<SeatId> seatIds = List.of(SeatId.of(UUID.randomUUID()), SeatId.of(UUID.randomUUID()));
        List<UUID> bookingIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Instant deletedAt = Instant.now();

        when(routeRepository.findActiveIdsByTrainIds(trainIds)).thenReturn(routeIds);
        when(coachRepository.findActiveIdsByTrainIds(trainIds)).thenReturn(coachIds);
        when(seatRepository.findActiveIdsByCoachIds(coachIds)).thenReturn(seatIds);
        when(availabilityRepository.findDistinctActiveBookingIdsBySeatIds(seatIds))
                .thenReturn(bookingIds);
        when(trainRepository.softDeleteByIds(trainIds, deletedAt)).thenReturn(2);

        int affected = service.execute(trainIds, deletedAt);

        assertThat(affected).isEqualTo(2);
        verify(bulkForceBookingCancellationHelper).cancelAll(bookingIds);
        verify(availabilityRepository).hardDeleteBySeatIds(seatIds);
        verify(seatRepository).softDeleteByIds(seatIds, deletedAt);
        verify(routeRepository).softDeleteByIds(routeIds, deletedAt);
        verify(coachRepository).softDeleteByIds(coachIds, deletedAt);
        verify(trainRepository).softDeleteByIds(trainIds, deletedAt);
    }
}

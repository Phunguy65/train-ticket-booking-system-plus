package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.SeatCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Keeps {@link Train#getTotalSeats()} and {@link Coach#getTotalSeats()} in sync with the actual
 * number of active seats. Listens to seat lifecycle events and recalculates the counts via DB queries.
 */
@Service
public class OnSeatCountChangedListener {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final TrainRepository trainRepository;

    public OnSeatCountChangedListener(
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            TrainRepository trainRepository) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.trainRepository = trainRepository;
    }

    @ApplicationModuleListener
    public void onSeatCreated(SeatCreated event) {
        recalculateForCoach(event.coachId().value());
    }

    @ApplicationModuleListener
    public void onSeatsCreated(SeatsCreated event) {
        recalculateForCoach(event.coachId().value());
    }

    @ApplicationModuleListener
    public void onSeatDeleted(SeatDeleted event) {
        recalculateForCoach(event.coachId().value());
    }

    @ApplicationModuleListener
    public void onSeatsDeleted(SeatsDeleted event) {
        List<TrainId> trainIds = seatRepository.findDistinctTrainIdsBySeatIds(event.seatIds());
        for (TrainId trainId : trainIds) {
            recalculateForTrain(trainId);
        }
        List<CoachId> coachIds = seatRepository.findDistinctCoachIdsBySeatIds(event.seatIds());
        for (CoachId coachId : coachIds) {
            recalculateCoachSeats(coachId);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void recalculateForCoach(UUID coachId) {
        CoachId cid = CoachId.of(coachId);
        recalculateCoachSeats(cid);
        coachRepository.findById(cid).map(Coach::getTrainId).ifPresent(this::recalculateForTrain);
    }

    private void recalculateCoachSeats(CoachId coachId) {
        coachRepository.findById(coachId).ifPresent(coach -> {
            int count = seatRepository.countActiveByCoachId(coachId);
            coach.updateTotalSeats(count);
            coachRepository.save(coach);
        });
    }

    private void recalculateForTrain(TrainId trainId) {
        trainRepository.findById(trainId).ifPresent(train -> {
            int count = seatRepository.countActiveByTrainId(trainId);
            train.updateTotalSeats(count);
            trainRepository.save(train);
        });
    }
}

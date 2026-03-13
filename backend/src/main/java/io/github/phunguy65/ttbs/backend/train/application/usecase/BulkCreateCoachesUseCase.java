package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkCreateCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkCreateCoachesUseCase {

    private final TrainRepository trainRepository;
    private final CoachRepository coachRepository;

    public BulkCreateCoachesUseCase(
            TrainRepository trainRepository, CoachRepository coachRepository) {
        this.trainRepository = trainRepository;
        this.coachRepository = coachRepository;
    }

    @Transactional
    public Result<List<CoachResponse>, CoachError> execute(BulkCreateCoachesCommand command) {
        TrainId trainId = TrainId.of(command.trainId());

        if (trainRepository.findById(trainId).isEmpty()) {
            return Result.failure(new CoachError.TrainNotFound());
        }

        List<Integer> requestedCarNumbers = command.coaches().stream()
                .map(BulkCreateCoachesCommand.CoachItem::carNumber)
                .toList();
        List<Integer> inRequestDuplicates = findDuplicates(requestedCarNumbers);
        if (!inRequestDuplicates.isEmpty()) {
            return Result.failure(new CoachError.DuplicateCarNumbersInRequest(inRequestDuplicates));
        }

        Set<Integer> existingCarNumbers = new HashSet<>();
        coachRepository
                .findByTrainId(trainId)
                .forEach(c -> existingCarNumbers.add(c.getCarNumber()));

        List<Integer> conflicting = requestedCarNumbers.stream()
                .filter(existingCarNumbers::contains)
                .toList();
        if (!conflicting.isEmpty()) {
            return Result.failure(new CoachError.CarNumbersAlreadyExist(conflicting));
        }

        List<Coach> coaches = command.coaches().stream()
                .map(item -> Coach.create(
                        CoachId.of(UuidGenerator.generate()),
                        trainId,
                        item.carNumber(),
                        item.totalSeats()))
                .toList();

        List<Coach> saved = coachRepository.saveAll(coaches);

        return Result.success(saved.stream().map(this::toDto).toList());
    }

    private List<Integer> findDuplicates(List<Integer> values) {
        Set<Integer> seen = new HashSet<>();
        List<Integer> duplicates = new ArrayList<>();
        for (Integer value : values) {
            if (!seen.add(value)) {
                duplicates.add(value);
            }
        }
        return duplicates;
    }

    private CoachResponse toDto(Coach coach) {
        return new CoachResponse(
                coach.getId().value(),
                coach.getTrainId().value(),
                coach.getCarNumber(),
                coach.getTotalSeats(),
                coach.getCreatedAt());
    }
}

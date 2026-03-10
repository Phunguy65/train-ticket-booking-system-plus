package io.github.phunguy65.ttbs.backend.train.application.command;

import java.util.List;
import java.util.UUID;

/**
 * Command for bulk-creating coaches under a train.
 *
 * @param trainId the ID of the parent train
 * @param coaches the list of coaches to create
 */
public record BulkCreateCoachesCommand(UUID trainId, List<CoachItem> coaches) {

    /**
     * A single coach item within a bulk-create request.
     *
     * @param carNumber  the car number (must be unique within the train)
     * @param totalSeats the total number of seats in this coach
     */
    public record CoachItem(int carNumber, int totalSeats) {}
}

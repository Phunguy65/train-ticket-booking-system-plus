package io.github.phunguy65.ttbs.backend.train.application.command;

import java.util.List;
import java.util.UUID;

/**
 * Command for bulk-creating seats under a coach.
 *
 * @param coachId the ID of the parent coach
 * @param seats   the list of seats to create
 */
public record BulkCreateSeatsCommand(UUID coachId, List<SeatItem> seats) {

    /**
     * A single seat item within a bulk-create request.
     *
     * @param seatNumber the seat number (must be unique within the coach)
     */
    public record SeatItem(String seatNumber) {}
}

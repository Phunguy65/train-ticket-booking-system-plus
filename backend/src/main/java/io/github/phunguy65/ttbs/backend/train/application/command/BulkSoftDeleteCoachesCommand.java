package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import java.util.List;

public record BulkSoftDeleteCoachesCommand(List<CoachId> coachIds) {}

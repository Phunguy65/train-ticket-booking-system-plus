package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;

public record SoftDeleteCoachCommand(CoachId coachId, TrainId trainId) {}

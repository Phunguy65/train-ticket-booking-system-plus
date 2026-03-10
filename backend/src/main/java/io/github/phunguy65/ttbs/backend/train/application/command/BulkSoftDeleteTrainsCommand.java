package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.util.List;

public record BulkSoftDeleteTrainsCommand(List<TrainId> trainIds) {}

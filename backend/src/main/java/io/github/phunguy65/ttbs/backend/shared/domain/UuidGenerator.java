package io.github.phunguy65.ttbs.backend.shared.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** Centralized UUID v7 generator. Produces time-ordered, monotonically increasing UUIDs. */
public final class UuidGenerator {

    private UuidGenerator() {}

    /**
     * Generates a UUID v7 (time-ordered epoch). Uses Method 2 (PlusN) to guarantee monotonic
     * ordering even when multiple UUIDs are generated within the same millisecond.
     *
     * @return a new UUID v7
     */
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpochPlusN();
    }
}

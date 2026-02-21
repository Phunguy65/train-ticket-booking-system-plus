package io.github.phunguy65.ttbs.backend.shared.infrastructure;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}

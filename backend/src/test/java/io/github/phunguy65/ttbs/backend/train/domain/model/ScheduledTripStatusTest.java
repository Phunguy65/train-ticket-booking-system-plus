package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScheduledTripStatusTest {

    @Test
    void enumContainsExpectedOperationalLifecycle() {
        assertThat(ScheduledTripStatus.values())
                .containsExactly(
                        ScheduledTripStatus.SCHEDULED,
                        ScheduledTripStatus.BOARDING,
                        ScheduledTripStatus.DEPARTED,
                        ScheduledTripStatus.ARRIVED,
                        ScheduledTripStatus.CANCELLED);
    }
}

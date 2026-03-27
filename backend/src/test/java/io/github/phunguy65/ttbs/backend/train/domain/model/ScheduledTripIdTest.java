package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduledTripIdTest {

    @Test
    void constructorRejectsNullValue() {
        assertThatThrownBy(() -> new ScheduledTripId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ScheduledTripId value must not be null");
    }

    @Test
    void toStringReturnsWrappedUuid() {
        UUID value = UUID.randomUUID();

        assertThat(ScheduledTripId.of(value).toString()).isEqualTo(value.toString());
    }
}

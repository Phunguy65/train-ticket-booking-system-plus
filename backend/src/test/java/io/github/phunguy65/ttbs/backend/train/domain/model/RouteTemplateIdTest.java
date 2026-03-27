package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteTemplateIdTest {

    @Test
    void constructorRejectsNullValue() {
        assertThatThrownBy(() -> new RouteTemplateId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("RouteTemplateId value must not be null");
    }

    @Test
    void toStringReturnsWrappedUuid() {
        UUID value = UUID.randomUUID();

        assertThat(RouteTemplateId.of(value).toString()).isEqualTo(value.toString());
    }
}

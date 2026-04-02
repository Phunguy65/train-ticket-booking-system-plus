package io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CursorEncoderTest {

    private final CursorEncoder encoder = new CursorEncoder(new ObjectMapper());

    @Test
    void encodeAndDecodeRoundTripCursorPayload() {
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "650000", UUID.fromString("11111111-1111-1111-1111-111111111111"));

        String encoded = encoder.encode(cursor);
        SearchScheduledTripsCursor decoded =
                encoder.decode(encoded, SearchScheduledTripsCursor.class);

        assertThat(encoded).matches("^[A-Za-z0-9_-]+$");
        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    void decodeRejectsMalformedCursor() {
        assertThatThrownBy(() ->
                        encoder.decode("not-a-valid-cursor!!!", SearchScheduledTripsCursor.class))
                .isInstanceOf(InvalidCursorException.class)
                .hasMessage("The supplied cursor is invalid");
    }
}

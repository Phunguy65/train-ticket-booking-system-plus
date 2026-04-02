package io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor;

import io.github.phunguy65.ttbs.backend.shared.application.port.CursorCodec;
import java.util.Base64;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CursorEncoder implements CursorCodec {

    private final ObjectMapper objectMapper;

    public CursorEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> String encode(T value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (RuntimeException ex) {
            throw new InvalidCursorException("Failed to encode cursor", ex);
        }
    }

    @Override
    public <T> T decode(String encodedValue, Class<T> type) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedValue);
            return objectMapper.readValue(decoded, type);
        } catch (RuntimeException ex) {
            throw new InvalidCursorException("The supplied cursor is invalid", ex);
        }
    }
}

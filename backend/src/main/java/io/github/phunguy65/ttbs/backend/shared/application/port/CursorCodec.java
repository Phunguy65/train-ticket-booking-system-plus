package io.github.phunguy65.ttbs.backend.shared.application.port;

public interface CursorCodec {

    <T> String encode(T value);

    <T> T decode(String encodedValue, Class<T> type);
}

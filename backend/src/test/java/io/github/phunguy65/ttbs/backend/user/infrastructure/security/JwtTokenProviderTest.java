package io.github.phunguy65.ttbs.backend.user.infrastructure.security;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    private static final String VALID_SECRET =
            "a-very-long-test-secret-key-that-is-at-least-32-chars-long-for-hs256";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(provider, "accessTokenExpirySeconds", 900L);
        provider.init();
    }

    private User makeUser() {
        return User.reconstitute(
                UserId.of(UUID.randomUUID()),
                "test@example.com",
                "$2a$12$hash",
                "Test User",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now());
    }

    @Test
    void generateAndValidateToken_shouldRoundtrip() {
        User user = makeUser();
        String token = provider.generateAccessToken(user);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        User user = makeUser();
        String token = provider.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(
                shortLivedProvider, "accessTokenExpirySeconds", -1L); // already expired
        shortLivedProvider.init();

        User user = makeUser();
        String token = shortLivedProvider.generateAccessToken(user);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldProduceUniqueTokens() {
        String t1 = provider.generateRefreshToken();
        String t2 = provider.generateRefreshToken();

        assertThat(t1).isNotBlank();
        assertThat(t2).isNotBlank();
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void init_shortSecret_shouldThrowIllegalStateException() {
        JwtTokenProvider badProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(badProvider, "secret", "tooshort");
        ReflectionTestUtils.setField(badProvider, "accessTokenExpirySeconds", 900L);

        assertThatThrownBy(badProvider::init).isInstanceOf(IllegalStateException.class);
    }
}

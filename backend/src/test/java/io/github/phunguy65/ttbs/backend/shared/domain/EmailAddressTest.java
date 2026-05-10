package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmailAddress")
class EmailAddressTest {

    @Nested
    @DisplayName("validation — rejects invalid inputs")
    class Validation {

        @Test
        @DisplayName("rejects null with NullPointerException")
        void rejectsNull() {
            assertThatThrownBy(() -> EmailAddress.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects blank string")
        void rejectsBlank() {
            assertThatThrownBy(() -> EmailAddress.of("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects email without @")
        void rejectsMissingAtSign() {
            assertThatThrownBy(() -> EmailAddress.of("userexample.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects email starting with @")
        void rejectsStartingWithAtSign() {
            assertThatThrownBy(() -> EmailAddress.of("@example.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects email ending with @")
        void rejectsEndingWithAtSign() {
            assertThatThrownBy(() -> EmailAddress.of("user@"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("normalizes to lowercase")
        void normalizesToLowercase() {
            EmailAddress email = EmailAddress.of("User@Example.COM");

            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("trims leading and trailing whitespace")
        void trimsWhitespace() {
            EmailAddress email = EmailAddress.of("  user@example.com  ");

            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("accepts valid email")
        void acceptsValidEmail() {
            EmailAddress email = EmailAddress.of("valid.user+tag@sub.example.co");

            assertThat(email.value()).isEqualTo("valid.user+tag@sub.example.co");
        }
    }
}

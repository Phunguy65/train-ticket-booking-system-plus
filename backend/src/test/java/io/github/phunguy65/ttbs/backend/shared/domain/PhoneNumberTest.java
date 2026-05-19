package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PhoneNumber")
class PhoneNumberTest {

    @Nested
    @DisplayName("validation — rejects invalid inputs")
    class Validation {

        @Test
        @DisplayName("rejects null with NullPointerException")
        void rejectsNull() {
            assertThatThrownBy(() -> PhoneNumber.of(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects number too short (< 8 digits after normalization)")
        void rejectsTooShort() {
            assertThatThrownBy(() -> PhoneNumber.of("123456"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects number too long (> 20 chars after normalization)")
        void rejectsTooLong() {
            assertThatThrownBy(() -> PhoneNumber.of("123456789012345678901"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("removes spaces, dashes, and parentheses")
        void removesSpacesDashesAndParens() {
            PhoneNumber phone = PhoneNumber.of("(090) 123-4567");

            assertThat(phone.value()).isEqualTo("0901234567");
        }

        @Test
        @DisplayName("preserves + prefix")
        void preservesPlusPrefix() {
            PhoneNumber phone = PhoneNumber.of("+84 901 234 567");

            assertThat(phone.value()).isEqualTo("+84901234567");
        }
    }

    @Nested
    @DisplayName("ofNullable()")
    class OfNullable {

        @Test
        @DisplayName("returns null for null input")
        void ofNullable_returnsNullForNull() {
            assertThat(PhoneNumber.ofNullable(null)).isNull();
        }

        @Test
        @DisplayName("returns null for blank input")
        void ofNullable_returnsNullForBlank() {
            assertThat(PhoneNumber.ofNullable("   ")).isNull();
        }

        @Test
        @DisplayName("returns PhoneNumber for valid input")
        void ofNullable_returnsPhoneNumberForValidInput() {
            PhoneNumber phone = PhoneNumber.ofNullable("0901234567");

            assertThat(phone).isNotNull();
            assertThat(phone.value()).isEqualTo("0901234567");
        }
    }
}

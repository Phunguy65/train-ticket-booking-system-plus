package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PersonName")
class PersonNameTest {

    @Nested
    @DisplayName("validation — rejects invalid inputs")
    class Validation {

        @Test
        @DisplayName("rejects null with NullPointerException")
        void rejectsNull() {
            assertThatThrownBy(() -> PersonName.of(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects blank string")
        void rejectsBlank() {
            assertThatThrownBy(() -> PersonName.of("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects name longer than 255 characters")
        void rejectsNameLongerThan255Chars() {
            String tooLong = "A".repeat(256);

            assertThatThrownBy(() -> PersonName.of(tooLong))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("normalizes multiple whitespace to single space")
        void normalizesMultipleWhitespace() {
            PersonName name = PersonName.of("Nguyen  Van   A");

            assertThat(name.value()).isEqualTo("Nguyen Van A");
        }

        @Test
        @DisplayName("trims leading and trailing whitespace")
        void trimsLeadingAndTrailingWhitespace() {
            PersonName name = PersonName.of("  Nguyen Van A  ");

            assertThat(name.value()).isEqualTo("Nguyen Van A");
        }

        @Test
        @DisplayName("accepts name with exactly 255 characters after normalization")
        void accepts255CharName() {
            String exactly255 = "A".repeat(255);

            PersonName name = PersonName.of(exactly255);

            assertThat(name.value()).hasSize(255);
        }
    }
}

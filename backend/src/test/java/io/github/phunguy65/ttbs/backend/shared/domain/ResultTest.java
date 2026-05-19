package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Result")
class ResultTest {

    @Nested
    @DisplayName("success(value)")
    class SuccessWithValue {

        @Test
        @DisplayName("isSuccess=true, isFailure=false, holds value")
        void successWithValue_isSuccessAndHoldsValue() {
            Result<String, Integer> result = Result.success("hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
            assertThat(((Result.Success<String, Integer>) result).value()).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("success() — void overload")
    class SuccessVoid {

        @Test
        @DisplayName("isSuccess=true with null value")
        void successVoid_isSuccess() {
            Result<Void, String> result = Result.success();

            assertThat(result.isSuccess()).isTrue();
            assertThat(((Result.Success<Void, String>) result).value()).isNull();
        }
    }

    @Nested
    @DisplayName("failure(error)")
    class Failure {

        @Test
        @DisplayName("isSuccess=false, isFailure=true, holds error")
        void failure_isFailureAndHoldsError() {
            Result<String, Integer> result = Result.failure(42);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<String, Integer>) result).error()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("map()")
    class Map {

        @Test
        @DisplayName("transforms success value")
        void map_transformsSuccessValue() {
            Result<String, Integer> success = Result.success("hello");
            var result = success.map(String::length);

            assertThat(result.isSuccess()).isTrue();
            assertThat(((Result.Success<?, ?>) result).value()).isEqualTo(5);
        }

        @Test
        @DisplayName("leaves failure untouched")
        void map_leavesFailureUntouched() {
            Result<String, Integer> failure = Result.failure(99);
            Result<Integer, Integer> mapped = failure.map(String::length);

            assertThat(mapped.isFailure()).isTrue();
            assertThat(((Result.Failure<Integer, Integer>) mapped).error()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("fold()")
    class Fold {

        @Test
        @DisplayName("applies onSuccess for success")
        void fold_appliesOnSuccessForSuccess() {
            Result<String, Integer> result = Result.success("world");

            String folded = result.fold(v -> "ok:" + v, e -> "err:" + e);

            assertThat(folded).isEqualTo("ok:world");
        }

        @Test
        @DisplayName("applies onFailure for failure")
        void fold_appliesOnFailureForFailure() {
            Result<String, Integer> result = Result.failure(7);

            String folded = result.fold(v -> "ok:" + v, e -> "err:" + e);

            assertThat(folded).isEqualTo("err:7");
        }
    }
}

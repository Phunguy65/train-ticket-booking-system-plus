package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class GetUserBookingsRequestTest {

    private final jakarta.validation.Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsToFirstPageWithTwentyItems() {
        GetUserBookingsRequest request = new GetUserBookingsRequest();

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void rejectsNegativePage() {
        assertThat(validator.validate(new GetUserBookingsRequest(-1, 20)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("page");
    }

    @Test
    void rejectsInvalidSizes() {
        assertThat(validator.validate(new GetUserBookingsRequest(0, 0)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("size");
        assertThat(validator.validate(new GetUserBookingsRequest(0, 101)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("size");
    }
}

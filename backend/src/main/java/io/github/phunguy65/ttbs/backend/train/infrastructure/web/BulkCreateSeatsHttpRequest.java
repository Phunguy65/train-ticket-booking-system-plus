package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

record BulkCreateSeatsHttpRequest(
        @NotEmpty(message = "At least one seat must be provided") @Size(max = 100, message = "Bulk create is limited to 100 seats per request") List<@Valid SeatItemRequest> seats) {

    record SeatItemRequest(
            @NotBlank(message = "Seat number must not be blank") @Size(max = 10, message = "Seat number must not exceed 10 characters") String seatNumber) {}
}

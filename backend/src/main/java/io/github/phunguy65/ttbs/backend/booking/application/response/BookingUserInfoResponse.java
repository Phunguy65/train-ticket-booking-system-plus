package io.github.phunguy65.ttbs.backend.booking.application.response;

import java.time.LocalDate;

public record BookingUserInfoResponse(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine) {}

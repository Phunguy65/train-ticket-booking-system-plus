package io.github.phunguy65.ttbs.backend.booking.domain.projection;

import java.time.LocalDate;

public record BookingUserInfoSummary(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine) {}

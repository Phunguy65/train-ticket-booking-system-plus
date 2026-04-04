package io.github.phunguy65.ttbs.backend.booking.application.response;

import java.time.LocalDate;

public record PassengerInfoResponse(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine) {}

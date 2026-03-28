package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.time.LocalDate;

public record BookingUserInfoSnapshotJson(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine) {}

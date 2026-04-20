package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Value object representing a passenger assigned to a specific seat in a booking.
 */
public record BookingPassenger(
        SeatId seatId,
        String fullName,
        String idDocumentNumber,
        LocalDate dateOfBirth,
        String gender)
        implements ValueObject {

    public BookingPassenger {
        Objects.requireNonNull(seatId, "seatId must not be null");
        PersonName.of(fullName);
        idDocumentNumber = toValue(IdDocumentNumber.of(idDocumentNumber));
        Objects.requireNonNull(dateOfBirth, "dateOfBirth must not be null");
        gender = toValue(Gender.of(gender));
    }

    public static BookingPassenger of(
            SeatId seatId,
            String fullName,
            String idDocumentNumber,
            LocalDate dateOfBirth,
            String gender) {
        return new BookingPassenger(seatId, fullName, idDocumentNumber, dateOfBirth, gender);
    }

    private static String toValue(Gender gender) {
        return gender == null ? null : gender.value();
    }

    private static String toValue(IdDocumentNumber idDocumentNumber) {
        return idDocumentNumber == null ? null : idDocumentNumber.value();
    }
}

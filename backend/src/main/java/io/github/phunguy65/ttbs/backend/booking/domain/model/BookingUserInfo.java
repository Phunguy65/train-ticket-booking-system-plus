package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.time.LocalDate;

public record BookingUserInfo(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine)
        implements ValueObject {

    public BookingUserInfo {
        PersonName.of(fullName);
        EmailAddress.of(email);
        PhoneNumber.ofNullable(phone);
        gender = toValue(Gender.ofNullable(gender));
        idDocumentNumber = toValue(IdDocumentNumber.ofNullable(idDocumentNumber));
        addressLine = toValue(AddressLine.ofNullable(addressLine));
    }

    public static BookingUserInfo of(
            String fullName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String gender,
            String idDocumentNumber,
            String addressLine) {
        return new BookingUserInfo(
                fullName, email, phone, dateOfBirth, gender, idDocumentNumber, addressLine);
    }

    private static String toValue(Gender gender) {
        return gender == null ? null : gender.value();
    }

    private static String toValue(IdDocumentNumber idDocumentNumber) {
        return idDocumentNumber == null ? null : idDocumentNumber.value();
    }

    private static String toValue(AddressLine addressLine) {
        return addressLine == null ? null : addressLine.value();
    }
}

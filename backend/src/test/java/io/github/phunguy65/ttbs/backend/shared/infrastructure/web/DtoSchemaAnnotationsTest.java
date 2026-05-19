package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoSchemaAnnotationsTest {

    @Test
    void requestRecordsExposeSchemaMetadataAndSensitiveInputHandling() throws Exception {
        for (String className : List.of(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest",
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.LoginRequest",
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest",
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest",
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest",
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest",
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetBookingDetailRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetTrainsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetTrainByIdRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachesRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachByIdRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetSeatsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetAvailableSeatsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachSeatMapRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRouteTemplatesRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRouteTemplateByIdRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripByIdRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest",
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest",
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationByIdRequest",
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.SearchStationsRequest",
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByIdRequest",
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByBookingIdRequest")) {
            assertRecordAndComponentsAnnotated(loadClass(className));
        }

        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest",
                                "password"))
                        .writeOnly())
                .isTrue();
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.LoginRequest",
                                "password"))
                        .writeOnly())
                .isTrue();
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest",
                                "refreshToken"))
                        .writeOnly())
                .isTrue();
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest",
                                "idempotencyKey"))
                        .writeOnly())
                .isTrue();
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest",
                                "idDocumentNumber"))
                        .writeOnly())
                .isTrue();
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest",
                                "addressLine"))
                        .example())
                .isEqualTo("redacted-address");

        assertThat(arraySchema(component(
                                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest",
                                "seatIds"))
                        .minItems())
                .isEqualTo(1);
    }

    @Test
    void responseRecordsExposeFieldMetadataFormatsAndConservativeExamples() throws Exception {
        for (String className : List.of(
                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                "io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.PaymentDetailResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse$Trip",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse$Seat",
                "io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse$Seat",
                "io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Train",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Route",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Station",
                "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse",
                "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Train",
                "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Route",
                "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Station",
                "io.github.phunguy65.ttbs.backend.station.application.response.StationResponse",
                "io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse",
                "io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse")) {
            assertRecordAndComponentsAnnotated(loadClass(className));
        }

        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                                "id"))
                        .format())
                .isEqualTo("uuid");
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                                "createdAt"))
                        .format())
                .isEqualTo("date-time");
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                                "id"))
                        .accessMode())
                .isEqualTo(Schema.AccessMode.READ_ONLY);
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse",
                                "accessToken"))
                        .accessMode())
                .isEqualTo(Schema.AccessMode.READ_ONLY);
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse",
                                "refreshToken"))
                        .accessMode())
                .isEqualTo(Schema.AccessMode.READ_ONLY);
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                                "idDocumentNumber"))
                        .example())
                .isEqualTo("redacted-id-document");
        assertThat(schema(component(
                                "io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse",
                                "addressLine"))
                        .example())
                .isEqualTo("redacted-address");

        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.PaymentDetailResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse$Trip"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse$Seat"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse$Seat"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Train"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Route"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse$Station"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Train"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Route"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse$Station"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.station.application.response.StationResponse"));
        assertFormatsByType(
                loadClass(
                        "io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse"));
        assertFormatsByType(loadClass(
                "io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse"));
    }

    @Test
    void sharedPayloadAndPaginationSchemasRemainFullyAnnotated() throws Exception {
        for (String className : List.of(
                "io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData",
                "io.github.phunguy65.ttbs.backend.shared.infrastructure.web.Violation",
                "io.github.phunguy65.ttbs.backend.shared.domain.PageResponse",
                "io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse",
                "io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse")) {
            assertRecordAndComponentsAnnotated(loadClass(className));
        }

        assertThat(loadClass("io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode")
                        .isAnnotationPresent(Schema.class))
                .isTrue();
        assertThat(loadClass(
                                "io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ViolationCode")
                        .isAnnotationPresent(Schema.class))
                .isTrue();

        assertThat(arraySchema(component(
                        "io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData",
                        "errors")))
                .isNotNull();
        assertThat(arraySchema(component(
                        "io.github.phunguy65.ttbs.backend.shared.domain.PageResponse", "content")))
                .isNotNull();
        assertThat(arraySchema(component(
                        "io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse", "content")))
                .isNotNull();
    }

    @Test
    void paginatedRequestsCarryConstraintAndSchemaMetadata() throws Exception {
        for (String className : List.of(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetTrainsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachesRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetSeatsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetAvailableSeatsRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachSeatMapRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRouteTemplatesRequest",
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest",
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest")) {
            Field pageField = component(className, "page");
            Field sizeField = component(className, "size");

            assertThat(pageField.getAnnotation(Min.class)).isNotNull();
            assertThat(schema(pageField)).isNotNull();
            assertThat(pageField.getAnnotation(Min.class).value()).isEqualTo(0);
            assertThat(schema(pageField).minimum()).isEqualTo("0");

            assertThat(sizeField.getAnnotation(Min.class)).isNotNull();
            assertThat(sizeField.getAnnotation(Max.class)).isNotNull();
            assertThat(schema(sizeField)).isNotNull();
            assertThat(sizeField.getAnnotation(Min.class).value()).isEqualTo(1);
            assertThat(sizeField.getAnnotation(Max.class).value()).isGreaterThanOrEqualTo(20);
            assertThat(schema(sizeField).minimum()).isEqualTo("1");
            assertThat(schema(sizeField).maximum()).isNotBlank();
        }
    }

    @Test
    void cursorBasedRequestsDocumentCursorSemantics() throws Exception {
        Field cursorField = component(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest",
                "cursor");
        Field sizeField = component(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest",
                "size");

        assertThat(schema(cursorField)).isNotNull();
        assertThat(sizeField.getAnnotation(Min.class)).isNotNull();
        assertThat(sizeField.getAnnotation(Max.class)).isNotNull();
        assertThat(schema(cursorField).description()).containsIgnoringCase("cursor");
        assertThat(schema(cursorField).example()).isEqualTo("opaque-cursor-token");
        assertThat(sizeField.getAnnotation(Min.class).value()).isEqualTo(1);
        assertThat(sizeField.getAnnotation(Max.class).value()).isEqualTo(50);
        assertThat(schema(sizeField).maximum()).isEqualTo("50");
    }

    private void assertRecordAndComponentsAnnotated(Class<?> type) {
        assertThat(type.isRecord()).isTrue();
        Schema recordSchema = type.getAnnotation(Schema.class);
        assertThat(recordSchema)
                .withFailMessage("%s missing record-level @Schema", type.getName())
                .isNotNull();
        assertThat(recordSchema.name())
                .withFailMessage("%s should not override the generated schema name", type.getName())
                .isBlank();
        assertThat(recordSchema.description()).isNotBlank();

        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }

            if (List.class.isAssignableFrom(field.getType())) {
                ArraySchema arraySchema = field.getAnnotation(ArraySchema.class);
                assertThat(arraySchema)
                        .withFailMessage(
                                "%s#%s missing @ArraySchema", type.getName(), field.getName())
                        .isNotNull();
                continue;
            }

            Schema schema = field.getAnnotation(Schema.class);
            assertThat(schema)
                    .withFailMessage("%s#%s missing @Schema", type.getName(), field.getName())
                    .isNotNull();
            assertThat(schema.description()).isNotBlank();
        }
    }

    private void assertFormatsByType(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            if (field.getType().equals(UUID.class)) {
                assertThat(schema(field).format()).isEqualTo("uuid");
            }
            if (field.getType().equals(Instant.class)) {
                assertThat(schema(field).format()).isEqualTo("date-time");
            }
            if (field.getType().equals(LocalDate.class)) {
                assertThat(schema(field).format()).isEqualTo("date");
            }
            if (field.getType().equals(BigDecimal.class)) {
                assertThat(schema(field).description()).isNotBlank();
            }
        }
    }

    private Field component(String className, String componentName) throws Exception {
        for (Field field : loadClass(className).getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            if (field.getName().equals(componentName)) {
                return field;
            }
        }
        throw new IllegalArgumentException(className + "#" + componentName + " not found");
    }

    private Schema schema(Field field) {
        Schema schema = field.getAnnotation(Schema.class);
        assertThat(schema).isNotNull();
        return schema;
    }

    private ArraySchema arraySchema(Field field) {
        ArraySchema arraySchema = field.getAnnotation(ArraySchema.class);
        assertThat(arraySchema).isNotNull();
        return arraySchema;
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }
}

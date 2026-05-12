package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

class ControllerContractAnnotationsTest {

    private static final Map<String, List<String>> PUBLIC_CONTROLLER_METHODS =
            new LinkedHashMap<>();

    static {
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                List.of("register", "login", "refresh", "logout", "me", "updateMe", "deleteMe"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                List.of("listByUser", "getById", "create", "cancel"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                List.of("list", "getById"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                List.of("getCoachesByTrain", "getCoachById"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                List.of("getSeatsByTrain", "getAvailableSeats", "getCoachSeatMap"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                List.of("list", "getById"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                List.of("list", "filter", "getById"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                List.of("list", "search", "getById"));
        PUBLIC_CONTROLLER_METHODS.put(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                List.of("getPaymentById", "getPaymentByBookingId"));
    }

    @Test
    void internalControllersAreHiddenFromCustomerContract() throws Exception {
        assertThat(loadClass(
                                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.sse.SeatEventController")
                        .isAnnotationPresent(Hidden.class))
                .isTrue();
        assertThat(loadClass(
                                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.StripeWebhookController")
                        .isAnnotationPresent(Hidden.class))
                .isTrue();
    }

    @Test
    void authenticatedEndpointsDeclareBearerSecurityAndHideAuthenticationInjection()
            throws Exception {
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "me",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "updateMe",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "deleteMe",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "listByUser",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "getById",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "create",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "cancel",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                "getPaymentById",
                "bearerAuth");
        assertSecurityAndHiddenAuthParam(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                "getPaymentByBookingId",
                "bearerAuth");
    }

    @Test
    void publicAuthEndpointsExplicitlyClearGlobalSecurityRequirement() throws Exception {
        assertThat(securityRequirementName(
                        "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                        "register"))
                .isEmpty();
        assertThat(securityRequirementName(
                        "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                        "login"))
                .isEmpty();
        assertThat(securityRequirementName(
                        "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                        "refresh"))
                .isEmpty();
        assertThat(securityRequirementName(
                        "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                        "logout"))
                .isEmpty();
    }

    @Test
    void successPayloadAnnotationsMatchExpectedContractShapes() throws Exception {
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "register",
                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                SuccessResponseKind.OBJECT,
                "201");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "login",
                "io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "refresh",
                "io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "logout",
                Void.class.getName(),
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "me",
                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "updateMe",
                "io.github.phunguy65.ttbs.backend.user.application.response.UserResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "deleteMe",
                Void.class.getName(),
                SuccessResponseKind.OBJECT,
                "");

        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "listByUser",
                "io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "getById",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "create",
                "io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse",
                SuccessResponseKind.OBJECT,
                "201");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "cancel",
                Void.class.getName(),
                SuccessResponseKind.OBJECT,
                "");

        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                "list",
                "io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                "getById",
                "io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                "getCoachesByTrain",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                "getCoachById",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                "getSeatsByTrain",
                "io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                "getAvailableSeats",
                "io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                "getCoachSeatMap",
                "io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                "list",
                "io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                "getById",
                "io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                "list",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                "filter",
                "io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse",
                SuccessResponseKind.SLICE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                "getById",
                "io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                "list",
                "io.github.phunguy65.ttbs.backend.station.application.response.StationResponse",
                SuccessResponseKind.PAGE,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                "search",
                "io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse",
                SuccessResponseKind.ARRAY,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                "getById",
                "io.github.phunguy65.ttbs.backend.station.application.response.StationResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                "getPaymentById",
                "io.github.phunguy65.ttbs.backend.payment.application.response.PaymentDetailResponse",
                SuccessResponseKind.OBJECT,
                "");
        assertSuccessPayload(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                "getPaymentByBookingId",
                "io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse",
                SuccessResponseKind.OBJECT,
                "");
    }

    @Test
    void trainControllersShareTheConfiguredTrainsTag() throws Exception {
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                "Trains");
    }

    @Test
    @DisplayName(
            "All public operations declare operation, responses, security, and parameter metadata")
    void publicOperationsDeclareCoreContractAnnotations() throws Exception {
        for (Map.Entry<String, List<String>> entry : PUBLIC_CONTROLLER_METHODS.entrySet()) {
            Class<?> controllerClass = loadClass(entry.getKey());
            for (String methodName : entry.getValue()) {
                Method method = method(controllerClass, methodName);

                Operation operation = method.getAnnotation(Operation.class);
                assertThat(operation)
                        .withFailMessage("%s#%s missing @Operation", entry.getKey(), methodName)
                        .isNotNull();
                assertThat(operation.operationId()).isNotBlank();
                assertThat(operation.summary()).isNotBlank();

                ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
                assertThat(apiResponses)
                        .withFailMessage("%s#%s missing @ApiResponses", entry.getKey(), methodName)
                        .isNotNull();
                assertThat(apiResponses.value()).isNotEmpty();

                assertThat(method.getAnnotation(SuccessPayload.class))
                        .withFailMessage(
                                "%s#%s missing @SuccessPayload", entry.getKey(), methodName)
                        .isNotNull();

                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    if (Authentication.class.isAssignableFrom(parameter.getType())) {
                        Parameter openApiParameter = parameter.getAnnotation(Parameter.class);
                        assertThat(openApiParameter)
                                .withFailMessage(
                                        "%s#%s auth parameter missing @Parameter(hidden = true)",
                                        entry.getKey(), methodName)
                                .isNotNull();
                        assertThat(openApiParameter.hidden()).isTrue();
                    }

                    if (parameter.isAnnotationPresent(PathVariable.class)) {
                        Parameter openApiParameter = parameter.getAnnotation(Parameter.class);
                        assertThat(openApiParameter)
                                .withFailMessage(
                                        "%s#%s path variable missing @Parameter description",
                                        entry.getKey(), methodName)
                                .isNotNull();
                        assertThat(openApiParameter.description()).isNotBlank();
                    }

                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        assertThat(parameter.isAnnotationPresent(jakarta.validation.Valid.class))
                                .withFailMessage(
                                        "%s#%s request body should remain @Valid",
                                        entry.getKey(), methodName)
                                .isTrue();
                    }
                }
            }
        }
    }

    @Test
    void publicControllersUseConfiguredTagTaxonomy() throws Exception {
        assertTag(
                "io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController",
                "Authentication");
        assertTag(
                "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                "Bookings");
        assertTag(
                "io.github.phunguy65.ttbs.backend.payment.infrastructure.web.PaymentController",
                "Payments");
        assertTag(
                "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                "Stations");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                "Trains");
        assertTag(
                "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                "Trains");
    }

    @Test
    void securityRequirementsAreExplicitAcrossPublicOperations() throws Exception {
        for (Map.Entry<String, List<String>> entry : PUBLIC_CONTROLLER_METHODS.entrySet()) {
            Class<?> controllerClass = loadClass(entry.getKey());
            for (String methodName : entry.getValue()) {
                Method method = method(controllerClass, methodName);
                SecurityRequirement securityRequirement =
                        method.getAnnotation(SecurityRequirement.class);

                if (method.isAnnotationPresent(PreAuthorize.class)) {
                    assertThat(securityRequirement)
                            .withFailMessage(
                                    "%s#%s missing bearer security", entry.getKey(), methodName)
                            .isNotNull();
                    assertThat(securityRequirement.name()).isEqualTo("bearerAuth");
                } else if ("io.github.phunguy65.ttbs.backend.user.infrastructure.web.AuthController"
                                .equals(entry.getKey())
                        && List.of("register", "login", "refresh", "logout").contains(methodName)) {
                    assertThat(securityRequirement)
                            .withFailMessage(
                                    "%s#%s should explicitly clear global security",
                                    entry.getKey(), methodName)
                            .isNotNull();
                    assertThat(securityRequirement.name()).isEmpty();
                }
            }
        }
    }

    @Test
    void paginatedAndCollectionEndpointsUseExpectedSuccessKinds() throws Exception {
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.booking.infrastructure.web.BookingController",
                        "listByUser"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.TrainController",
                        "list"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.CoachController",
                        "getCoachesByTrain"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                        "getSeatsByTrain"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.SeatController",
                        "getAvailableSeats"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.RouteTemplateController",
                        "list"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                        "list"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.train.infrastructure.web.ScheduledTripController",
                        "filter"))
                .isEqualTo(SuccessResponseKind.SLICE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                        "list"))
                .isEqualTo(SuccessResponseKind.PAGE);
        assertThat(kindOf(
                        "io.github.phunguy65.ttbs.backend.station.infrastructure.web.StationController",
                        "search"))
                .isEqualTo(SuccessResponseKind.ARRAY);
    }

    private void assertSecurityAndHiddenAuthParam(
            String controllerClassName, String methodName, String expectedRequirement)
            throws Exception {
        Method method = method(loadClass(controllerClassName), methodName);
        SecurityRequirement securityRequirement = method.getAnnotation(SecurityRequirement.class);
        assertThat(securityRequirement).isNotNull();
        assertThat(securityRequirement.name()).isEqualTo(expectedRequirement);

        java.lang.reflect.Parameter authenticationParameter = Arrays.stream(method.getParameters())
                .filter(parameter -> Authentication.class.isAssignableFrom(parameter.getType()))
                .findFirst()
                .orElseThrow();
        Parameter parameterAnnotation = authenticationParameter.getAnnotation(Parameter.class);
        assertThat(parameterAnnotation).isNotNull();
        assertThat(parameterAnnotation.hidden()).isTrue();
    }

    private void assertSuccessPayload(
            String controllerClassName,
            String methodName,
            String expectedValueClassName,
            SuccessResponseKind expectedKind,
            String expectedResponseCode)
            throws Exception {
        SuccessPayload successPayload = method(loadClass(controllerClassName), methodName)
                .getAnnotation(SuccessPayload.class);
        assertThat(successPayload).isNotNull();
        assertThat(successPayload.kind()).isEqualTo(expectedKind);
        assertThat(successPayload.responseCode()).isEqualTo(expectedResponseCode);
        assertThat(successPayload.value().getName()).isEqualTo(expectedValueClassName);
    }

    private void assertTag(String controllerClassName, String expectedTagName) throws Exception {
        io.swagger.v3.oas.annotations.tags.Tag tag = loadClass(controllerClassName)
                .getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        assertThat(tag).isNotNull();
        assertThat(tag.name()).isEqualTo(expectedTagName);
    }

    private SuccessResponseKind kindOf(String controllerClassName, String methodName)
            throws Exception {
        return method(loadClass(controllerClassName), methodName)
                .getAnnotation(SuccessPayload.class)
                .kind();
    }

    private String securityRequirementName(String controllerClassName, String methodName)
            throws Exception {
        SecurityRequirement securityRequirement = method(loadClass(controllerClassName), methodName)
                .getAnnotation(SecurityRequirement.class);
        assertThat(securityRequirement).isNotNull();
        return securityRequirement.name();
    }

    private Method method(Class<?> controllerClass, String methodName) {
        Map<String, Method> methods = Arrays.stream(controllerClass.getDeclaredMethods())
                .collect(Collectors.toMap(Method::getName, method -> method));
        assertThat(methods).containsKey(methodName);
        return methods.get(methodName);
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }
}

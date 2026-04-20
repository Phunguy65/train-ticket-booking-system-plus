package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetUserPaymentsQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.UserPaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.UserPaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetUserPaymentsUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);

    private final GetUserPaymentsUseCase useCase = new GetUserPaymentsUseCase(paymentRepository);

    @Test
    void returnsForbiddenWhenAuthenticatedUserDoesNotMatchPathUser() {
        GetUserPaymentsQuery query = new GetUserPaymentsQuery(
                USER_ID, UUID.fromString("22222222-2222-2222-2222-222222222222"), 0, 20);

        Result<PageResponse<UserPaymentResponse>, PaymentError> result = useCase.execute(query);

        assertThat(result).isEqualTo(Result.failure(new PaymentError.Forbidden()));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void returnsPagedPaymentsNewestFirstUsingDefaultSort() {
        UserPaymentSummary payment = payment(
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                PaymentStatus.PAID,
                450000,
                "2026-04-02T10:00:00Z",
                "Hanoi",
                "Ho Chi Minh City",
                "2026-05-01T08:00:00Z");
        PageResponse<UserPaymentSummary> page = PageResponse.of(List.of(payment), 0, 20, false, 1);
        when(paymentRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(0),
                        eq(20),
                        eq(List.of(SortOrder.desc("created_at"), SortOrder.desc("id")))))
                .thenReturn(page);

        Result<PageResponse<UserPaymentResponse>, PaymentError> result =
                useCase.execute(new GetUserPaymentsQuery(USER_ID, USER_ID, 0, 20));

        assertThat(result.isSuccess()).isTrue();
        PageResponse<UserPaymentResponse> response =
                ((Result.Success<PageResponse<UserPaymentResponse>, PaymentError>) result).value();
        assertThat(response.content()).hasSize(1);
        UserPaymentResponse paymentResponse = response.content().getFirst();
        assertThat(paymentResponse.id()).isEqualTo(payment.id());
        assertThat(paymentResponse.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentResponse.amount()).isEqualTo(BigDecimal.valueOf(450000));
        assertThat(paymentResponse.currency()).isEqualTo("VND");
        assertThat(paymentResponse.bookingId()).isEqualTo(payment.bookingId());
        assertThat(paymentResponse.booking().origin()).isEqualTo("Hanoi");
        assertThat(paymentResponse.booking().destination()).isEqualTo("Ho Chi Minh City");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        verify(paymentRepository)
                .findByUserId(
                        UserId.of(USER_ID),
                        0,
                        20,
                        List.of(SortOrder.desc("created_at"), SortOrder.desc("id")));
    }

    @Test
    void returnsEmptyPageWhenUserHasNoPayments() {
        when(paymentRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(0),
                        eq(20),
                        eq(List.of(SortOrder.desc("created_at"), SortOrder.desc("id")))))
                .thenReturn(PageResponse.empty(20));

        Result<PageResponse<UserPaymentResponse>, PaymentError> result =
                useCase.execute(new GetUserPaymentsQuery(USER_ID, USER_ID, 0, 20));

        assertThat(result.isSuccess()).isTrue();
        PageResponse<UserPaymentResponse> response =
                ((Result.Success<PageResponse<UserPaymentResponse>, PaymentError>) result).value();
        assertThat(response.content()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void preservesPaginationMetadataForLaterPages() {
        UserPaymentSummary newerPayment = payment(
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                PaymentStatus.PAID,
                550000,
                "2026-04-03T10:00:00Z",
                "Hanoi",
                "Da Nang",
                "2026-05-02T08:00:00Z");
        UserPaymentSummary olderPayment = payment(
                "55555555-5555-5555-5555-555555555555",
                "66666666-6666-6666-6666-666666666666",
                PaymentStatus.PENDING,
                350000,
                "2026-04-02T10:00:00Z",
                "Ho Chi Minh City",
                "Hanoi",
                "2026-05-01T08:00:00Z");
        PageResponse<UserPaymentSummary> page =
                PageResponse.of(List.of(newerPayment, olderPayment), 1, 20, true, 42);
        when(paymentRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(1),
                        eq(20),
                        eq(List.of(SortOrder.desc("created_at"), SortOrder.desc("id")))))
                .thenReturn(page);

        Result<PageResponse<UserPaymentResponse>, PaymentError> result =
                useCase.execute(new GetUserPaymentsQuery(USER_ID, USER_ID, 1, 20));

        PageResponse<UserPaymentResponse> response =
                ((Result.Success<PageResponse<UserPaymentResponse>, PaymentError>) result).value();
        assertThat(response.content())
                .extracting(UserPaymentResponse::id)
                .containsExactly(newerPayment.id(), olderPayment.id());
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(42);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    private UserPaymentSummary payment(
            String paymentId,
            String bookingId,
            PaymentStatus status,
            long amount,
            String createdAt,
            String origin,
            String destination,
            String departureTime) {
        return new UserPaymentSummary(
                UUID.fromString(paymentId),
                UUID.fromString(bookingId),
                USER_ID,
                status.name(),
                amount,
                "VND",
                Instant.parse(createdAt),
                origin,
                destination,
                Instant.parse(departureTime));
    }
}

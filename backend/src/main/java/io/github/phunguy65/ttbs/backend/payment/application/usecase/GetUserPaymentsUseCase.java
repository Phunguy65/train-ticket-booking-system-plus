package io.github.phunguy65.ttbs.backend.payment.application.usecase;

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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    public GetUserPaymentsUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Result<PageResponse<UserPaymentResponse>, PaymentError> execute(
            GetUserPaymentsQuery query) {
        // Owner-only authorization: user can only view their own payments
        if (!query.userId().equals(query.requestingUserId())) {
            return Result.failure(new PaymentError.Forbidden());
        }

        PageResponse<UserPaymentSummary> payments = paymentRepository.findByUserId(
                UserId.of(query.userId()),
                query.page(),
                query.size(),
                List.of(SortOrder.desc("created_at"), SortOrder.desc("id")));

        return Result.success(PageResponse.of(
                payments.content().stream().map(this::toResponse).toList(),
                payments.page(),
                payments.size(),
                payments.hasNext(),
                payments.total()));
    }

    private UserPaymentResponse toResponse(UserPaymentSummary summary) {
        return new UserPaymentResponse(
                summary.id(),
                PaymentStatus.valueOf(summary.status()),
                BigDecimal.valueOf(summary.amount()),
                summary.currency(),
                summary.createdAt(),
                summary.bookingId(),
                new UserPaymentResponse.BookingSummary(
                        summary.bookingId(),
                        summary.originStationName(),
                        summary.destinationStationName(),
                        summary.departureTime()));
    }
}

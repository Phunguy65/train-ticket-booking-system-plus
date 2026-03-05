package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.port.PaymentCheckoutSessionPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpireCheckoutSessionOnCancelListenerTest {

    @Mock
    private PaymentCheckoutSessionPort checkoutSessionPort;

    @InjectMocks
    private ExpireCheckoutSessionOnCancelListener listener;

    @Test
    void on_withValidSessionId_shouldExpireSession() {
        BookingCancelled event =
                BookingCancelled.of(BookingId.of(UUID.randomUUID()), "cs_test_abc123");

        listener.on(event);

        verify(checkoutSessionPort).expireSession(CheckoutSessionId.of("cs_test_abc123"));
    }

    @Test
    void on_withNullSessionId_shouldSkipGracefully() {
        BookingCancelled event = BookingCancelled.of(BookingId.of(UUID.randomUUID()), null);

        listener.on(event);

        verify(checkoutSessionPort, never()).expireSession(any());
    }

    @Test
    void on_withBlankSessionId_shouldSkipGracefully() {
        BookingCancelled event = BookingCancelled.of(BookingId.of(UUID.randomUUID()), "  ");

        listener.on(event);

        verify(checkoutSessionPort, never()).expireSession(any());
    }

    @Test
    void on_whenExpireThrows_shouldNotPropagateException() {
        BookingCancelled event =
                BookingCancelled.of(BookingId.of(UUID.randomUUID()), "cs_test_abc123");
        doThrow(new RuntimeException("Stripe error"))
                .when(checkoutSessionPort)
                .expireSession(any());

        // Should not throw
        listener.on(event);
    }
}

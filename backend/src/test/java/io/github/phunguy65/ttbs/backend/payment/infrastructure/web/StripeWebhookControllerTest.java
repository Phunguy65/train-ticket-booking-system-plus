package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.payment.application.usecase.HandlePaymentFailedUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.HandlePaymentSuccessUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe.StripeConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JacksonConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StripeWebhookController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, WebConfig.class, JacksonConfig.class})
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripeConfig stripeConfig;

    @MockitoBean
    private HandlePaymentSuccessUseCase handlePaymentSuccessUseCase;

    @MockitoBean
    private HandlePaymentFailedUseCase handlePaymentFailedUseCase;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void handleWebhook_invalidSignature_shouldReturn400() throws Exception {
        when(stripeConfig.getWebhookSecret()).thenReturn("whsec_test_secret");

        mockMvc.perform(post("/api/v1.0/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid_signature")
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    void handleWebhook_missingSignatureHeader_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest());
    }
}

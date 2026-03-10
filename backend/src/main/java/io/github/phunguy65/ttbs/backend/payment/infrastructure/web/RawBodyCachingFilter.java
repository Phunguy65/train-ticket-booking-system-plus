package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Wraps incoming requests with {@link ContentCachingRequestWrapper} so that the raw body
 * can be read multiple times — required for Stripe webhook signature verification.
 */
@Component
class RawBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().contains("/webhooks/stripe")) {
            ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, 65536);
            filterChain.doFilter(wrapper, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}

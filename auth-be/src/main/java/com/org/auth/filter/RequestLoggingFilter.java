package com.org.auth.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID    = "correlationId";
    private static final int    MAX_CORRELATION_ID_LEN = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // M2+L1: sanitize client-supplied correlation ID — strip control characters
        // (newlines, tabs, etc.) to prevent log injection, and cap at 64 chars.
        String rawCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId;
        if (rawCorrelationId == null || rawCorrelationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            // Remove any control characters (ASCII 0-31 and 127)
            String sanitized = rawCorrelationId.replaceAll("[\\p{Cntrl}]", "");
            correlationId = sanitized.length() > MAX_CORRELATION_ID_LEN
                    ? sanitized.substring(0, MAX_CORRELATION_ID_LEN)
                    : sanitized;
            if (correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // M1: log the real client IP — honour X-Forwarded-For set by the reverse
        // proxy (server.forward-headers-strategy=native ensures Spring unwraps it
        // so getRemoteAddr() already returns the real IP in a proxied deployment).
        long start = System.currentTimeMillis();
        String xff = request.getHeader("X-Forwarded-For");
        String clientIp = (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
        log.info("Inbound  {} {} from [{}]",
                request.getMethod(), request.getRequestURI(), clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("Outbound {} {} → {} ({}ms)",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration);
            MDC.clear();
        }
    }
}

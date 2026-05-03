package com.shop.auth.config;

import java.io.IOException;

import com.shop.auth.dto.ResponseDto;
import com.shop.auth.filter.JwtAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;  // local static instance — not injected
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Stateless sessions — no {@code HttpSession} created (JWT-only).</li>
 *   <li>{@link JwtAuthenticationFilter} runs before {@link UsernamePasswordAuthenticationFilter}.</li>
 *   <li>{@code FilterRegistrationBean} disables servlet auto-registration of the filter
 *       so it executes only inside the Security filter chain (no double-execution).</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} on controllers
 *       and services (Step 3 onward).</li>
 *   <li>401 and 403 responses are JSON {@link ResponseDto} — never Spring's default HTML.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                .accessDeniedHandler(jwtAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/verify-otp",
                    "/auth/resend-otp").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Prevents Spring Boot from also registering the filter as a plain servlet filter,
     * which would cause it to execute twice per request.
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> reg =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── 401 / 403 JSON response handlers ─────────────────────────────────────

    /**
     * Returns a 401 JSON response when a request reaches a protected endpoint
     * without a valid token (e.g., no Authorization header, or the filter let it through
     * to the security layer without setting an authentication).
     */
    private AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (HttpServletRequest req, HttpServletResponse res,
                org.springframework.security.core.AuthenticationException ex) -> {
            writeJson(res, HttpStatus.UNAUTHORIZED, "Authentication required");
        };
    }

    /**
     * Returns a 403 JSON response when {@code @PreAuthorize} rejects an authenticated
     * user who lacks the required authority.
     */
    private AccessDeniedHandler jwtAccessDeniedHandler() {
        return (HttpServletRequest req, HttpServletResponse res,
                org.springframework.security.access.AccessDeniedException ex) -> {
            writeJson(res, HttpStatus.FORBIDDEN, "Access denied");
        };
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ResponseDto<Void> body = new ResponseDto<>();
        body.setStatus(ResponseDto.Status.FAIL);
        body.setMessage(message);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}

package com.shop.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Registers a shared {@link ObjectMapper} bean with Java-time support.
 *
 * <p>
 * Spring Boot 4.x does not auto-configure {@code ObjectMapper} unless
 * {@code JacksonAutoConfiguration} is explicitly triggered.
 * {@code @ConditionalOnMissingBean}
 * ensures we do not override a bean that is already present (e.g., if a future
 * Boot upgrade
 * re-enables the auto-configuration).
 * </p>
 *
 * <p>
 * {@link JavaTimeModule} is required to serialise/deserialise
 * {@code java.time.Instant}
 * used in {@code OtpEmailMessage}. {@code WRITE_DATES_AS_TIMESTAMPS} is
 * disabled so that
 * dates are written as ISO-8601 strings, which are human-readable and
 * unambiguous.
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

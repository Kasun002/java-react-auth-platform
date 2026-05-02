package com.shop.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Authentication and user management endpoints for the shop platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Shop Platform Team")
                                .email("dev@shop.com"))
                        .license(new License()
                                .name("Private")));
    }
}

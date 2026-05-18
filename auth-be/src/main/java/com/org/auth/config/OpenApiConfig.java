package com.org.auth.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

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

        /**
         * Adds a 500 response to every endpoint globally — no need to repeat it per
         * method.
         */
        @Bean
        OperationCustomizer globalResponses() {
                return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
                        operation.getResponses().addApiResponse("500",
                                        new ApiResponse()
                                                        .description("Unexpected server error")
                                                        .content(new Content().addMediaType(
                                                                        "application/json",
                                                                        new MediaType().schema(new Schema<>().$ref(
                                                                                        "#/components/schemas/ResponseDto")))));
                        return operation;
                };
        }
}

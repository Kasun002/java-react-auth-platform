package com.org.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Standard API response wrapper")
public class ResponseDto<T> {

    public enum Status {
        SUCCESS,
        FAIL
    }

    @Schema(description = "Response payload — null for void operations")
    private T data;

    @Schema(description = "Operation outcome", example = "SUCCESS")
    private Status status;

    @Schema(description = "Human-readable message", example = "User registered successfully")
    private String message;

    @Schema(description = "Field-level validation errors — populated on 400 responses")
    private List<String> errors;
}

package com.shop.auth.dto;

import java.util.List;

import lombok.Data;

@Data
public class ResponseDto<T> {
    public enum Status {
        SUCCESS,
        FAIL
    }

    private T data;

    private Status status;

    private String message;

    private List<String> errors;
}

package com.shop.auth.exception.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shop.auth.dto.ResponseDto;
import com.shop.auth.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ResponseDto<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.FAIL);
        response.setMessage(ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);
        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.FAIL);
        response.setMessage("Validation failed");
        response.setErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ResponseDto<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.FAIL);
        response.setMessage("Malformed JSON request");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ResponseDto<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.FAIL);
        response.setMessage("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

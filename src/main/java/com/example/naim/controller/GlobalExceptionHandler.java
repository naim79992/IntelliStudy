package com.example.naim.controller;

import com.example.naim.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAllExceptions(Exception ex) {
        return ResponseEntity.internalServerError().body(
            ApiResponse.<String>builder()
                .success(false)
                .message("An unexpected error occurred: " + ex.getMessage())
                .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
            ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .build()
        );
    }
}

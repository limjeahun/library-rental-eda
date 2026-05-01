package com.example.library.common.core.web;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<FieldError> fieldErrors
) {
    public ErrorResponse {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ErrorResponse of(
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
    ) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}

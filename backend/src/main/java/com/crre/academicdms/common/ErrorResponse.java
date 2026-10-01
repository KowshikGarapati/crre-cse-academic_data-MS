package com.crre.academicdms.common;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    boolean success,
    String message,
    int status,
    Map<String, String> errors,
    Instant timestamp
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(false, message, status, null, Instant.now());
    }

    public static ErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ErrorResponse(false, message, status, errors, Instant.now());
    }
}

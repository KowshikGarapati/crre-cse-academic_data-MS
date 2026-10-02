package com.crre.academicdms.features.auth.dto;

public record AuthResponse(
    String message,
    UserSummaryDto user
) {
    public static AuthResponse of(UserSummaryDto user) {
        return new AuthResponse("Login successful", user);
    }
}

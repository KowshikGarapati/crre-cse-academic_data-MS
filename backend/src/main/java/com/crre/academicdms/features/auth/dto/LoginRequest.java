package com.crre.academicdms.features.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Identifier (username, email, registration number, or unique ID) is required")
    String identifier,

    @NotBlank(message = "Password is required")
    String password
) {}

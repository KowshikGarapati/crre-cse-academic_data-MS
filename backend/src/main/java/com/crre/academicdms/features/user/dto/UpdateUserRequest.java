package com.crre.academicdms.features.user.dto;

import com.crre.academicdms.features.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotBlank(message = "Full name is required") String fullName,
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
    @NotNull(message = "Role is required") Role role,
    String registrationNumber,
    String uniqueId,
    Long aadharNumber,
    Long apaarNumber,
    boolean enabled
) {}

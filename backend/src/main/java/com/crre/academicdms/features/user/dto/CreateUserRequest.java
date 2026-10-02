package com.crre.academicdms.features.user.dto;

import com.crre.academicdms.features.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateUserRequest(
    @NotBlank(message = "Full name is required") String fullName,
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
    @NotBlank(message = "Password is required") String password,
    @NotNull(message = "Role is required") Role role,
    String registrationNumber,
    String uniqueId,
    Long aadharNumber,
    Long apaarNumber,
    UUID departmentId,
    UUID programId,
    UUID subjectId,
    UUID sectionId,
    boolean classTeacher
) {}

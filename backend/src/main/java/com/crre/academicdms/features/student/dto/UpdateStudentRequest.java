package com.crre.academicdms.features.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateStudentRequest(
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName,

    @Email(message = "Invalid email format")
    String email,

    String entryType, // "REGULAR", "LATERAL"
    String admissionCategory, // "CONVENOR", "MANAGEMENT", or null/empty
    String tuitionCategory, // "RTF", "SELF_FINANCE", or null/empty
    Long aadharNumber,
    Long apaarNumber,
    UUID sectionId,
    String status // "ACTIVE", "INACTIVE"
) {}

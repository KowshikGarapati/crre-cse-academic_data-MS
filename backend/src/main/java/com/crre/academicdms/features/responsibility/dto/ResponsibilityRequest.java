package com.crre.academicdms.features.responsibility.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ResponsibilityRequest(
    @NotBlank(message = "Responsibility type is required") String type, // TEACHING, CLASS_TEACHER, HOD_DEPT, COUNSELLOR, PRINCIPAL_INSTITUTION
    UUID departmentId,
    UUID programId,
    UUID subjectId,
    UUID sectionId,
    String description
) {}

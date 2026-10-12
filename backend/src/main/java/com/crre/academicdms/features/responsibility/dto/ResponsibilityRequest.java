package com.crre.academicdms.features.responsibility.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ResponsibilityRequest(
    @NotBlank(message = "Responsibility type is required") String type, // TEACHING, CLASS_TEACHER, HOD_DEPT, COUNSELLOR, PRINCIPAL_INSTITUTION
    UUID departmentId,
    UUID programId,
    UUID subjectId,
    UUID sectionId,
    String description,
    List<UUID> studentIds
) {
    public ResponsibilityRequest(String type, UUID departmentId, UUID programId, UUID subjectId, UUID sectionId, String description) {
        this(type, departmentId, programId, subjectId, sectionId, description, null);
    }
}

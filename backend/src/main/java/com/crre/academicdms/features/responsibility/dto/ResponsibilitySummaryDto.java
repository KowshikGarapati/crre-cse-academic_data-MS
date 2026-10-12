package com.crre.academicdms.features.responsibility.dto;

import com.crre.academicdms.features.responsibility.model.Responsibility;
import java.util.UUID;

public record ResponsibilitySummaryDto(
    UUID id,
    String type,
    UUID departmentId,
    String departmentName,
    UUID programId,
    String programName,
    UUID subjectId,
    String subjectCode,
    String subjectName,
    UUID sectionId,
    String sectionName,
    String status,
    String description,
    UUID userId,
    Integer assignedStudentCount
) {
    public ResponsibilitySummaryDto(
        UUID id,
        String type,
        UUID departmentId,
        String departmentName,
        UUID programId,
        String programName,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        UUID sectionId,
        String sectionName,
        String status,
        String description
    ) {
        this(id, type, departmentId, departmentName, programId, programName, subjectId, subjectCode, subjectName, sectionId, sectionName, status, description, null, null);
    }

    public static ResponsibilitySummaryDto fromEntity(Responsibility r) {
        return fromEntity(r, null);
    }

    public static ResponsibilitySummaryDto fromEntity(Responsibility r, Integer studentCount) {
        return new ResponsibilitySummaryDto(
            r.getId(),
            r.getType(),
            r.getDepartment() != null ? r.getDepartment().getId() : null,
            r.getDepartment() != null ? r.getDepartment().getName() : null,
            r.getProgram() != null ? r.getProgram().getId() : null,
            r.getProgram() != null ? r.getProgram().getName() : null,
            r.getSubject() != null ? r.getSubject().getId() : null,
            r.getSubject() != null ? r.getSubject().getCode() : null,
            r.getSubject() != null ? r.getSubject().getName() : null,
            r.getSection() != null ? r.getSection().getId() : null,
            r.getSection() != null ? r.getSection().getSectionName() : null,
            r.getStatus(),
            r.getDescription(),
            r.getUser() != null ? r.getUser().getId() : null,
            studentCount
        );
    }
}

package com.crre.academicdms.features.responsibility.dto;

import com.crre.academicdms.features.academic.model.Program;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import java.util.UUID;

public record ResponsibilitySummaryDto(
    UUID id,
    String type,
    UUID departmentId,
    String departmentName,
    UUID programId,
    String programCode,
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
        this(id, type, departmentId, departmentName, programId, null, programName, subjectId, subjectCode, subjectName, sectionId, sectionName, status, description, null, null);
    }

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
        String description,
        UUID userId,
        Integer assignedStudentCount
    ) {
        this(id, type, departmentId, departmentName, programId, null, programName, subjectId, subjectCode, subjectName, sectionId, sectionName, status, description, userId, assignedStudentCount);
    }

    public static ResponsibilitySummaryDto fromEntity(Responsibility r) {
        return fromEntity(r, null);
    }

    public static ResponsibilitySummaryDto fromEntity(Responsibility r, Integer studentCount) {
        Program program = r.getProgram();
        if (program == null && r.getSection() != null && r.getSection().getSemester() != null) {
            program = r.getSection().getSemester().getProgram();
        }
        if (program == null && r.getSubject() != null) {
            program = r.getSubject().getProgram();
        }

        return new ResponsibilitySummaryDto(
            r.getId(),
            r.getType(),
            r.getDepartment() != null ? r.getDepartment().getId() : null,
            r.getDepartment() != null ? r.getDepartment().getName() : null,
            program != null ? program.getId() : null,
            program != null ? program.getCode() : null,
            program != null ? program.getName() : null,
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

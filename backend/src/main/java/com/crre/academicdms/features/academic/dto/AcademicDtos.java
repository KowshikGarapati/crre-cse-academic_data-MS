package com.crre.academicdms.features.academic.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AcademicDtos {

    public record DepartmentDto(UUID id, String code, String name) {}

    public record ProgramDto(UUID id, UUID departmentId, String code, String name, String degree) {}

    public record SectionDto(UUID id, UUID semesterId, int sectionNumber, String sectionName) {}

    public record SubjectDto(UUID id, UUID programId, UUID semesterId, String code, String name, String type, BigDecimal credits) {}

    public record AcademicMetadataDto(
        List<DepartmentDto> departments,
        List<ProgramDto> programs,
        List<SectionDto> sections,
        List<SubjectDto> subjects
    ) {}

    public record StudentDto(
        UUID id,
        String registrationNumber,
        String fullName,
        String entryType,
        String status,
        UUID sectionId,
        String sectionName,
        String programName,
        String semesterName
    ) {}

    public record TimetableEntryDto(
        UUID id,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        String facultyName,
        String dayOfWeek,
        int periodNumber,
        String startTime,
        String endTime
    ) {}
}

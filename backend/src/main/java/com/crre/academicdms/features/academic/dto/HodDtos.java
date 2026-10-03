package com.crre.academicdms.features.academic.dto;

import com.crre.academicdms.features.faculty.dto.FacultyDtos.FacultyProfileDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HodDtos {

    public record HodSectionSummaryDto(
        UUID id,
        int sectionNumber,
        String sectionName,
        long studentCount,
        String classTeacherName
    ) {}

    public record HodSemesterSummaryDto(
        UUID id,
        int semesterNumber,
        String termName,
        List<HodSectionSummaryDto> sections
    ) {}

    public record HodProgramHierarchyDto(
        UUID id,
        String code,
        String name,
        String degree,
        List<HodSemesterSummaryDto> semesters
    ) {}

    public record HodAcademicYearHierarchyDto(
        UUID id,
        String yearName,
        int yearNumber,
        boolean current,
        List<HodProgramHierarchyDto> programs
    ) {}

    public record HodSubjectSummaryDto(
        UUID id,
        String code,
        String name,
        String type,
        BigDecimal credits,
        String programCode,
        String programName,
        String semesterName,
        List<String> assignedFacultyNames
    ) {}

    public record DepartmentAcademicStructureDto(
        UUID departmentId,
        String departmentCode,
        String departmentName,
        String hodName,
        List<HodAcademicYearHierarchyDto> academicYears,
        List<FacultyProfileDto> faculty,
        List<HodSubjectSummaryDto> subjects
    ) {}
}

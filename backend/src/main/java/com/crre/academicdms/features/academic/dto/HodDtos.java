package com.crre.academicdms.features.academic.dto;

import com.crre.academicdms.features.faculty.dto.FacultyDtos.FacultyProfileDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HodDtos {

    public record HodSectionSummaryDto(
        UUID id,
        UUID sectionId,
        int sectionNumber,
        String sectionName,
        long studentCount,
        String classTeacherName
    ) {}

    public record HodSemesterSummaryDto(
        UUID id,
        UUID semesterId,
        int semesterNumber,
        String termName,
        List<HodSectionSummaryDto> sections
    ) {}

    public record HodProgramHierarchyDto(
        UUID id,
        UUID programId,
        String code,
        String programCode,
        String name,
        String programName,
        String degree,
        List<HodSemesterSummaryDto> semesters
    ) {}

    public record HodAcademicYearHierarchyDto(
        UUID id,
        UUID academicYearId,
        String yearName,
        int yearNumber,
        boolean current,
        boolean isCurrent,
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
        long totalFaculty,
        long totalStudents,
        List<HodAcademicYearHierarchyDto> academicYears,
        List<FacultyProfileDto> faculty,
        List<HodSubjectSummaryDto> subjects
    ) {}
}

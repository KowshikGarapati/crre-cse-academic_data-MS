package com.crre.academicdms.features.academic.dto;

import com.crre.academicdms.features.academic.dto.HodDtos.DepartmentAcademicStructureDto;
import java.util.List;
import java.util.UUID;

public class PrincipalDtos {

    public record CollegeDepartmentSummaryDto(
        UUID id,
        String code,
        String name,
        String hodName,
        int programCount,
        long facultyCount,
        long studentCount,
        List<String> programNames
    ) {}

    public record CollegeOverviewDto(
        UUID institutionId,
        String institutionCode,
        String institutionName,
        String principalName,
        int totalDepartments,
        int totalPrograms,
        long totalFaculty,
        long totalStudents,
        List<CollegeDepartmentSummaryDto> departments
    ) {}
}

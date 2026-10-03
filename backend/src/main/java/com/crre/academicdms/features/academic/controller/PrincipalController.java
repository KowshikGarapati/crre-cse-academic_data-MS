package com.crre.academicdms.features.academic.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.academic.dto.HodDtos.DepartmentAcademicStructureDto;
import com.crre.academicdms.features.academic.dto.PrincipalDtos.CollegeOverviewDto;
import com.crre.academicdms.features.academic.service.PrincipalService;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/principal")
public class PrincipalController {

    private final PrincipalService principalService;

    public PrincipalController(PrincipalService principalService) {
        this.principalService = principalService;
    }

    @GetMapping("/college-overview")
    public ResponseEntity<ApiResponse<CollegeOverviewDto>> getCollegeOverview(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        CollegeOverviewDto overview = principalService.getCollegeOverview(caller);
        return ResponseEntity.ok(ApiResponse.success("College institutional overview", overview));
    }

    @GetMapping("/departments/{deptId}")
    public ResponseEntity<ApiResponse<DepartmentAcademicStructureDto>> getDepartmentDetails(
        @PathVariable UUID deptId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        DepartmentAcademicStructureDto details = principalService.getDepartmentDetails(deptId, caller);
        return ResponseEntity.ok(ApiResponse.success("Department details retrieved", details));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllCollegeStudents(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<StudentDto> students = principalService.getAllCollegeStudents(caller);
        return ResponseEntity.ok(ApiResponse.success("College-wide students retrieved", students));
    }
}

package com.crre.academicdms.features.academic.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.HodDtos.DepartmentAcademicStructureDto;
import com.crre.academicdms.features.academic.service.HodService;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hod")
public class HodController {

    private final HodService hodService;

    public HodController(HodService hodService) {
        this.hodService = hodService;
    }

    @GetMapping("/my-department")
    public ResponseEntity<ApiResponse<DepartmentAcademicStructureDto>> getMyDepartmentStructure(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        DepartmentAcademicStructureDto structure = hodService.getMyDepartmentStructure(caller);
        return ResponseEntity.ok(ApiResponse.success("Department academic structure retrieved", structure));
    }
}

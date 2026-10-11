package com.crre.academicdms.features.counselling.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.CounsellingRecordDto;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.CounsellingStudentDto;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.CreateCounsellingRecordRequest;
import com.crre.academicdms.features.faculty.service.FacultyService;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/counselling")
public class CounsellingController {

    private final FacultyService facultyService;

    public CounsellingController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping("/mentees")
    public ResponseEntity<ApiResponse<List<CounsellingStudentDto>>> getMentees(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<CounsellingStudentDto> list = facultyService.getMyCounsellingStudents(caller);
        return ResponseEntity.ok(ApiResponse.success("Assigned counselling mentees retrieved", list));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<CounsellingRecordDto>>> getRecords(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<CounsellingRecordDto> list = facultyService.getMyCounsellingRecords(caller);
        return ResponseEntity.ok(ApiResponse.success("Counselling session logs retrieved", list));
    }

    @PostMapping("/records")
    public ResponseEntity<ApiResponse<CounsellingRecordDto>> createRecord(
        @Valid @RequestBody CreateCounsellingRecordRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        CounsellingRecordDto created = facultyService.createCounsellingRecord(req, caller);
        return ResponseEntity.ok(ApiResponse.success("Counselling record created", created));
    }
}

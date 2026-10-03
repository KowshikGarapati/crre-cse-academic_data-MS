package com.crre.academicdms.features.academic.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.AcademicMetadataDto;
import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import com.crre.academicdms.features.academic.service.AcademicService;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping("/academic/metadata")
    public ResponseEntity<ApiResponse<AcademicMetadataDto>> getMetadata() {
        AcademicMetadataDto metadata = academicService.getMetadata();
        return ResponseEntity.ok(ApiResponse.success("Academic metadata retrieved successfully", metadata));
    }

    @GetMapping("/sections/{sectionId}/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getSectionStudents(
        @PathVariable UUID sectionId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<StudentDto> students = academicService.getStudentsForSection(sectionId, caller);
        return ResponseEntity.ok(ApiResponse.success("Section students retrieved successfully", students));
    }

    @GetMapping("/sections/{sectionId}/timetable")
    public ResponseEntity<ApiResponse<List<TimetableEntryDto>>> getSectionTimetable(@PathVariable UUID sectionId) {
        List<TimetableEntryDto> timetable = academicService.getTimetableForSection(sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section timetable retrieved successfully", timetable));
    }

    @GetMapping("/faculty/my-teaching")
    public ResponseEntity<ApiResponse<List<ResponsibilitySummaryDto>>> getMyTeachingAssignments(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<ResponsibilitySummaryDto> teaching = academicService.getFacultyTeaching(caller.getId());
        return ResponseEntity.ok(ApiResponse.success("Teaching assignments retrieved successfully", teaching));
    }
}

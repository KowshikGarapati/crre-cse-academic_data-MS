package com.crre.academicdms.features.faculty.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.*;
import com.crre.academicdms.features.faculty.service.FacultyService;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping("/my-subjects")
    public ResponseEntity<ApiResponse<List<MySubjectDto>>> getMySubjects(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<MySubjectDto> list = facultyService.getMySubjects(caller);
        return ResponseEntity.ok(ApiResponse.success("Assigned subjects retrieved", list));
    }

    @GetMapping("/my-classes")
    public ResponseEntity<ApiResponse<List<MyClassDto>>> getMyClasses(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<MyClassDto> list = facultyService.getMyClasses(caller);
        return ResponseEntity.ok(ApiResponse.success("Assigned classes retrieved", list));
    }

    @GetMapping("/my-timetable")
    public ResponseEntity<ApiResponse<List<FacultyTimetableEntryDto>>> getMyTimetable(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<FacultyTimetableEntryDto> list = facultyService.getMyTimetable(caller);
        return ResponseEntity.ok(ApiResponse.success("Faculty timetable retrieved", list));
    }

    @GetMapping("/classes/{sectionId}/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getStudentsForClass(
        @PathVariable UUID sectionId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<StudentDto> list = facultyService.getStudentsForClass(sectionId, caller);
        return ResponseEntity.ok(ApiResponse.success("Section students retrieved", list));
    }

    @GetMapping("/my-section/overview")
    public ResponseEntity<ApiResponse<MySectionOverviewDto>> getMySectionOverview(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        MySectionOverviewDto overview = facultyService.getMySectionOverview(caller);
        return ResponseEntity.ok(ApiResponse.success("Class Teacher section overview", overview));
    }

    @GetMapping("/my-section/students")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getMySectionStudents(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<StudentDto> list = facultyService.getMySectionStudents(caller);
        return ResponseEntity.ok(ApiResponse.success("Class Teacher section students", list));
    }

    @GetMapping("/my-counselling/students")
    public ResponseEntity<ApiResponse<List<CounsellingStudentDto>>> getMyCounsellingStudents(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<CounsellingStudentDto> list = facultyService.getMyCounsellingStudents(caller);
        return ResponseEntity.ok(ApiResponse.success("Assigned counselling students", list));
    }

    @GetMapping("/my-counselling/records")
    public ResponseEntity<ApiResponse<List<CounsellingRecordDto>>> getMyCounsellingRecords(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<CounsellingRecordDto> list = facultyService.getMyCounsellingRecords(caller);
        return ResponseEntity.ok(ApiResponse.success("Counselling session logs", list));
    }

    @PostMapping("/my-counselling/records")
    public ResponseEntity<ApiResponse<CounsellingRecordDto>> createCounsellingRecord(
        @Valid @RequestBody CreateCounsellingRecordRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        CounsellingRecordDto created = facultyService.createCounsellingRecord(req, caller);
        return ResponseEntity.ok(ApiResponse.success("Counselling record created", created));
    }
}

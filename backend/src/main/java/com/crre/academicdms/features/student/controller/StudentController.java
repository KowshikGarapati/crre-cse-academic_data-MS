package com.crre.academicdms.features.student.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import com.crre.academicdms.features.student.dto.StudentDtos.StudentCounsellingSummaryDto;
import com.crre.academicdms.features.student.dto.StudentDtos.StudentDetailDto;
import com.crre.academicdms.features.student.dto.StudentDtos.StudentProfileDetailsDto;
import com.crre.academicdms.features.student.service.StudentService;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/student", "/api/v1/students"})
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfileDetailsDto>> getMyProfile(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentProfileDetailsDto profile = studentService.getMyProfile(caller);
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved", profile));
    }

    @GetMapping("/my-timetable")
    public ResponseEntity<ApiResponse<List<TimetableEntryDto>>> getMyTimetable(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<TimetableEntryDto> timetable = studentService.getMyTimetable(caller);
        return ResponseEntity.ok(ApiResponse.success("Student timetable retrieved", timetable));
    }

    @GetMapping("/my-counselling")
    public ResponseEntity<ApiResponse<StudentCounsellingSummaryDto>> getMyCounselling(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentCounsellingSummaryDto summary = studentService.getMyCounselling(caller);
        return ResponseEntity.ok(ApiResponse.success("Student counselling summary retrieved", summary));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto>>> getScopedStudents(
        @RequestParam(required = false) UUID sectionId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto> students = studentService.getScopedStudents(sectionId, caller);
        return ResponseEntity.ok(ApiResponse.success("Scoped students retrieved successfully", students));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentDetailDto>> getStudentDetails(
        @PathVariable String studentId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentDetailDto details = studentService.getStudentDetails(studentId, caller);
        return ResponseEntity.ok(ApiResponse.success("Student details retrieved successfully", details));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentDetailDto>> updateStudent(
        @PathVariable String studentId,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.crre.academicdms.features.student.dto.UpdateStudentRequest request,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentDetailDto updated = studentService.updateStudent(studentId, request, caller);
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", updated));
    }
}

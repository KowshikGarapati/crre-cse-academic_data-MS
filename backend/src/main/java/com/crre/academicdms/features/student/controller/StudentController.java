package com.crre.academicdms.features.student.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import com.crre.academicdms.features.student.dto.StudentDtos.StudentCounsellingSummaryDto;
import com.crre.academicdms.features.student.dto.StudentDtos.StudentProfileDetailsDto;
import com.crre.academicdms.features.student.service.StudentService;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
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
}

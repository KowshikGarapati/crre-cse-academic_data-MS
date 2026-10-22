package com.crre.academicdms.features.student.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import com.crre.academicdms.features.student.dto.StudentDtos.*;
import org.springframework.web.bind.annotation.PostMapping;
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

    @org.springframework.web.bind.annotation.PutMapping("/{studentId}/reassign-section")
    public ResponseEntity<ApiResponse<ReassignSectionResponse>> reassignLateralSection(
        @PathVariable String studentId,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ReassignSectionRequest req,
        @AuthenticationPrincipal UserPrincipal caller,
        jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String ua = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
        ReassignSectionResponse res = studentService.reassignLateralSection(studentId, req, caller, ip, ua);
        return ResponseEntity.ok(ApiResponse.success(res.message(), res));
    }

    @GetMapping("/{studentId}/certificates")
    public ResponseEntity<ApiResponse<List<StudentCertificateDto>>> getStudentCertificates(
        @PathVariable String studentId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<StudentCertificateDto> certs = studentService.getStudentCertificates(studentId, caller);
        return ResponseEntity.ok(ApiResponse.success("Student certificates retrieved successfully", certs));
    }

    @PostMapping("/{studentId}/certificates")
    public ResponseEntity<ApiResponse<StudentCertificateDto>> addStudentCertificate(
        @PathVariable String studentId,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpsertCertificateRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentCertificateDto cert = studentService.addStudentCertificate(studentId, req, caller);
        return ResponseEntity.ok(ApiResponse.success("Student certificate added successfully", cert));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{studentId}/certificates/{certificateId}")
    public ResponseEntity<ApiResponse<StudentCertificateDto>> updateStudentCertificate(
        @PathVariable String studentId,
        @PathVariable UUID certificateId,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpsertCertificateRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        StudentCertificateDto cert = studentService.updateStudentCertificate(studentId, certificateId, req, caller);
        return ResponseEntity.ok(ApiResponse.success("Student certificate updated successfully", cert));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{studentId}/certificates/{certificateId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudentCertificate(
        @PathVariable String studentId,
        @PathVariable UUID certificateId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        studentService.deleteStudentCertificate(studentId, certificateId, caller);
        return ResponseEntity.ok(ApiResponse.success("Student certificate deleted successfully", null));
    }

    @PostMapping("/certificates/backfill")
    public ResponseEntity<ApiResponse<Integer>> backfillCertificates(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        if (caller == null || (caller.getRole() != com.crre.academicdms.features.user.model.Role.ADMIN && caller.getRole() != com.crre.academicdms.features.user.model.Role.PRINCIPAL)) {
            throw new org.springframework.security.access.AccessDeniedException("Only Administrators or Principal can trigger certificate backfill");
        }
        int count = studentService.backfillCertificatesFromStudentRecords();
        return ResponseEntity.ok(ApiResponse.success("Initialized " + count + " certificate records", count));
    }
}

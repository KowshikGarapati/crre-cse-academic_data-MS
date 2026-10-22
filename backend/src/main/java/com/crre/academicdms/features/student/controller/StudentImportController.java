package com.crre.academicdms.features.student.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.student.dto.StudentImportDtos.*;
import com.crre.academicdms.features.student.service.StudentImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping({"/api/v1/students/import", "/api/v1/student/import"})
public class StudentImportController {

    private final StudentImportService importService;

    public StudentImportController(StudentImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'HOD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportPreviewResponse>> preview(
        @RequestBody(required = false) ImportRequest request
    ) {
        Path regPath = resolveRegularPath(request);
        Path latPath = resolveLateralPath(request);

        ImportPreviewResponse preview = importService.preview(regPath, latPath);
        return ResponseEntity.ok(ApiResponse.success("Import preview generated successfully", preview));
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'HOD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportExecuteResponse>> execute(
        @RequestBody(required = false) ImportRequest request
    ) {
        Path regPath = resolveRegularPath(request);
        Path latPath = resolveLateralPath(request);
        boolean dryRun = request != null && request.dryRun();

        ImportExecuteResponse response = importService.executeImport(regPath, latPath, dryRun);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    private Path resolveRegularPath(ImportRequest request) {
        if (request != null && request.regularFilePath() != null && !request.regularFilePath().isBlank()) {
            return Paths.get(request.regularFilePath());
        }
        return Paths.get(StudentImportService.DEFAULT_REGULAR_PATH);
    }

    private Path resolveLateralPath(ImportRequest request) {
        if (request != null && request.lateralFilePath() != null && !request.lateralFilePath().isBlank()) {
            return Paths.get(request.lateralFilePath());
        }
        return Paths.get(StudentImportService.DEFAULT_LATERAL_PATH);
    }
}

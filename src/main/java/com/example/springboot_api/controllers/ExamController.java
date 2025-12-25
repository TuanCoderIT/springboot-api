package com.example.springboot_api.controllers;

import com.example.springboot_api.dto.exam.*;
import com.example.springboot_api.services.exam.ExamService;
import com.example.springboot_api.services.exam.ExamExportService;
import com.example.springboot_api.config.security.JwtProvider;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exam Management", description = "APIs for managing online exams")
public class ExamController {
    
    private final ExamService examService;
    private final ExamExportService examExportService;
    private final UserRepository userRepository;
    
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a new exam", description = "Create a new exam for a class")
    public ResponseEntity<ExamResponse> createExam(
            @Valid @RequestBody CreateExamRequest request,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Creating exam for class {} by lecturer {}", request.getClassId(), lecturerId);
        
        ExamResponse response = examService.createExam(request, lecturerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{examId}/generate")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Generate questions for exam", description = "Generate questions from notebook files using AI")
    public ResponseEntity<ExamResponse> generateQuestions(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            @Valid @RequestBody GenerateQuestionsRequest request,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Generating questions for exam {} by lecturer {}", examId, lecturerId);
        
        ExamResponse response = examService.generateQuestions(examId, request, lecturerId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{examId}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Publish exam", description = "Publish exam to make it available for students")
    public ResponseEntity<ExamResponse> publishExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Publishing exam {} by lecturer {}", examId, lecturerId);
        
        ExamResponse response = examService.publishExam(examId, lecturerId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{examId}/activate")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Activate exam", description = "Activate exam to allow students to take it")
    public ResponseEntity<ExamResponse> activateExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Activating exam {} by lecturer {}", examId, lecturerId);
        
        ExamResponse response = examService.activateExam(examId, lecturerId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{examId}/cancel")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Cancel exam", description = "Cancel exam")
    public ResponseEntity<ExamResponse> cancelExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Cancelling exam {} by lecturer {}", examId, lecturerId);
        
        ExamResponse response = examService.cancelExam(examId, lecturerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get exams by class", description = "Get all exams for a specific class")
    public ResponseEntity<Page<ExamResponse>> getExamsByClass(
            @Parameter(description = "Class ID") @PathVariable UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ExamResponse> response = examService.getExamsByClass(classId, lecturerId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/lecturer")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get exams by lecturer", description = "Get all exams created by the lecturer")
    public ResponseEntity<Page<ExamResponse>> getExamsByLecturer(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ExamResponse> response = examService.getExamsByLecturer(lecturerId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{examId}")
    @Operation(summary = "Get exam details", description = "Get detailed information about an exam")
    public ResponseEntity<ExamResponse> getExamById(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID userId = getUserIdFromToken(httpRequest);
        ExamResponse response = examService.getExamById(examId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{examId}/preview")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Preview exam with questions and answers", description = "Preview complete exam structure with questions, answers, and scoring information for lecturers")
    public ResponseEntity<ExamPreviewResponse> previewExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Previewing exam {} by lecturer {}", examId, lecturerId);
        
        ExamPreviewResponse response = examService.previewExam(examId, lecturerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/available")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get available exams for student", description = "Get all exams that the student can take")
    public ResponseEntity<List<ExamResponse>> getAvailableExamsForStudent(
            HttpServletRequest httpRequest) {
        
        String studentCode = getStudentCodeFromToken(httpRequest);
        List<ExamResponse> response = examService.getAvailableExamsForStudent(studentCode);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Start exam", description = "Start taking an exam")
    public ResponseEntity<ExamAttemptResponse> startExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            @Valid @RequestBody StartExamRequest request,
            HttpServletRequest httpRequest) {
        
        String studentCode = getStudentCodeFromToken(httpRequest);
        
        // Set IP address from request
        request.setIpAddress(getClientIpAddress(httpRequest));
        request.setUserAgent(httpRequest.getHeader("User-Agent"));
        
        log.info("Starting exam {} for student {}", examId, studentCode);
        
        ExamAttemptResponse response = examService.startExam(examId, request, studentCode);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit exam", description = "Submit exam answers")
    public ResponseEntity<ExamResultResponse> submitExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            @Valid @RequestBody SubmitExamRequest request,
            HttpServletRequest httpRequest) {
        
        String studentCode = getStudentCodeFromToken(httpRequest);
        log.info("Submitting exam {} for student {}", examId, studentCode);
        
        ExamResultResponse response = examService.submitExam(request, studentCode);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{examId}/result")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get exam result", description = "Get exam result for student")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        String studentCode = getStudentCodeFromToken(httpRequest);
        ExamResultResponse response = examService.getExamResult(examId, studentCode);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{examId}/results")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get all exam results", description = "Get all exam results for lecturer")
    public ResponseEntity<Page<ExamResultResponse>> getExamResults(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ExamResultResponse> response = examService.getExamResults(examId, lecturerId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Delete exam", description = "Delete an exam")
    public ResponseEntity<Void> deleteExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Deleting exam {} by lecturer {}", examId, lecturerId);
        
        examService.deleteExam(examId, lecturerId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{examId}/can-take")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Check if student can take exam", description = "Check if student is eligible to take the exam")
    public ResponseEntity<Boolean> canStudentTakeExam(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            HttpServletRequest httpRequest) {
        
        String studentCode = getStudentCodeFromToken(httpRequest);
        boolean canTake = examService.canStudentTakeExam(examId, studentCode);
        return ResponseEntity.ok(canTake);
    }
    
    @PostMapping("/{examId}/export")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Export exam results", description = "Export exam results in Excel or CSV format")
    public ResponseEntity<byte[]> exportExamResults(
            @Parameter(description = "Exam ID") @PathVariable UUID examId,
            @Valid @RequestBody ExportRequest request,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Exporting exam results for exam {} by lecturer {} in format {}", 
                examId, lecturerId, request.getFormat());
        
        ExportResponse exportResponse = examExportService.exportExamResults(examId, request, lecturerId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(exportResponse.getMimeType()));
        headers.setContentDispositionFormData("attachment", exportResponse.getFilename());
        headers.setContentLength(exportResponse.getSize());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(exportResponse.getData());
    }
    
    @PostMapping("/export-multiple")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Export multiple exam results", description = "Export results from multiple exams in Excel or CSV format")
    public ResponseEntity<byte[]> exportMultipleExamResults(
            @RequestParam UUID[] examIds,
            @Valid @RequestBody ExportRequest request,
            HttpServletRequest httpRequest) {
        
        UUID lecturerId = getUserIdFromToken(httpRequest);
        log.info("Exporting multiple exam results for {} exams by lecturer {} in format {}", 
                examIds.length, lecturerId, request.getFormat());
        
        ExportResponse exportResponse = examExportService.exportMultipleExamResults(examIds, request, lecturerId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(exportResponse.getMimeType()));
        headers.setContentDispositionFormData("attachment", exportResponse.getFilename());
        headers.setContentLength(exportResponse.getSize());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(exportResponse.getData());
    }
    
    // Helper methods
    
    private UUID getUserIdFromToken(HttpServletRequest request) {
        // Sử dụng Security Context thay vì extract token thủ công
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        }
        throw new RuntimeException("No authenticated user found");
    }
    
    private String getStudentCodeFromToken(HttpServletRequest request) {
        // Cần query database để lấy student code từ user ID
        UUID userId = getUserIdFromToken(request);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getStudentCode() != null) {
                return user.getStudentCode();
            }
            throw new RuntimeException("Student code not found for user: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving student code: " + e.getMessage());
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
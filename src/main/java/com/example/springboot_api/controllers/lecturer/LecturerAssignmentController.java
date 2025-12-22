package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.lecturer.ClassResponse;
import com.example.springboot_api.dto.lecturer.ClassStudentResponse;
import com.example.springboot_api.dto.lecturer.LecturerAssignmentResponse;
import com.example.springboot_api.dto.lecturer.RequestTeachingRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.lecturer.LecturerAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller dành riêng cho các API của Giảng viên.
 */
@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Tag(name = "Lecturer Teaching", description = "Các API liên quan đến giảng dạy của Giảng viên")
public class LecturerAssignmentController {

    private final LecturerAssignmentService assignmentService;

    // ========== TEACHING ASSIGNMENTS ==========

    @GetMapping("/teaching-assignments")
    @Operation(summary = "Lấy danh sách phân công giảng dạy của mình")
    public PagedResponse<LecturerAssignmentResponse> getMyAssignments(
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String termStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return assignmentService.getMyAssignments(termId, status, termStatus, pageable);
    }

    @PostMapping("/teaching-assignments/request")
    @Operation(summary = "Gửi yêu cầu xin dạy môn học trong học kỳ")
    public LecturerAssignmentResponse requestTeaching(@Valid @RequestBody RequestTeachingRequest req) {
        return assignmentService.requestTeaching(req);
    }

    @GetMapping("/teaching-assignments/{assignmentId}/classes")
    @Operation(summary = "Lấy danh sách lớp học phần của một phân công")
    public PagedResponse<ClassResponse> getAssignmentClasses(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "classCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return assignmentService.getMyClasses(assignmentId, q, page, size, sortBy, sortDir);
    }

    @GetMapping("/teaching-assignments/{assignmentId}/students")
    @Operation(summary = "Lấy toàn bộ sinh viên trong phân công (từ tất cả lớp)")
    public PagedResponse<ClassStudentResponse> getAssignmentStudents(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return assignmentService.getAssignmentStudents(assignmentId, q, page, size, sortBy, sortDir);
    }

    // ========== CLASSES ==========

    @GetMapping("/classes/{classId}/members")
    @Operation(summary = "Lấy danh sách sinh viên trong 1 lớp học phần cụ thể")
    public PagedResponse<ClassStudentResponse> getClassMembers(
            @PathVariable UUID classId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return assignmentService.getClassStudents(classId, q, page, size, sortBy, sortDir);
    }
}

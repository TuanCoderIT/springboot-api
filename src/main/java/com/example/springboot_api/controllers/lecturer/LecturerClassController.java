package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.lecturer.ClassDetailResponse;
import com.example.springboot_api.dto.lecturer.ClassResponse;
import com.example.springboot_api.dto.lecturer.ClassStudentResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.lecturer.LecturerClassService;
import com.example.springboot_api.services.lecturer.LecturerStudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Tag(name = "Lecturer Classes", description = "Quản lý lớp học phần và sinh viên")
public class LecturerClassController {

    private final LecturerStudentService studentService;
    private final LecturerClassService classService;

    @GetMapping("/classes")
    @Operation(summary = "Lấy danh sách tất cả các lớp học phần (có filter)")
    public PagedResponse<ClassResponse> getMyClasses(
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "classCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return classService.getAllClasses(termId, assignmentId, q, page, size, sortBy, sortDir);
    }

    @GetMapping("/classes/{classId}")
    @Operation(summary = "Lấy chi tiết một lớp học phần")
    public ClassDetailResponse getClassDetail(@PathVariable UUID classId) {
        return classService.getClassDetail(classId);
    }

    @GetMapping("/classes/{classId}/members")
    @Operation(summary = "Lấy danh sách sinh viên trong 1 lớp học phần cụ thể")
    public PagedResponse<ClassStudentResponse> getClassMembers(
            @PathVariable UUID classId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return studentService.getClassStudents(classId, q, page, size, sortBy, sortDir);
    }
}

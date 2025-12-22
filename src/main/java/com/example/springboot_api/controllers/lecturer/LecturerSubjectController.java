package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.subject.ListSubjectRequest;
import com.example.springboot_api.dto.admin.subject.SubjectDetailResponse;
import com.example.springboot_api.dto.admin.subject.SubjectResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller read-only cho Lecturer lấy thông tin môn học.
 */
@RestController
@RequestMapping("/lecturer/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Lecturer - Subject", description = "API lấy thông tin môn học dành cho Giảng viên (read-only)")
public class LecturerSubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Lấy danh sách môn học", description = "Lấy danh sách môn học với phân trang và filter theo ngành")
    public PagedResponse<SubjectResponse> list(@ParameterObject @ModelAttribute ListSubjectRequest req) {
        return subjectService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết môn học", description = "Lấy thông tin chi tiết môn học bao gồm các ngành áp dụng")
    public SubjectDetailResponse getDetail(@PathVariable UUID id) {
        return subjectService.getDetail(id);
    }
}

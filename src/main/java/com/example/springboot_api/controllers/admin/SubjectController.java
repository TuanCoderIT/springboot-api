package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.subject.CreateSubjectRequest;
import com.example.springboot_api.dto.admin.subject.ListSubjectRequest;
import com.example.springboot_api.dto.admin.subject.SubjectDetailResponse;
import com.example.springboot_api.dto.admin.subject.SubjectResponse;
import com.example.springboot_api.dto.admin.subject.UpdateSubjectRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý Subject cho Admin.
 */
@RestController
@RequestMapping("/admin/subject")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Subject Management", description = "API quản lý môn học dành cho Admin")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Lấy danh sách môn học", description = "Lấy danh sách môn học với phân trang, search, filter theo isActive và majorId")
    public PagedResponse<SubjectResponse> list(@ParameterObject @ModelAttribute ListSubjectRequest req) {
        return subjectService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết môn học", description = "Lấy thông tin chi tiết môn học bao gồm danh sách ngành học có môn này")
    public SubjectDetailResponse getDetail(@PathVariable UUID id) {
        return subjectService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo môn học mới", description = "Tạo môn học mới với mã code duy nhất")
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest req) {
        return subjectService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật môn học", description = "Cập nhật thông tin môn học theo ID")
    public SubjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSubjectRequest req) {
        return subjectService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa môn học", description = "Xóa môn học (chỉ được xóa nếu không có phân công giảng dạy và không thuộc chương trình đào tạo)")
    public void delete(@PathVariable UUID id) {
        subjectService.delete(id);
    }
}

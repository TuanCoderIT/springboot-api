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

import com.example.springboot_api.dto.admin.term.CreateTermRequest;
import com.example.springboot_api.dto.admin.term.ListTermRequest;
import com.example.springboot_api.dto.admin.term.TermDetailResponse;
import com.example.springboot_api.dto.admin.term.TermResponse;
import com.example.springboot_api.dto.admin.term.UpdateTermRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.TermService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý Term cho Admin.
 */
@RestController
@RequestMapping("/admin/term")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Term Management", description = "API quản lý học kỳ dành cho Admin")
public class TermController {

    private final TermService termService;

    @GetMapping
    @Operation(summary = "Lấy danh sách học kỳ", description = "Lấy danh sách học kỳ với phân trang, search và filter")
    public PagedResponse<TermResponse> list(@ParameterObject @ModelAttribute ListTermRequest req) {
        return termService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết học kỳ", description = "Lấy thông tin chi tiết học kỳ bao gồm danh sách môn học được mở và số giảng viên")
    public TermDetailResponse getDetail(@PathVariable UUID id) {
        return termService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo học kỳ mới", description = "Tạo học kỳ mới với mã code duy nhất")
    public TermResponse create(@Valid @RequestBody CreateTermRequest req) {
        return termService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật học kỳ", description = "Cập nhật thông tin học kỳ theo ID")
    public TermResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTermRequest req) {
        return termService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa học kỳ", description = "Xóa học kỳ khỏi hệ thống (chỉ được xóa nếu không có phân công giảng dạy)")
    public void delete(@PathVariable UUID id) {
        termService.delete(id);
    }
}

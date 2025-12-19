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

import com.example.springboot_api.dto.admin.lecturer.CreateLecturerRequest;
import com.example.springboot_api.dto.admin.lecturer.LecturerResponse;
import com.example.springboot_api.dto.admin.lecturer.ListLecturerRequest;
import com.example.springboot_api.dto.admin.lecturer.UpdateLecturerRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.LecturerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/lecturer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Lecturer Management", description = "API quản lý giảng viên dành cho Admin")
public class LecturerController {

    private final LecturerService lecturerService;

    @GetMapping
    @Operation(summary = "Lấy danh sách giảng viên", description = "Lấy danh sách giảng viên với phân trang và filter")
    public PagedResponse<LecturerResponse> list(@ParameterObject @ModelAttribute ListLecturerRequest req) {
        return lecturerService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin giảng viên", description = "Lấy thông tin chi tiết một giảng viên theo ID")
    public LecturerResponse getOne(@PathVariable UUID id) {
        return lecturerService.getOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo giảng viên mới", description = "Tạo tài khoản giảng viên mới với role LECTURER")
    public LecturerResponse create(@Valid @RequestBody CreateLecturerRequest req) {
        return lecturerService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật giảng viên", description = "Cập nhật thông tin giảng viên theo ID")
    public LecturerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLecturerRequest req) {
        return lecturerService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa giảng viên", description = "Xóa giảng viên khỏi hệ thống")
    public void delete(@PathVariable UUID id) {
        lecturerService.delete(id);
    }
}

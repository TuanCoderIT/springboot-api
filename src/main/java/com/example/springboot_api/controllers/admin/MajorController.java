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

import com.example.springboot_api.dto.admin.major.CreateMajorRequest;
import com.example.springboot_api.dto.admin.major.ListMajorRequest;
import com.example.springboot_api.dto.admin.major.MajorDetailResponse;
import com.example.springboot_api.dto.admin.major.MajorResponse;
import com.example.springboot_api.dto.admin.major.UpdateMajorRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.MajorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý Major cho Admin.
 */
@RestController
@RequestMapping("/admin/major")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Major Management", description = "API quản lý ngành học dành cho Admin")
public class MajorController {

    private final MajorService majorService;

    @GetMapping
    @Operation(summary = "Lấy danh sách ngành học", description = "Lấy danh sách ngành học với phân trang, search, filter theo isActive và orgUnitId")
    public PagedResponse<MajorResponse> list(@ParameterObject @ModelAttribute ListMajorRequest req) {
        return majorService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết ngành học", description = "Lấy thông tin chi tiết ngành học bao gồm chương trình đào tạo (danh sách môn học)")
    public MajorDetailResponse getDetail(@PathVariable UUID id) {
        return majorService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo ngành học mới", description = "Tạo ngành học mới với mã code duy nhất")
    public MajorResponse create(@Valid @RequestBody CreateMajorRequest req) {
        return majorService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ngành học", description = "Cập nhật thông tin ngành học theo ID")
    public MajorResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMajorRequest req) {
        return majorService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa ngành học", description = "Xóa ngành học (chỉ được xóa nếu không có sinh viên và không có môn học)")
    public void delete(@PathVariable UUID id) {
        majorService.delete(id);
    }
}

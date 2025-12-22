package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.major.ListMajorRequest;
import com.example.springboot_api.dto.admin.major.MajorDetailResponse;
import com.example.springboot_api.dto.admin.major.MajorResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.MajorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller read-only cho Lecturer lấy thông tin ngành học.
 */
@RestController
@RequestMapping("/lecturer/majors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Lecturer - Major", description = "API lấy thông tin ngành học dành cho Giảng viên (read-only)")
public class LecturerMajorController {

    private final MajorService majorService;

    @GetMapping
    @Operation(summary = "Lấy danh sách ngành học", description = "Lấy danh sách ngành học với phân trang và filter")
    public PagedResponse<MajorResponse> list(@ParameterObject @ModelAttribute ListMajorRequest req) {
        return majorService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết ngành học", description = "Lấy thông tin chi tiết ngành học bao gồm danh sách môn học")
    public MajorDetailResponse getDetail(@PathVariable UUID id) {
        return majorService.getDetail(id);
    }
}

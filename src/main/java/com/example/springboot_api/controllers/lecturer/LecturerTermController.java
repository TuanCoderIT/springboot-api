package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.term.ListTermRequest;
import com.example.springboot_api.dto.admin.term.TermDetailResponse;
import com.example.springboot_api.dto.admin.term.TermResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.TermService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller read-only cho Lecturer lấy thông tin học kỳ.
 */
@RestController
@RequestMapping("/lecturer/terms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Lecturer - Term", description = "API lấy thông tin học kỳ dành cho Giảng viên (read-only)")
public class LecturerTermController {

    private final TermService termService;

    @GetMapping
    @Operation(summary = "Lấy danh sách học kỳ khả dụng", description = "Lấy danh sách học kỳ còn khả dụng (endDate >= hôm nay)")
    public PagedResponse<TermResponse> list(@ParameterObject @ModelAttribute ListTermRequest req) {
        return termService.listAvailable(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết học kỳ", description = "Lấy thông tin chi tiết học kỳ theo ID")
    public TermDetailResponse getDetail(@PathVariable UUID id) {
        return termService.getDetail(id);
    }
}

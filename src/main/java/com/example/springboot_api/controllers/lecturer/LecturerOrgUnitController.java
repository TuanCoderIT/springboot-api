package com.example.springboot_api.controllers.lecturer;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.orgunit.ListOrgUnitRequest;
import com.example.springboot_api.dto.admin.orgunit.OrgUnitResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.OrgUnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller read-only cho Lecturer lấy thông tin đơn vị tổ chức.
 */
@RestController
@RequestMapping("/lecturer/org-units")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Lecturer - OrgUnit", description = "API lấy thông tin đơn vị tổ chức dành cho Giảng viên (read-only)")
public class LecturerOrgUnitController {

    private final OrgUnitService orgUnitService;

    @GetMapping
    @Operation(summary = "Lấy danh sách đơn vị tổ chức", description = "Lấy danh sách đơn vị với phân trang và filter")
    public PagedResponse<OrgUnitResponse> list(@ParameterObject @ModelAttribute ListOrgUnitRequest req) {
        return orgUnitService.list(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết đơn vị tổ chức", description = "Lấy thông tin chi tiết đơn vị theo ID")
    public OrgUnitResponse getOne(@PathVariable UUID id) {
        return orgUnitService.getOne(id);
    }
}

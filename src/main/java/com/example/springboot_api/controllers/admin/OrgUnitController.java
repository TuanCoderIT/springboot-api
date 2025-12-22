package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.dto.admin.orgunit.CreateOrgUnitRequest;
import com.example.springboot_api.dto.admin.orgunit.ListOrgUnitRequest;
import com.example.springboot_api.dto.admin.orgunit.OrgUnitResponse;
import com.example.springboot_api.dto.admin.orgunit.UpdateOrgUnitRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.OrgUnitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý đơn vị tổ chức (OrgUnit).
 */
@RestController
@RequestMapping("/admin/org-units")
@RequiredArgsConstructor
public class OrgUnitController {

    private final OrgUnitService orgUnitService;

    /**
     * Lấy danh sách đơn vị tổ chức với phân trang.
     * GET /admin/org-units
     */
    @GetMapping
    public ResponseEntity<PagedResponse<OrgUnitResponse>> list(@ModelAttribute ListOrgUnitRequest req) {
        return ResponseEntity.ok(orgUnitService.list(req));
    }

    /**
     * Lấy thông tin chi tiết một đơn vị tổ chức.
     * GET /admin/org-units/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrgUnitResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(orgUnitService.getOne(id));
    }

    /**
     * Tạo đơn vị tổ chức mới.
     * POST /admin/org-units
     */
    @PostMapping
    public ResponseEntity<OrgUnitResponse> create(@Valid @RequestBody CreateOrgUnitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orgUnitService.create(req));
    }

    /**
     * Cập nhật đơn vị tổ chức.
     * PUT /admin/org-units/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrgUnitResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrgUnitRequest req) {
        return ResponseEntity.ok(orgUnitService.update(id, req));
    }

    /**
     * Xóa đơn vị tổ chức.
     * DELETE /admin/org-units/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orgUnitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.example.springboot_api.controllers.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.dto.ApiResponse;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.admin.regulation.GetRegulationFilesRequest;
import com.example.springboot_api.dto.admin.regulation.RegulationFileResponse;
import com.example.springboot_api.dto.admin.regulation.RegulationFileUploadRequest;
import com.example.springboot_api.dto.admin.regulation.RegulationNotebookResponse;
import com.example.springboot_api.dto.admin.regulation.RenameRegulationFileRequest;
import com.example.springboot_api.dto.admin.regulation.UpdateRegulationNotebookRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.services.admin.AdminRegulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin API quản lý tài liệu quy chế.
 */
@Slf4j
@RestController
@RequestMapping("/admin/regulation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Regulation", description = "API quản lý tài liệu quy chế")
public class AdminRegulationController {

    private final AdminRegulationService regulationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/notebook")
    @Operation(summary = "Lấy regulation notebook")
    public ResponseEntity<ApiResponse<RegulationNotebookResponse>> getRegulationNotebook() {
        RegulationNotebookResponse notebook = regulationService.getRegulationNotebook();
        return ResponseEntity.ok(ApiResponse.success(notebook, "Regulation notebook"));
    }

    @PutMapping("/notebook")
    @Operation(summary = "Cập nhật thông tin regulation notebook")
    public ResponseEntity<ApiResponse<RegulationNotebookResponse>> updateRegulationNotebook(
            @Valid @RequestBody UpdateRegulationNotebookRequest request) {

        RegulationNotebookResponse updated = regulationService.updateRegulationNotebook(request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Notebook updated successfully"));
    }

    @GetMapping("/files")
    @Operation(summary = "Lấy danh sách tài liệu quy chế (có phân trang, lọc, sắp xếp)")
    public ResponseEntity<ApiResponse<PagedResponse<RegulationFileResponse>>> getRegulationFiles(
            @Valid @ModelAttribute GetRegulationFilesRequest request) {

        PagedResponse<RegulationFileResponse> files = regulationService.getRegulationFiles(request);
        return ResponseEntity.ok(ApiResponse.success(files, "Regulation files"));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload tài liệu quy chế (PDF/DOCX)")
    public ResponseEntity<ApiResponse<List<RegulationFileResponse>>> uploadRegulationFiles(
            @RequestPart(value = "files") List<MultipartFile> files,
            @Valid @ModelAttribute RegulationFileUploadRequest request) {

        try {
            UUID adminId = currentUserProvider.getCurrentUserId();
            List<RegulationFileResponse> responses = regulationService.uploadRegulationFiles(
                    adminId, files, request);

            return ResponseEntity.ok(ApiResponse.success(
                    responses,
                    String.format("Uploaded %d file(s)", files.size())));
        } catch (Exception e) {
            log.error("Failed to upload regulation files", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    @PutMapping("/files/{id}/rename")
    @Operation(summary = "Đổi tên file (không đổi đuôi)")
    public ResponseEntity<ApiResponse<RegulationFileResponse>> renameFile(
            @PathVariable UUID id,
            @Valid @RequestBody RenameRegulationFileRequest request) {

        RegulationFileResponse response = regulationService.renameFile(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "File renamed successfully"));
    }

    @PostMapping("/files/{id}/retry-ocr")
    @Operation(summary = "Retry OCR cho file bị lỗi")
    public ResponseEntity<ApiResponse<RegulationFileResponse>> retryOcr(@PathVariable UUID id) {
        RegulationFileResponse response = regulationService.retryOcr(id);
        return ResponseEntity.ok(ApiResponse.success(response, "OCR retry started"));
    }

    @DeleteMapping("/files/{id}")
    @Operation(summary = "Xóa tài liệu quy chế")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID id) {
        regulationService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
    }
}

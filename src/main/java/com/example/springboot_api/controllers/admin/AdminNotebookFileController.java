package com.example.springboot_api.controllers.admin;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.admin.notebook.ContributorInfo;
import com.example.springboot_api.dto.admin.notebook.NotebookFileResponse;
import com.example.springboot_api.dto.admin.notebook.PageResponse;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.services.admin.AdminNotebookFileService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/notebooks/{notebookId}/files")
@RequiredArgsConstructor
public class AdminNotebookFileController {

    private final AdminNotebookFileService adminNotebookFileService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = { "multipart/form-data" })
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotebookFileResponse> uploadFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @RequestPart("request") String requestJson,
            @RequestPart("files") List<MultipartFile> files)
            throws IOException {

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một file để upload.");
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("File không được để trống.");
            }
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String lower = filename.toLowerCase();
                boolean isValid = lower.endsWith(".pdf") ||
                        lower.endsWith(".doc") ||
                        lower.endsWith(".docx");
                if (!isValid) {
                    throw new BadRequestException(
                            "Chỉ chấp nhận file PDF và Word (.doc, .docx). File không hợp lệ: " + filename);
                }
            }
        }

        FileUploadRequest request;
        try {
            request = objectMapper.readValue(requestJson, FileUploadRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Dữ liệu cấu hình (request) không hợp lệ: " + e.getMessage());
        }

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessage = violations.iterator().next().getMessage();
            throw new BadRequestException("Lỗi tham số: " + errorMessage);
        }

        return adminNotebookFileService.uploadFiles(admin.getId(), notebookId, request, files)
                .stream()
                .map(file -> adminNotebookFileService.toResponse(file, null))
                .toList();
    }

    @GetMapping
    public PageResponse<NotebookFileResponse> getFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) Boolean ocrDone,
            @RequestParam(required = false) Boolean embeddingDone,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.getFiles(
                notebookId, status, mimeType, ocrDone, embeddingDone, uploadedBy, search, sortBy, page, size);
    }

    @GetMapping("/contributors")
    public List<ContributorInfo> getContributors(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String search) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.getContributors(notebookId, search);
    }

    @GetMapping("/pending")
    public PageResponse<NotebookFileResponse> getPendingFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.getPendingFiles(
                notebookId, mimeType, uploadedBy, search, sortBy, page, size);
    }

    @PutMapping("/{fileId}/approve")
    public NotebookFileResponse approve(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        NotebookFile approvedFile = adminNotebookFileService.approveFile(admin.getId(), notebookId, fileId);
        long chunksCount = adminNotebookFileService.getChunksCount(approvedFile.getId());
        return adminNotebookFileService.toResponse(approvedFile, chunksCount);
    }

    @PutMapping("/{fileId}/reject")
    public NotebookFileResponse reject(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        NotebookFile rejectedFile = adminNotebookFileService.rejectFile(admin.getId(), notebookId, fileId);
        long chunksCount = adminNotebookFileService.getChunksCount(rejectedFile.getId());
        return adminNotebookFileService.toResponse(rejectedFile, chunksCount);
    }

    @PutMapping("/approve-all")
    public java.util.Map<String, Object> approveAll(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        int count = adminNotebookFileService.approveAllFilesByNotebook(admin.getId(), notebookId);
        return java.util.Map.of("approvedCount", count, "message", "Đã duyệt " + count + " file(s)");
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        adminNotebookFileService.deleteFile(admin.getId(), notebookId, fileId);
    }
}
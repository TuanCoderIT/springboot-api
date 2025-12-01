package com.example.springboot_api.controllers.admin;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.services.admin.AdminNotebookFileService;
// CÁC IMPORTS MỚI CHO VIỆC TỰ XỬ LÝ JSON VÀ VALIDATION
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/notebooks/{notebookId}/files")
@RequiredArgsConstructor
public class AdminNotebookFileController {

    private final AdminNotebookFileService adminNotebookFileService;
    private final ObjectMapper objectMapper; // 🟢 NEW: Inject ObjectMapper để xử lý JSON
    private final Validator validator; // 🟢 NEW: Inject Validator để tự kiểm tra @Valid

    @PostMapping(consumes = { "multipart/form-data" })
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotebookFile> uploadFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @RequestPart("request") String reqJson, // 🟢 SỬA: Nhận JSON dưới dạng String
            @RequestPart("files") List<MultipartFile> files)
            throws IOException {

        // if (admin == null)
        // throw new RuntimeException("Admin chưa đăng nhập.");

        // 1. TỰ DESERIALIZE (KHẮC PHỤC LỖI CONTENT-TYPE)
        FileUploadRequest req;
        try {
            req = objectMapper.readValue(reqJson, FileUploadRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Dữ liệu cấu hình (request) không hợp lệ. Vui lòng kiểm tra cú pháp JSON.");
        }

        // 2. TỰ VALIDATE (thay thế @Valid)
        var violations = validator.validate(req);
        if (!violations.isEmpty()) {
            // Lấy lỗi đầu tiên để hiển thị chi tiết
            String errorMessage = violations.iterator().next().getMessage();
            throw new BadRequestException("Lỗi tham số chunking: " + errorMessage);
        }

        return adminNotebookFileService.uploadFiles(admin.getId(), notebookId, req, files);
    }

    @GetMapping("/pending")
    public List<NotebookFile> getPendingFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.getPendingFiles(notebookId);
    }

    @PutMapping("/{fileId}/approve")
    public NotebookFile approve(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.approveFile(admin.getId(), notebookId, fileId);
    }

    @PutMapping("/{fileId}/reject")
    public NotebookFile reject(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        return adminNotebookFileService.rejectFile(admin.getId(), notebookId, fileId);
    }
}
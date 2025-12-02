package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.admin.notebook.NotebookFileResponse;
import com.example.springboot_api.dto.admin.notebook.PageResponse;
import com.example.springboot_api.services.admin.AdminNotebookFileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/files/pending")
@RequiredArgsConstructor
public class AdminPendingFileController {

    private final AdminNotebookFileService adminNotebookFileService;

    @GetMapping
    public PageResponse<NotebookFileResponse> getPendingFiles(
            @AuthenticationPrincipal UserPrincipal admin,
            @RequestParam(required = false) UUID notebookId,
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

    @PutMapping("/approve-all")
    public java.util.Map<String, Object> approveAll(
            @AuthenticationPrincipal UserPrincipal admin,
            @RequestParam(required = false) UUID notebookId) {

        if (admin == null)
            throw new RuntimeException("Admin chưa đăng nhập.");

        int count;
        if (notebookId != null) {
            count = adminNotebookFileService.approveAllFilesByNotebook(admin.getId(), notebookId);
        } else {
            count = adminNotebookFileService.approveAllFiles(admin.getId());
        }

        return java.util.Map.of("approvedCount", count, "message", "Đã duyệt " + count + " file(s)");
    }
}


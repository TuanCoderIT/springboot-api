package com.example.springboot_api.controllers.user;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;
import com.example.springboot_api.dto.user.notebook.FileChunkResponse;
import com.example.springboot_api.dto.user.notebook.UserNotebookFileDetailResponse;
import com.example.springboot_api.services.user.UserNotebookFileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/notebooks/{notebookId}/files")
@RequiredArgsConstructor
public class UserNotebookFileController {

    private final UserNotebookFileService userNotebookFileService;

    @PostMapping(consumes = { "multipart/form-data" })
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotebookFileResponse> uploadFiles(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestPart("files") List<MultipartFile> files)
            throws IOException {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

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

        return userNotebookFileService.uploadFiles(user.getId(), notebookId, files)
                .stream()
                .map(userNotebookFileService::toResponse)
                .toList();
    }

    /**
     * Lấy danh sách file theo notebookId:
     * - File có status = 'done' (đã duyệt và xử lý xong)
     * - File của user hiện tại với các status khác (pending, failed, rejected,
     * processing)
     * - Có tìm kiếm theo tên file (optional)
     * 
     * GET /user/notebooks/{notebookId}/files
     * GET /user/notebooks/{notebookId}/files?search=document
     */
    @GetMapping
    public List<NotebookFileResponse> getFiles(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String search) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        return userNotebookFileService.getFilesForUser(user.getId(), notebookId, search)
                .stream()
                .map(userNotebookFileService::toResponse)
                .toList();
    }

    @GetMapping("/{fileId}")
    public UserNotebookFileDetailResponse getFileDetail(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        return userNotebookFileService.getFileDetail(user.getId(), notebookId, fileId);
    }

    @GetMapping("/{fileId}/chunks")
    public List<FileChunkResponse> getFileChunks(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        return userNotebookFileService.getFileChunks(user.getId(), notebookId, fileId);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID fileId) {

        if (user == null)
            throw new RuntimeException("User chưa đăng nhập.");

        userNotebookFileService.deleteFile(user.getId(), notebookId, fileId);
    }

}

package com.example.springboot_api.controllers.lecturer;

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
import com.example.springboot_api.dto.lecturer.notebook.LecturerNotebookFileResponse;
import com.example.springboot_api.dto.user.notebook.FileUploadRequest;
import com.example.springboot_api.services.lecturer.LecturerNotebookFileService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lecturer/notebooks")
@RequiredArgsConstructor
@Tag(name = "Lecturer Notebook Files", description = "API quản lý files notebook cho giảng viên")
public class LecturerNotebookFileController {

    private final LecturerNotebookFileService lecturerNotebookFileService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Upload files vào notebook (Simplified version)
     * Lecturer có thể upload files vào notebooks mà họ có quyền quản lý
     */
    @PostMapping(value = "/{notebookId}/files/simple", consumes = { "multipart/form-data" })
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload files to notebook (Simple)", description = "Upload files vào notebook với cấu hình mặc định")
    public List<LecturerNotebookFileResponse> uploadFilesSimple(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Notebook ID") @PathVariable UUID notebookId,
            @Parameter(description = "Files to upload") @RequestPart("files") List<MultipartFile> files) 
            throws IOException {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một file để upload.");
        }

        // Validate file types
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

        // Sử dụng cấu hình mặc định
        FileUploadRequest defaultRequest = new FileUploadRequest();
        defaultRequest.setChunkSize(3000);
        defaultRequest.setChunkOverlap(250);

        return lecturerNotebookFileService.uploadFiles(lecturer.getId(), notebookId, defaultRequest, files);
    }

    /**
     * Upload files vào notebook (Advanced version with configuration)
     * Lecturer có thể upload files vào notebooks mà họ có quyền quản lý
     */
    @PostMapping(value = "/{notebookId}/files", consumes = { "multipart/form-data" })
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload files to notebook (Advanced)", description = "Upload files vào notebook với cấu hình tùy chỉnh")
    public List<LecturerNotebookFileResponse> uploadFiles(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Notebook ID") @PathVariable UUID notebookId,
            @Parameter(description = "Upload configuration") @RequestPart("request") String requestJson,
            @Parameter(description = "Files to upload") @RequestPart("files") List<MultipartFile> files) 
            throws IOException {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một file để upload.");
        }

        // Validate file types
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

        // Parse and validate request
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

        return lecturerNotebookFileService.uploadFiles(lecturer.getId(), notebookId, request, files);
    }

    /**
     * Lấy danh sách files từ một notebook cụ thể
     * Chỉ lấy files có status = 'done' (đã xử lý xong) để dùng cho tạo câu hỏi
     */
    @GetMapping("/{notebookId}/files")
    @Operation(summary = "Get files by notebook", description = "Lấy danh sách files đã xử lý xong từ notebook để tạo câu hỏi")
    public List<LecturerNotebookFileResponse> getFilesByNotebook(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Notebook ID") @PathVariable UUID notebookId,
            @Parameter(description = "Tìm kiếm theo tên file") @RequestParam(required = false) String search) {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        return lecturerNotebookFileService.getFilesByNotebook(lecturer.getId(), notebookId, search);
    }

    /**
     * Lấy tất cả files từ các notebooks mà lecturer có quyền truy cập
     * Hữu ích cho trang tạo câu hỏi AI khi muốn chọn files từ nhiều notebook
     */
    @GetMapping("/files")
    @Operation(summary = "Get all accessible files", description = "Lấy tất cả files mà lecturer có thể truy cập để tạo câu hỏi")
    public List<LecturerNotebookFileResponse> getAllAccessibleFiles(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Tìm kiếm theo tên file") @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo notebook ID") @RequestParam(required = false) UUID notebookId,
            @Parameter(description = "Giới hạn số lượng kết quả") @RequestParam(required = false, defaultValue = "100") int limit) {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        return lecturerNotebookFileService.getAllAccessibleFiles(lecturer.getId(), search, notebookId, limit);
    }

    /**
     * Lấy danh sách notebooks mà lecturer có quyền truy cập
     * Để hiển thị dropdown chọn notebook trong giao diện
     */
    @GetMapping("/accessible")
    @Operation(summary = "Get accessible notebooks", description = "Lấy danh sách notebooks mà lecturer có thể truy cập")
    public List<com.example.springboot_api.dto.lecturer.notebook.LecturerNotebookSummary> getAccessibleNotebooks(
            @AuthenticationPrincipal UserPrincipal lecturer) {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        return lecturerNotebookFileService.getAccessibleNotebooks(lecturer.getId());
    }

    /**
     * Lấy thông tin chi tiết của một file
     * Để hiển thị preview nội dung file khi chọn
     */
    @GetMapping("/{notebookId}/files/{fileId}")
    @Operation(summary = "Get file detail", description = "Lấy thông tin chi tiết của file")
    public com.example.springboot_api.dto.lecturer.notebook.LecturerFileDetailResponse getFileDetail(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Notebook ID") @PathVariable UUID notebookId,
            @Parameter(description = "File ID") @PathVariable UUID fileId) {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        return lecturerNotebookFileService.getFileDetail(lecturer.getId(), notebookId, fileId);
    }

    /**
     * Xóa file khỏi notebook
     * Lecturer có thể xóa files mà họ đã upload hoặc có quyền quản lý
     */
    @DeleteMapping("/{notebookId}/files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete file", description = "Xóa file khỏi notebook")
    public void deleteFile(
            @AuthenticationPrincipal UserPrincipal lecturer,
            @Parameter(description = "Notebook ID") @PathVariable UUID notebookId,
            @Parameter(description = "File ID") @PathVariable UUID fileId) {

        if (lecturer == null)
            throw new RuntimeException("Lecturer chưa đăng nhập.");

        lecturerNotebookFileService.deleteFile(lecturer.getId(), notebookId, fileId);
    }
}
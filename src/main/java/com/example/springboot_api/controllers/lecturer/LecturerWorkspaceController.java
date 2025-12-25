package com.example.springboot_api.controllers.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.dto.ApiResponse;
import com.example.springboot_api.dto.lecturer.workspace.LecturerWorkspaceFileRequest;
import com.example.springboot_api.dto.lecturer.workspace.LecturerWorkspaceFileResponse;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceAiRequest;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceAiResponse;
import com.example.springboot_api.services.lecturer.LecturerWorkspaceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller cho AI Workspace của giảng viên.
 * Tái sử dụng toàn bộ logic AI từ notebook user, không duplicate code.
 * 
 * Concept: Workspace = Notebook với type="class"
 * - Giảng viên quản lý tài liệu và tạo AI content
 * - Tất cả AI features đều dùng chung pipeline existing
 */
@Slf4j
@RestController
@RequestMapping("/api/lecturer/workspace")
@RequiredArgsConstructor
@Tag(name = "Lecturer AI Workspace", description = "API quản lý AI Workspace cho giảng viên")
public class LecturerWorkspaceController {
    
    private final LecturerWorkspaceService workspaceService;
    
    // ==================== FILE MANAGEMENT ====================
    
    @PostMapping(value = "/{notebookId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload tài liệu vào workspace", 
               description = "Upload và quản lý tài liệu cho workspace của giảng viên")
    public ResponseEntity<ApiResponse<LecturerWorkspaceFileResponse>> uploadFile(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "File tài liệu cần upload") 
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "Thông tin bổ sung về tài liệu") 
            @Valid @ModelAttribute LecturerWorkspaceFileRequest request) {
        
        try {
            log.info("Uploading file to workspace {} by lecturer {}", notebookId, lecturerId);
            
            LecturerWorkspaceFileResponse response = workspaceService.uploadFile(
                notebookId, lecturerId, request, file);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Upload tài liệu thành công"));
            
        } catch (Exception e) {
            log.error("Error uploading file to workspace: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi upload tài liệu: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{notebookId}/files")
    @Operation(summary = "Lấy danh sách tài liệu trong workspace", 
               description = "Lấy danh sách tài liệu đã upload trong workspace")
    public ResponseEntity<ApiResponse<List<LecturerWorkspaceFileResponse>>> getWorkspaceFiles(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Lọc theo chương (optional)") 
            @RequestParam(required = false) String chapter) {
        
        try {
            List<LecturerWorkspaceFileResponse> files = workspaceService.getWorkspaceFiles(
                notebookId, lecturerId, chapter);
            
            return ResponseEntity.ok(ApiResponse.success(files, 
                "Lấy danh sách tài liệu thành công"));
            
        } catch (Exception e) {
            log.error("Error getting workspace files: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi lấy danh sách tài liệu: " + e.getMessage()));
        }
    }
    
    // ==================== AI CONTENT GENERATION ====================
    
    @PostMapping("/{notebookId}/ai/summary")
    @Operation(summary = "Tạo tóm tắt AI", 
               description = "Tạo tóm tắt nội dung từ tài liệu bằng AI (tái sử dụng logic notebook)")
    public ResponseEntity<ApiResponse<WorkspaceAiResponse>> generateSummary(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin yêu cầu tạo tóm tắt") 
            @Valid @RequestBody WorkspaceAiRequest request) {
        
        try {
            log.info("Generating summary for workspace {} by lecturer {}", notebookId, lecturerId);
            
            WorkspaceAiResponse response = workspaceService.generateSummary(
                notebookId, lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Đã bắt đầu tạo tóm tắt AI"));
            
        } catch (Exception e) {
            log.error("Error generating summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi tạo tóm tắt: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{notebookId}/ai/quiz")
    @Operation(summary = "Tạo quiz AI", 
               description = "Tạo bộ câu hỏi trắc nghiệm từ tài liệu bằng AI")
    public ResponseEntity<ApiResponse<WorkspaceAiResponse>> generateQuiz(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin yêu cầu tạo quiz") 
            @Valid @RequestBody WorkspaceAiRequest request) {
        
        try {
            log.info("Generating quiz for workspace {} by lecturer {}", notebookId, lecturerId);
            
            WorkspaceAiResponse response = workspaceService.generateQuiz(
                notebookId, lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Đã bắt đầu tạo quiz AI"));
            
        } catch (Exception e) {
            log.error("Error generating quiz: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi tạo quiz: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{notebookId}/ai/flashcard")
    @Operation(summary = "Tạo flashcard AI", 
               description = "Tạo bộ thẻ ghi nhớ từ tài liệu bằng AI")
    public ResponseEntity<ApiResponse<WorkspaceAiResponse>> generateFlashcard(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin yêu cầu tạo flashcard") 
            @Valid @RequestBody WorkspaceAiRequest request) {
        
        try {
            log.info("Generating flashcard for workspace {} by lecturer {}", notebookId, lecturerId);
            
            WorkspaceAiResponse response = workspaceService.generateFlashcard(
                notebookId, lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Đã bắt đầu tạo flashcard AI"));
            
        } catch (Exception e) {
            log.error("Error generating flashcard: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi tạo flashcard: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{notebookId}/ai/video")
    @Operation(summary = "Tạo video learning content AI", 
               description = "Tạo nội dung video học tập từ tài liệu bằng AI")
    public ResponseEntity<ApiResponse<WorkspaceAiResponse>> generateVideo(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin yêu cầu tạo video") 
            @Valid @RequestBody WorkspaceAiRequest request) {
        
        try {
            log.info("Generating video for workspace {} by lecturer {}", notebookId, lecturerId);
            
            WorkspaceAiResponse response = workspaceService.generateVideo(
                notebookId, lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Đã bắt đầu tạo video learning content AI"));
            
        } catch (Exception e) {
            log.error("Error generating video: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi tạo video: " + e.getMessage()));
        }
    }
    
    // ==================== AI CONTENT MANAGEMENT ====================
    
    @GetMapping("/{notebookId}/ai/content")
    @Operation(summary = "Lấy danh sách AI content đã tạo", 
               description = "Lấy danh sách nội dung AI đã tạo trong workspace")
    public ResponseEntity<ApiResponse<List<WorkspaceAiResponse>>> getWorkspaceAiContent(
            @Parameter(description = "ID của workspace (notebook)") 
            @PathVariable UUID notebookId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Lọc theo loại content (optional): summary, quiz, flashcard, video") 
            @RequestParam(required = false) String contentType) {
        
        try {
            List<WorkspaceAiResponse> content = workspaceService.getWorkspaceAiContent(
                notebookId, lecturerId, contentType);
            
            return ResponseEntity.ok(ApiResponse.success(content, 
                "Lấy danh sách AI content thành công"));
            
        } catch (Exception e) {
            log.error("Error getting workspace AI content: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi lấy danh sách AI content: " + e.getMessage()));
        }
    }
}
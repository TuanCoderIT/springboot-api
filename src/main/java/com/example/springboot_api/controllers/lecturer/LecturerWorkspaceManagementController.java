package com.example.springboot_api.controllers.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.common.dto.ApiResponse;
import com.example.springboot_api.dto.lecturer.workspace.CreateWorkspaceRequest;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceResponse;
import com.example.springboot_api.services.lecturer.LecturerWorkspaceManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller quản lý workspace (class notebook) cho giảng viên.
 * Tạo, cập nhật, xóa và quản lý workspace.
 */
@Slf4j
@RestController
@RequestMapping("/api/lecturer/workspace-management")
@RequiredArgsConstructor
@Tag(name = "Lecturer Workspace Management", description = "API quản lý workspace cho giảng viên")
public class LecturerWorkspaceManagementController {
    
    private final LecturerWorkspaceManagementService workspaceManagementService;
    
    @PostMapping
    @Operation(summary = "Tạo workspace mới", 
               description = "Tạo workspace (lớp học phần) mới cho giảng viên")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin workspace cần tạo") 
            @Valid @RequestBody CreateWorkspaceRequest request) {
        
        try {
            log.info("Creating workspace for lecturer {}: {}", lecturerId, request.getTitle());
            
            WorkspaceResponse response = workspaceManagementService.createWorkspace(lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Tạo workspace thành công"));
            
        } catch (Exception e) {
            log.error("Error creating workspace: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi tạo workspace: " + e.getMessage()));
        }
    }
    
    @GetMapping
    @Operation(summary = "Lấy danh sách workspace", 
               description = "Lấy danh sách workspace của giảng viên")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getLecturerWorkspaces(
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId) {
        
        try {
            List<WorkspaceResponse> workspaces = workspaceManagementService.getLecturerWorkspaces(lecturerId);
            
            return ResponseEntity.ok(ApiResponse.success(workspaces, 
                "Lấy danh sách workspace thành công"));
            
        } catch (Exception e) {
            log.error("Error getting lecturer workspaces: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi lấy danh sách workspace: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{workspaceId}")
    @Operation(summary = "Lấy thông tin chi tiết workspace", 
               description = "Lấy thông tin chi tiết của một workspace")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspaceDetails(
            @Parameter(description = "ID của workspace") 
            @PathVariable UUID workspaceId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId) {
        
        try {
            WorkspaceResponse workspace = workspaceManagementService.getWorkspaceDetails(workspaceId, lecturerId);
            
            return ResponseEntity.ok(ApiResponse.success(workspace, 
                "Lấy thông tin workspace thành công"));
            
        } catch (Exception e) {
            log.error("Error getting workspace details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi lấy thông tin workspace: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{workspaceId}")
    @Operation(summary = "Cập nhật thông tin workspace", 
               description = "Cập nhật thông tin workspace (chỉ owner)")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @Parameter(description = "ID của workspace") 
            @PathVariable UUID workspaceId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId,
            
            @Parameter(description = "Thông tin cập nhật workspace") 
            @Valid @RequestBody CreateWorkspaceRequest request) {
        
        try {
            log.info("Updating workspace {} by lecturer {}", workspaceId, lecturerId);
            
            WorkspaceResponse response = workspaceManagementService.updateWorkspace(
                workspaceId, lecturerId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Cập nhật workspace thành công"));
            
        } catch (Exception e) {
            log.error("Error updating workspace: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi cập nhật workspace: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "Xóa workspace", 
               description = "Xóa workspace (chỉ owner)")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
            @Parameter(description = "ID của workspace") 
            @PathVariable UUID workspaceId,
            
            @Parameter(description = "ID của giảng viên") 
            @RequestHeader("X-User-Id") UUID lecturerId) {
        
        try {
            log.info("Deleting workspace {} by lecturer {}", workspaceId, lecturerId);
            
            workspaceManagementService.deleteWorkspace(workspaceId, lecturerId);
            
            return ResponseEntity.ok(ApiResponse.success(null, 
                "Xóa workspace thành công"));
            
        } catch (Exception e) {
            log.error("Error deleting workspace: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Lỗi xóa workspace: " + e.getMessage()));
        }
    }
}
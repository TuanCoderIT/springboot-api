package com.example.springboot_api.services.lecturer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.lecturer.workspace.CreateWorkspaceRequest;
import com.example.springboot_api.dto.lecturer.workspace.WorkspaceResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý workspace (class notebook) cho giảng viên.
 * Tạo, quản lý và phân quyền workspace.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LecturerWorkspaceManagementService {

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;

    /**
     * Tạo workspace mới cho giảng viên (tạo notebook với type="class").
     */
    @Transactional
    public WorkspaceResponse createWorkspace(UUID lecturerId, CreateWorkspaceRequest request) {
        log.info("Creating workspace for lecturer {}: {}", lecturerId, request.getTitle());
        
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new NotFoundException("Giảng viên không tồn tại"));
        
        // Validate lecturer role
        if (!"lecturer".equals(lecturer.getRole()) && !"admin".equals(lecturer.getRole())) {
            throw new ForbiddenException("Chỉ giảng viên mới có thể tạo workspace");
        }
        
        // Create notebook as workspace
        Notebook workspace = new Notebook();
        workspace.setTitle(request.getTitle());
        workspace.setDescription(request.getDescription());
        workspace.setType("class"); // Important: type="class" for lecturer workspace
        workspace.setVisibility("private"); // Default private, can be changed later
        workspace.setCreatedBy(lecturer);
        workspace.setThumbnailUrl(request.getThumbnailUrl());
        workspace.setCreatedAt(OffsetDateTime.now());
        workspace.setUpdatedAt(OffsetDateTime.now());
        
        // Set workspace metadata
        workspace.setMetadata(java.util.Map.of(
            "lecturerWorkspace", true,
            "subject", request.getSubject() != null ? request.getSubject() : "",
            "semester", request.getSemester() != null ? request.getSemester() : "",
            "academicYear", request.getAcademicYear() != null ? request.getAcademicYear() : ""
        ));
        
        Notebook savedWorkspace = notebookRepository.save(workspace);
        
        // Add lecturer as owner
        NotebookMember ownerMember = NotebookMember.builder()
                .notebook(savedWorkspace)
                .user(lecturer)
                .role("owner")
                .status("approved")
                .joinedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        memberRepository.save(ownerMember);
        
        return mapToWorkspaceResponse(savedWorkspace);
    }

    /**
     * Lấy danh sách workspace của giảng viên.
     */
    public List<WorkspaceResponse> getLecturerWorkspaces(UUID lecturerId) {
        log.info("Getting workspaces for lecturer {}", lecturerId);
        
        // Get all class notebooks where lecturer is owner or has lecturer role
        List<Notebook> workspaces = notebookRepository.findClassNotebooksByLecturerId(lecturerId);
        
        return workspaces.stream()
                .map(this::mapToWorkspaceResponse)
                .toList();
    }

    /**
     * Lấy thông tin chi tiết workspace.
     */
    public WorkspaceResponse getWorkspaceDetails(UUID notebookId, UUID lecturerId) {
        log.info("Getting workspace details {} for lecturer {}", notebookId, lecturerId);
        
        Notebook workspace = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Workspace không tồn tại"));
        
        // Validate permission
        validateLecturerPermission(workspace, lecturerId);
        
        return mapToWorkspaceResponse(workspace);
    }

    /**
     * Cập nhật thông tin workspace.
     */
    @Transactional
    public WorkspaceResponse updateWorkspace(UUID notebookId, UUID lecturerId, CreateWorkspaceRequest request) {
        log.info("Updating workspace {} by lecturer {}", notebookId, lecturerId);
        
        Notebook workspace = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Workspace không tồn tại"));
        
        // Validate permission (only owner can update)
        validateOwnerPermission(workspace, lecturerId);
        
        // Update workspace info
        workspace.setTitle(request.getTitle());
        workspace.setDescription(request.getDescription());
        workspace.setThumbnailUrl(request.getThumbnailUrl());
        workspace.setUpdatedAt(OffsetDateTime.now());
        
        // Update metadata
        java.util.Map<String, Object> metadata = workspace.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put("subject", request.getSubject() != null ? request.getSubject() : "");
        metadata.put("semester", request.getSemester() != null ? request.getSemester() : "");
        metadata.put("academicYear", request.getAcademicYear() != null ? request.getAcademicYear() : "");
        workspace.setMetadata(metadata);
        
        Notebook updated = notebookRepository.save(workspace);
        return mapToWorkspaceResponse(updated);
    }

    /**
     * Xóa workspace (chỉ owner).
     */
    @Transactional
    public void deleteWorkspace(UUID notebookId, UUID lecturerId) {
        log.info("Deleting workspace {} by lecturer {}", notebookId, lecturerId);
        
        Notebook workspace = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Workspace không tồn tại"));
        
        // Validate permission (only owner can delete)
        validateOwnerPermission(workspace, lecturerId);
        
        // Delete workspace (cascade will handle related data)
        notebookRepository.deleteById(notebookId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateLecturerPermission(Notebook workspace, UUID lecturerId) {
        if (!"class".equals(workspace.getType())) {
            throw new BadRequestException("Đây không phải là workspace lớp học phần");
        }
        
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(workspace.getId(), lecturerId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập workspace này"));
        
        if (!"owner".equals(member.getRole()) && !"lecturer".equals(member.getRole())) {
            throw new ForbiddenException("Chỉ giảng viên mới có quyền truy cập workspace này");
        }
        
        if (!"approved".equals(member.getStatus())) {
            throw new ForbiddenException("Tài khoản chưa được duyệt cho workspace này");
        }
    }

    private void validateOwnerPermission(Notebook workspace, UUID lecturerId) {
        if (!"class".equals(workspace.getType())) {
            throw new BadRequestException("Đây không phải là workspace lớp học phần");
        }
        
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(workspace.getId(), lecturerId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập workspace này"));
        
        if (!"owner".equals(member.getRole())) {
            throw new ForbiddenException("Chỉ chủ sở hữu mới có quyền thực hiện thao tác này");
        }
        
        if (!"approved".equals(member.getStatus())) {
            throw new ForbiddenException("Tài khoản chưa được duyệt cho workspace này");
        }
    }

    private WorkspaceResponse mapToWorkspaceResponse(Notebook workspace) {
        java.util.Map<String, Object> metadata = workspace.getMetadata();
        
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .title(workspace.getTitle())
                .description(workspace.getDescription())
                .thumbnailUrl(workspace.getThumbnailUrl())
                .subject(metadata != null ? (String) metadata.get("subject") : null)
                .semester(metadata != null ? (String) metadata.get("semester") : null)
                .academicYear(metadata != null ? (String) metadata.get("academicYear") : null)
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}
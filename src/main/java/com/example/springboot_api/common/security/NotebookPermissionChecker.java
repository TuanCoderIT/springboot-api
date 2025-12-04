package com.example.springboot_api.common.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Utility class để kiểm tra quyền của user trong notebook
 */
@Component
@RequiredArgsConstructor
public class NotebookPermissionChecker {

    private final NotebookMemberRepository memberRepository;

    /**
     * Kiểm tra user hiện tại có phải owner của notebook không
     */
    public boolean isOwner(UUID notebookId) {
        UserPrincipal user = getCurrentUser();
        if (user == null) return false;

        return memberRepository.findByNotebookIdAndUserId(notebookId, user.getId())
                .map(m -> "owner".equals(m.getRole()) && "approved".equals(m.getStatus()))
                .orElse(false);
    }

    /**
     * Kiểm tra user hiện tại có phải admin của notebook không (admin hoặc owner)
     */
    public boolean isAdmin(UUID notebookId) {
        UserPrincipal user = getCurrentUser();
        if (user == null) return false;

        return memberRepository.findByNotebookIdAndUserId(notebookId, user.getId())
                .map(m -> ("admin".equals(m.getRole()) || "owner".equals(m.getRole())) 
                        && "approved".equals(m.getStatus()))
                .orElse(false);
    }

    /**
     * Kiểm tra user hiện tại có phải member của notebook không (đã approved)
     */
    public boolean isMember(UUID notebookId) {
        UserPrincipal user = getCurrentUser();
        if (user == null) return false;

        return memberRepository.findByNotebookIdAndUserId(notebookId, user.getId())
                .map(m -> "approved".equals(m.getStatus()))
                .orElse(false);
    }

    /**
     * Lấy NotebookMember của user hiện tại trong notebook
     */
    public NotebookMember getMembership(UUID notebookId) {
        UserPrincipal user = getCurrentUser();
        if (user == null) return null;

        return memberRepository.findByNotebookIdAndUserId(notebookId, user.getId())
                .orElse(null);
    }

    /**
     * Kiểm tra user có phải system admin không (role ADMIN)
     */
    public boolean isSystemAdmin() {
        UserPrincipal user = getCurrentUser();
        if (user == null) return false;

        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Lấy user hiện tại từ SecurityContext
     */
    private UserPrincipal getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal) {
                return (UserPrincipal) principal;
            }
        } catch (Exception e) {
            // Nếu không có authentication hoặc lỗi, trả về null
        }
        return null;
    }
}


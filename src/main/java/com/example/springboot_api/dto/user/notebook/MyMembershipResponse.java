package com.example.springboot_api.dto.user.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO cho thông tin quyền của user trong notebook.
 */
public record MyMembershipResponse(
        UUID memberId,
        String role, // "owner" | "admin" | "member"
        String status, // "approved" | "pending" | "rejected"
        OffsetDateTime joinedAt,
        boolean canManageMembers, // Có thể quản lý thành viên không
        boolean canUploadFiles, // Có thể upload files không
        boolean canDeleteNotebook, // Có thể xóa notebook không
        boolean canEditNotebook // Có thể chỉnh sửa notebook không
) {
    /**
     * Factory method để tạo response với quyền tự động tính toán
     */
    public static MyMembershipResponse of(UUID memberId, String role, String status, OffsetDateTime joinedAt) {
        boolean isOwner = "owner".equals(role);
        boolean isAdmin = "admin".equals(role);
        boolean isApproved = "approved".equals(status);

        return new MyMembershipResponse(
                memberId,
                role,
                status,
                joinedAt,
                isApproved && (isOwner || isAdmin), // canManageMembers
                isApproved, // canUploadFiles
                isOwner, // canDeleteNotebook
                isOwner || isAdmin // canEditNotebook
        );
    }
}

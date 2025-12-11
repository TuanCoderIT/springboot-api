package com.example.springboot_api.controllers.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.notebook.MyMembershipResponse;
import com.example.springboot_api.dto.user.notebook.NotebookMembersResponse;
import com.example.springboot_api.services.user.UserNotebookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/notebooks")
@RequiredArgsConstructor
public class UserNotebookMemberController {

    private final UserNotebookService notebookService;

    /**
     * Lấy thông tin quyền của user hiện tại trong notebook
     * 
     * @param notebookId ID của notebook
     */
    @GetMapping("/{notebookId}/me")
    public MyMembershipResponse getMyMembership(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal UserPrincipal user) {
        return notebookService.getMyMembership(notebookId, user.getId());
    }

    /**
     * Lấy danh sách thành viên của notebook
     * - Chỉ trả về members có status = approved
     * - Hỗ trợ tìm kiếm theo tên/email
     * - Phân trang bằng cursor (dựa trên joinedAt)
     * 
     * @param notebookId ID của notebook
     * @param q          Từ khóa tìm kiếm (tên hoặc email)
     * @param cursor     Cursor để phân trang (joinedAt của member cuối cùng)
     * @param limit      Số lượng items mỗi trang (mặc định 20)
     */
    @GetMapping("/{notebookId}/members")
    public NotebookMembersResponse getNotebookMembers(
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal user) {
        return notebookService.getNotebookMembers(notebookId, user.getId(), q, cursor, limit);
    }
}

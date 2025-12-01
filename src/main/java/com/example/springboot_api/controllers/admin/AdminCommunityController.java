package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.admin.notebook.ApproveAllResponse;
import com.example.springboot_api.dto.admin.notebook.ApproveRejectBlockRequest;
import com.example.springboot_api.dto.admin.notebook.ListCommunityRequest;
import com.example.springboot_api.dto.admin.notebook.MemberResponse;
import com.example.springboot_api.dto.admin.notebook.NotebookCreateRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookResponse;
import com.example.springboot_api.dto.admin.notebook.PendingRequestResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.user.community.NotebookDetailResponse;
import com.example.springboot_api.services.admin.AdminCommunityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService service;

    @GetMapping
    public PagedResponse<NotebookResponse> list(@ParameterObject @ModelAttribute ListCommunityRequest req) {
        return service.list(req);
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public NotebookResponse create(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("visibility") String visibility,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        UUID adminId = principal.getId();

        NotebookCreateRequest req = new NotebookCreateRequest(title, description, visibility);
        return service.createCommunity(req, thumbnail, adminId);
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public NotebookResponse update(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("visibility") String visibility,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {

        NotebookCreateRequest req = new NotebookCreateRequest(title, description, visibility);
        return service.update(id, req, thumbnail);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public NotebookResponse getOne(@PathVariable UUID id) {
        return service.getOne(id);
    }

    @GetMapping("/pending-requests")
    public PagedResponse<PendingRequestResponse> getPendingRequests(
            @RequestParam(required = false) UUID notebookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getPendingRequests(notebookId, status, q, sortBy, sortDir, page, size);
    }

    @PostMapping("/member/action")
    public void approveRejectBlockMember(@RequestBody ApproveRejectBlockRequest req) {
        service.approveRejectBlockMember(req);
    }

    @GetMapping("/{id}/detail")
    public NotebookDetailResponse getNotebookDetail(@PathVariable UUID id) {
        return service.getNotebookDetail(id);
    }

    @GetMapping("/{notebookId}/members")
    public PagedResponse<MemberResponse> getNotebookMembers(
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getNotebookMembers(notebookId, status, q, sortBy, sortDir, page, size);
    }

    @PutMapping("/members/{memberId}/role")
    public void updateMemberRole(
            @PathVariable UUID memberId,
            @RequestParam String role) {
        service.updateMemberRole(memberId, role);
    }

    @DeleteMapping("/members/{memberId}")
    public void deleteMember(@PathVariable UUID memberId) {
        service.deleteMember(memberId);
    }

    @PutMapping("/members/{memberId}/block")
    public void blockMember(@PathVariable UUID memberId) {
        service.blockMember(memberId);
    }

    @PutMapping("/members/{memberId}/unblock")
    public void unblockMember(@PathVariable UUID memberId) {
        service.unblockMember(memberId);
    }

    @PutMapping("/members/{memberId}/approve")
    public void approveMember(@PathVariable UUID memberId) {
        service.approveMember(memberId);
    }

    @PostMapping("/pending-requests/approve-all")
    public ApproveAllResponse approveAllPendingRequests(
            @RequestParam(required = false) UUID notebookId) {
        int approvedCount = service.approveAllPendingRequests(notebookId);
        return new ApproveAllResponse(approvedCount);
    }

    @PostMapping("/{notebookId}/pending-requests/approve-all")
    public ApproveAllResponse approveAllPendingRequestsByNotebook(
            @PathVariable UUID notebookId) {
        int approvedCount = service.approveAllPendingRequests(notebookId);
        return new ApproveAllResponse(approvedCount);
    }
}

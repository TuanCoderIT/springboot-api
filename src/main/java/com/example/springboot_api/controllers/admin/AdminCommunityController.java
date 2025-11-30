package com.example.springboot_api.controllers.admin;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.admin.notebook.ApproveRejectBlockRequest;
import com.example.springboot_api.dto.admin.notebook.ListCommunityRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookCreateRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookResponse;
import com.example.springboot_api.dto.admin.notebook.PendingRequestResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.shared.community.NotebookDetailResponse;
import com.example.springboot_api.services.admin.AdminCommunityService;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService service;

    @GetMapping
    public PagedResponse<NotebookResponse> list(@ParameterObject @ModelAttribute ListCommunityRequest req) {
        return service.list(req);
    }

    @PostMapping(consumes = {"multipart/form-data"})
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

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getPendingRequests(notebookId, page, size);
    }

    @PostMapping("/member/action")
    public void approveRejectBlockMember(@RequestBody ApproveRejectBlockRequest req) {
        service.approveRejectBlockMember(req);
    }

    @GetMapping("/{id}/detail")
    public NotebookDetailResponse getNotebookDetail(@PathVariable UUID id) {
        return service.getNotebookDetail(id);
    }
}
